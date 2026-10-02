"""Supplementary fictional project study, NOT a listed-system or structural design approval.

Run after verify_assumed_project.py. Emits reproducible JSON to stdout, never changes files.
Spring contact is a derived mechanics model, not an implementation of ACI/AISC provisions.
All equipment curves, stiffnesses, loss allowances and demand parameters below are assumed.
"""
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
baseline = json.loads((ROOT / 'docs/assumed-project-inputs-and-results.json').read_text(encoding='utf-8'))
def value(calc, result):
    return next(r['value'] for c in baseline if c['calculator'] == calc
                for r in c['output']['results'] if r['id'] == result)

def contact(n, moment, diameter=.8, bcd=.65, count=8, strips=2400, foundation_k=60e9):
    """Rigid disk on compression-only Winkler bed + tension-only axial anchor springs.

    Positive displacement is downward: u(x)=w+theta*x. Anchors have no pretension.
    Solve sum(R)=N and sum(R*x)=M, where anchor reactions are negative.
    Integration uses exact strip areas and midpoint x; test grid convergence separately.
    """
    radius = diameter / 2
    def primitive(x):
        return x * math.sqrt(max(0, radius**2-x*x)) + radius**2 * math.asin(max(-1,min(1,x/radius)))
    edges = [-radius + diameter*i/strips for i in range(strips+1)]
    bed = [((a+b)/2, foundation_k*(primitive(b)-primitive(a))) for a,b in zip(edges,edges[1:])]
    anchors = [(bcd/2*math.cos(2*math.pi*i/count), 200e9*245e-6/.4) for i in range(count)]
    w, theta = n/(foundation_k*math.pi*radius**2), 0.0
    def state(w, theta):
        f=m=a=b=c=0.0
        for x,k in bed:
            u=w+theta*x
            if u>0:
                r=k*u; f+=r; m+=r*x; a+=k; b+=k*x; c+=k*x*x
        for x,k in anchors:
            u=w+theta*x
            if u<0:
                r=k*u; f+=r; m+=r*x; a+=k; b+=k*x; c+=k*x*x
        return f,m,a,b,c
    for iteration in range(80):
        f,m,a,b,c=state(w,theta)
        rn,rm=n-f,moment-m
        error=math.hypot(rn,rm/radius)
        if error<1e-6: break
        determinant=a*c-b*b
        assert determinant>0
        dw=(rn*c-rm*b)/determinant
        dt=(rm*a-rn*b)/determinant
        scale=1.0
        for _ in range(30):
            trial=state(w+scale*dw,theta+scale*dt)
            if math.hypot(n-trial[0],(moment-trial[1])/radius)<error: break
            scale*=.5
        else: raise AssertionError('Contact line search failed')
        w+=scale*dw; theta+=scale*dt
    else: raise AssertionError('Contact did not converge')
    tension=[max(0,-k*(w+theta*x)) for x,k in anchors]
    compression=sum(k*max(0,w+theta*x) for x,k in bed)
    contact_area=sum(k/foundation_k for x,k in bed if w+theta*x>0)
    assert abs(compression-sum(tension)-n)<1e-5
    return dict(max_anchor_tension_kN=max(tension)/1000,
                total_anchor_tension_kN=sum(tension)/1000, compression_kN=compression/1000,
                anchor_tensions_kN=[t/1000 for t in tension],
                max_pressure_kPa=foundation_k*max(0,w+abs(theta)*radius)/1000,
                contact_area_fraction=contact_area/(math.pi*radius**2),
                neutral_axis_mm=(-w/theta*1000 if theta else None),
                force_residual_N=f-n, moment_residual_Nm=m-moment,
                rotation_rad=theta, foundation_k_N_per_m3=foundation_k)

