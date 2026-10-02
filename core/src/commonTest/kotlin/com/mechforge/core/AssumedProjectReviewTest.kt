package com.mechforge.core

import com.mechforge.core.engine.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.PI
import kotlin.test.*

/** Fictional connected project. Equipment data are assumptions, never manufacturer certification. */
class AssumedProjectReviewTest {
    private fun CalcOutput.v(id: String) = results.first { it.id == id }.value
    private fun run(id: String, data: String): CalcOutput {
        val inputs = data.split(" ").filter { it.isNotBlank() }.map {
            val p = it.split(":")
            T.iv(p[0], p[1].toDouble(), p[2])
        }.associateBy { it.inputId }
        val out = CalculatorRegistry.byIdOrThrow(id).run(inputs)
        println("PROJECT|{\"calculator\":\"$id\",\"displayInputs\":" + Json.encodeToString(data) + ",\"output\":" + Json.encodeToString(out) + "}")
        return out
    }

    @Test fun connectedUtilitiesProject() {
        val v = run("pipe-velocity", "q:100:m3h d:154.08:mm").v("v")
        assertEquals((100.0 / 3600) / (PI * 0.15408 * 0.15408 / 4), v, 1e-10)
        val re = run("reynolds-number", "v:$v:ms d:154.08:mm nu:1:cst").v("re")
        val straight = run("darcy-weisbach", "v:$v:ms d:154.08:mm l:60:m re:$re:dash eps:0.045:mm").v("hf")
        val minor = run("minor-losses", "v:$v:ms k:5:dash").results.first().value
        assertEquals(5 * v * v / (2 * 9.80665), minor, 1e-10)
        val h = run("fire-pump-head", "preq:4:bar pavail:0:bar hstatic:10:m hf:${straight + minor + 1}:m rho:998.2:kgm3").v("h")
        // Open reservoir: free surface 2 m above pump; terminal 12 m above pump.
        // Static difference = 10 m; both reservoir and terminal use gauge pressure.
        val power = run("fire-pump-power", "q:100:m3h h:$h:m eta:75:pct rho:998.2:kgm3 bhp150:26:kw bhpmax:30:kw")
        assertTrue(power.v("motor") >= 30)
        val npsh = run("npsh-available", "patm:101.325:kpa pv:2.339:kpa rho:998.2:kgm3 hs:2:m hf:1:m npshr:3:m")
        assertTrue(npsh.v("margin") >= 1 && npsh.v("marginRatio") >= 1.3)
        assertEquals(100.0, run("tank-volume", "q:100:m3h t:1:h").v("v"), 1e-10)
        val heat = run("heat-dissipation", "p:30:kw dt:10:delk tair:35:c alt:0:m cp:1005:jkgk fancap:4000:m3h nfans:3:dash")
        val ach = run("air-changes-hour", "vroom:320:m3 ach:6:perh")
        assertTrue(heat.v("q") < 12000 && heat.v("q") > 1920)
        assertTrue(ach.results.any { it.unitId == "m3h" && kotlin.math.abs(it.value - 1920) < 1e-8 })
        run("fan-power", "q:4000:m3h dp:400:pa etaf:65:pct etad:95:pct")
        val dv = run("duct-velocity", "q:12000:m3h w:800:mm h:600:mm").v("v")
        run("duct-pressure-loss", "v:$dv:ms w:800:mm h:600:mm l:20:m tair:35:c alt:0:m nu:16.5:cst eps:0.09:mm")
        val wall = run("pipe-wall-thickness", "p:16:bar d:168.3:mm sigma:138:mpa e:1:dash weld:1:dash y:0.4:dash ca:1:mm ma:0:mm mill:12.5:pct")
        assertTrue(wall.v("tNom") < 7.11)
        val gas = run("fm200-agent-quantity", "v:300:m3 vgross:320:m3 vexcl:20:m3 hazard:2:dash c:7.5:pct t:20:c s:0.1373:m3perkg mcyl:60:kg")
        assertEquals(3.0, gas.v("cylinders"))
        assertTrue(gas.v("installed") >= gas.v("w"))
        val valve = run("valve-kv", "kv:10:dash dp:2:bar sg:0.9982:dash p1:10:bar pv:0.02339:bar pc:220.64:bar fl:0.8:dash fp:0.9:dash flp:0.72:dash")
        assertTrue(valve.v("q") > 10)
        val choked = run("valve-kv", "kv:10:dash dp:9.5:bar sg:0.9982:dash p1:10:bar pv:0.02339:bar pc:220.64:bar fl:0.8:dash fp:0.9:dash flp:0.72:dash")
        assertTrue(choked.v("dpSizing") < 9.5)
        val base = run("duck-foot-bend-base", "d:154.08:mm tElbow:7.11:mm tPipe:7.11:mm l:3:m q:100:m3h pPump:12:bar pDes:16:bar pTest:24:bar fy:235:mpa fos:1.67:dash rhoW:998.2:kgm3 gammaS:7850:kgm3 h:0.3:m wElbow:50:kg dPlate:800:mm nRibs:4:dash hRib:150:mm nBolts:8:dash bcd:650:mm sigmaBolt:140:mpa tPlateSel:20:mm tRibSel:12:mm dBoltSel:20:mm edgeSel:75:mm atBolt:245:mm2")
        assertTrue(base.v("eccentricity") > base.v("kernRadius"))
        assertTrue(base.results.none { it.id == "bearingMin" || it.id == "bearingMax" })
    }
}