n=value('duck-foot-bend-base','nTotal')*1000
m=value('duck-foot-bend-base','moment')
h=value('duck-foot-bend-base','hTotal')*1000
base=contact(n,m)
fine=contact(n,m,strips=4800)
assert abs(base['max_anchor_tension_kN']-fine['max_anchor_tension_kN'])<.001
centered=contact(n,0)
assert centered['max_anchor_tension_kN']==0
assert math.isclose(centered['max_pressure_kPa'],n/(math.pi*.4**2)/1000,rel_tol=1e-10)
mirrored=contact(n,-m)
assert math.isclose(mirrored['max_anchor_tension_kN'],base['max_anchor_tension_kN'],rel_tol=1e-9)
scaled=contact(2*n,2*m)
assert math.isclose(scaled['max_anchor_tension_kN'],2*base['max_anchor_tension_kN'],rel_tol=1e-9)
# Continuous external fillet weld around assumed 168.3 mm OD; elastic screening only.
rw=.1683/2; throat=.006/math.sqrt(2)
aw=2*math.pi*rw*throat
sigma=n/aw + m/(math.pi*throat*rw**2)
tau=h/aw
base['thread_stress_MPa']=base['max_anchor_tension_kN']*1000/(245e-6)/1e6
base['weld_6mm_elastic_equivalent_MPa']=math.sqrt(sigma*sigma+3*tau*tau)/1e6
base['stiffness_sensitivity']=[contact(n,m,foundation_k=k) for k in (30e9,120e9)]
base['limitations']=['Rigid plate; foundation stiffness and anchor effective length assumed.',
    'No ACI concrete breakout/pullout/pryout/edge/group/seismic checks.',
    'No plate flexibility/prying or stiffener/weld load-sharing model; elastic weld stress is screening only.']

# Hypothetical pump points Q m3/h, H m, Pshaft kW, NPSHr m.
curve=[(0,70,12,2),(50,65,17,2.5),(100,55,22,3),(125,48,26,3.8),(150,38,30,4.5)]
def interpolate(q,column):
    assert curve[0][0]<=q<=curve[-1][0], 'No curve extrapolation'
    for a,b in zip(curve,curve[1:]):
        if a[0]<=q<=b[0]: return a[column]+(b[column]-a[column])*(q-a[0])/(b[0]-a[0])
def system_head(q):
    return 400000/(998.2*9.80665)+10+(value('darcy-weisbach','hf')+value('minor-losses','hm')+1)*(q/100)**2
lo,hi=100,125
for _ in range(80):
    mid=(lo+hi)/2
    if interpolate(mid,1)>system_head(mid): lo=mid
    else: hi=mid
operating=(lo+hi)/2
pump_rows=[]
for flow in (50,100,operating,125,150):
    na=(101325-2339)/(998.2*9.80665)+2-(flow/100)**2
    nr=interpolate(flow,3)
    pump_rows.append(dict(flow_m3h=flow,head_m=interpolate(flow,1),shaft_kW=interpolate(flow,2),
                          npsha_m=na,npshr_m=nr,margin_m=na-nr,ratio=na/nr))
assert all(row['margin_m']>=1 and row['ratio']>=1.3 for row in pump_rows)

# Illustrative sprinkler-demand sensitivity: no claim of current NFPA design-table certification.
gpm_m3h=.003785411784*60
demand=(.15*1500+250)*gpm_m3h
fire=dict(assumed_class='OH1 study assumption; actual occupancy/storage not specified',
    density_gpm_ft2=.15,area_ft2=1500,hose_gpm=250,duration_min=90,
    lower_bound_flow_m3h=demand,usable_storage_m3=demand*1.5,
    storage_shortfall_m3=demand*1.5-100,
    assumed_dead_volume_m3=10,assumed_freeboard_volume_m3=10,
    preliminary_gross_tank_m3=math.ceil(demand*1.5+20),
    note='Density times area is a lower bound; hydraulic over-discharge and hose connection locations remain unresolved.')

# Explicit symmetric wet-pipe study: 3 identical branches, 4 sprinklers per branch.
# Each sprinkler covers 125 ft2. US K=5.6 gpm/sqrt(psi); C=120 assumed throughout.
# Three 3 m inter-head segments plus 3 m to branch inlet, ID 40 mm;
# each branch has an identical 20 m ID 50 mm feeder, making equal inlet pressures.
psi=6894.757293168
def hazen(length,flow_m3h,diameter):
    return 10.67*length*(flow_m3h/3600)**1.852/(120**1.852*diameter**4.87)
pressure=(.15*125/5.6)**2*psi
branch_flow=0
sprinklers=[]
for index in range(4):
    flow=5.6*math.sqrt(pressure/psi)*gpm_m3h
    branch_flow+=flow
    sprinklers.append(dict(index_from_remote=index+1,pressure_bar=pressure/1e5,flow_m3h=flow))
    pressure+=hazen(3,branch_flow,.04)*998.2*9.80665
pressure+=hazen(20,branch_flow,.05)*998.2*9.80665
sprinkler_total=3*branch_flow
total_fire_flow=sprinkler_total+250*gpm_m3h
main_v=(sprinkler_total/3600)/(math.pi*.15408**2/4)
discharge_head=max(12+pressure/(998.2*9.80665)+hazen(60,sprinkler_total,.15408)+5*main_v**2/(2*9.80665),
                   2e5/(998.2*9.80665))
fire['symmetric_network']=dict(sprinklers_per_branch=sprinklers,branch_flow_m3h=branch_flow,
    sprinkler_total_m3h=sprinkler_total,total_including_hose_m3h=total_fire_flow,
    manifold_pressure_bar=pressure/1e5,
    pump_head_at_demand_m=discharge_head-2+(total_fire_flow/100)**2,
    usable_storage_90min_m3=total_fire_flow*1.5,
    gross_tank_with_20m3_allowances_m3=math.ceil(total_fire_flow*1.5+20),
    note='All branches assumed identical. Hose at pump discharge with 2 bar minimum; excludes unspecified valves/backflow devices. Separate fire network supersedes the original 4 bar receiving-vessel topology.')
assert sprinkler_total>.15*1500*gpm_m3h
assert all(row['flow_m3h']>=.15*125*gpm_m3h-1e-10 for row in sprinklers)

# Clean-agent sensitivity uses entered vapour-volume assumptions, no hydraulic/nozzle model.
def gas_mass(volume,concentration,s): return volume/s*concentration/(1-concentration)
gas=dict(baseline_300m3_kg=gas_mass(300,.075,.1373),
         no_exclusions_320m3_kg=gas_mass(320,.075,.1373),
         no_exclusions_cylinders_60kg=math.ceil(gas_mass(320,.075,.1373)/60),
         original_installed_concentration_at_320m3_pct=100*180*.1373/(320+180*.1373),
         original_installed_concentration_at_300m3_pct=100*180*.1373/(300+180*.1373),
         hypothetical_240kg_at_35C_300m3_pct=100*240*.145/(300+240*.145),
         note='S at 35 C = 0.145 m3/kg is only a sensitivity assumption. Neither 3 nor 4 cylinders is approved.')

# Sum total-pressure losses along ONE critical path, not all three parallel fan branches.
rho_air=value('heat-dissipation','densityUsed')
velocity=value('duct-velocity','v')
dynamic=rho_air*velocity**2/2
losses={'straight_duct':value('duct-pressure-loss','dp'), 'two_elbows_K_0_35_each':.7*dynamic,
        'transition_K_0_25':.25*dynamic,'discharge_K_1':dynamic,
        'intake_louver_assumed':50,'dirty_filter_assumed':120,'damper_assumed':30,
        'branch_and_system_effect_assumed':40}
total=sum(losses.values()); target=1.1*total
vent=dict(losses_Pa=losses,total_Pa=total,ten_percent_design_allowance_Pa=target,
          assumed_fan_pressure_Pa=400,remaining_Pa=400-target,
          two_fans_flow_m3h=8000,required_flow_m3h=value('heat-dissipation','q'),
          two_fans_temperature_rise_K=30000/(rho_air*1005*(8000/3600)),
          recommendation='For N+1 at assumed duty use 4 units: 3 duty plus 1 standby; verify actual parallel curves and isolation.')
assert 400>target
assert 8000<value('heat-dissipation','q')

# Pipe span and fully restrained thermal screen; not B31.3 flexibility analysis.
od=.1683; inside=.15408; wall=.00711; span=3; elastic=200e9
steelarea=math.pi/4*(od**2-inside**2)
inertia=math.pi/64*(od**4-inside**4)
udl=(steelarea*7850+math.pi/4*inside**2*998.2)*9.80665
section=inertia/(od/2)
pipe=dict(material='Illustrative carbon steel; S=138 MPa is an unverified project assumption',
          span_m=span,udl_Nm=udl,bending_stress_MPa=(udl*span**2/8)/section/1e6,
          deflection_mm=5*udl*span**4/(384*elastic*inertia)*1000,
          free_expansion_60m_30K_mm=12e-6*60*30*1000,
          fully_restrained_thermal_stress_MPa=elastic*12e-6*30/1e6,
          required_kv_for_10m3h=10/(.9*math.sqrt(2/.9982)),
          valve_p2_bar_abs=8,valve_pv_bar_abs=.02339,
          note='P2>Pv excludes bulk flashing at this point only; incipient cavitation/noise require valve-specific data.')

print(json.dumps(dict(basis='FICTIONAL STUDY ONLY',base_contact=base,
    pump=dict(curve=curve,operating_flow_m3h=operating,operating_head_m=system_head(operating),
              rows=pump_rows,shutoff_ratio=70/55,flow150_head_ratio=38/55,
              note='No manufacturer curve or certification; fixed-friction Q squared system approximation.'),
    fire_demand=fire,gas_sensitivity=gas,ventilation=vent,piping=pipe),indent=2))
