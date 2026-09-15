package com.mechforge.core.units

/**
 * A unit inside a [UnitFamily]. All conversions go through a family base unit (SI).
 * Offset units (temperature) implement both directions explicitly.
 */
data class UnitDef(
    val id: String,
    val symbol: String,
    val family: UnitFamily,
    val toBase: (Double) -> Double,
    val fromBase: (Double) -> Double,
) {
    fun convert(value: Double, to: UnitDef): Double {
        require(family == to.family) {
            "Cannot convert between different unit families: $family -> ${to.family}"
        }
        return to.fromBase(toBase(value))
    }
}

object Units {

    val all: List<UnitDef> = buildList {
        // Pressure (base: Pa)
        add(factorUnit("pa", "Pa", UnitFamily.PRESSURE, 1.0))
        add(factorUnit("kpa", "kPa", UnitFamily.PRESSURE, 1e3))
        add(factorUnit("mpa", "MPa", UnitFamily.PRESSURE, 1e6))
        add(factorUnit("bar", "bar", UnitFamily.PRESSURE, 1e5))
        add(factorUnit("psi", "psi", UnitFamily.PRESSURE, 6894.757293168361))
        add(factorUnit("atm", "atm", UnitFamily.PRESSURE, 101325.0))
        add(factorUnit("mh2o", "mH₂O", UnitFamily.PRESSURE, 9806.65))
        add(factorUnit("fth2o", "ftH₂O", UnitFamily.PRESSURE, 2989.0669))
        add(factorUnit("nmm2", "N/mm²", UnitFamily.PRESSURE, 1e6))

        // Pressure gradient (base: Pa/m)
        add(factorUnit("pam", "Pa/m", UnitFamily.PRESSURE_GRADIENT, 1.0))
        add(factorUnit("kpam", "kPa/m", UnitFamily.PRESSURE_GRADIENT, 1e3))
        add(factorUnit("inh2operft", "inH₂O/ft", UnitFamily.PRESSURE_GRADIENT, 9806.65 / 12.0))

        // Flow (base: m³/s)
        add(factorUnit("m3s", "m³/s", UnitFamily.FLOW, 1.0))
        add(factorUnit("m3h", "m³/h", UnitFamily.FLOW, 1.0 / 3600.0))
        add(factorUnit("m3d", "m³/d", UnitFamily.FLOW, 1.0 / 86400.0))
        add(factorUnit("ls", "L/s", UnitFamily.FLOW, 1e-3))
        add(factorUnit("lmin", "L/min", UnitFamily.FLOW, 1e-3 / 60.0))
        add(factorUnit("gpm", "GPM", UnitFamily.FLOW, 6.30901964e-5))
        add(factorUnit("cfm", "CFM", UnitFamily.FLOW, 4.719474432e-4))

        // Power (base: W)
        add(factorUnit("w", "W", UnitFamily.POWER, 1.0))
        add(factorUnit("kw", "kW", UnitFamily.POWER, 1e3))
        add(factorUnit("hp", "HP", UnitFamily.POWER, 745.69987158227))
        add(factorUnit("tr", "TR", UnitFamily.POWER, 3516.8528))
        add(factorUnit("btuh", "BTU/h", UnitFamily.POWER, 0.29307107017))

        // Length (base: m)
        add(factorUnit("mm", "mm", UnitFamily.LENGTH, 1e-3))
        add(factorUnit("cm", "cm", UnitFamily.LENGTH, 1e-2))
        add(factorUnit("m", "m", UnitFamily.LENGTH, 1.0))
        add(factorUnit("in", "in", UnitFamily.LENGTH, 0.0254))
        add(factorUnit("ft", "ft", UnitFamily.LENGTH, 0.3048))

        // Temperature (base: K) — offset units
        add(UnitDef("k", "K", UnitFamily.TEMPERATURE, { it }, { it }))
        add(UnitDef("c", "°C", UnitFamily.TEMPERATURE, { it + 273.15 }, { it - 273.15 }))
        add(
            UnitDef(
                "f", "°F", UnitFamily.TEMPERATURE,
                { (it - 32.0) * 5.0 / 9.0 + 273.15 },
                { (it - 273.15) * 9.0 / 5.0 + 32.0 },
            )
        )

        // Mass (base: kg)
        add(factorUnit("g", "g", UnitFamily.MASS, 1e-3))
        add(factorUnit("kg", "kg", UnitFamily.MASS, 1.0))
        add(factorUnit("t", "t", UnitFamily.MASS, 1e3))
        add(factorUnit("lb", "lb", UnitFamily.MASS, 0.45359237))

        // Mass per length (base: kg/m)
        add(factorUnit("kgperm", "kg/m", UnitFamily.MASS_PER_LENGTH, 1.0))
        add(factorUnit("kgpermm", "kg/mm", UnitFamily.MASS_PER_LENGTH, 1e3))
        add(factorUnit("lbperft", "lb/ft", UnitFamily.MASS_PER_LENGTH, 1.488163943))

        // Density (base: kg/m³)
        add(factorUnit("kgm3", "kg/m³", UnitFamily.DENSITY, 1.0))
        add(factorUnit("gcm3", "g/cm³", UnitFamily.DENSITY, 1e3))
        add(factorUnit("lbft3", "lb/ft³", UnitFamily.DENSITY, 16.0184634))
        add(factorUnit("mgl", "mg/L", UnitFamily.DENSITY, 1e-3))

        // Velocity (base: m/s)
        add(factorUnit("ms", "m/s", UnitFamily.VELOCITY, 1.0))
        add(factorUnit("kmh", "km/h", UnitFamily.VELOCITY, 1.0 / 3.6))
        add(factorUnit("mh", "m/h", UnitFamily.VELOCITY, 1.0 / 3600.0))
        add(factorUnit("md", "m/d", UnitFamily.VELOCITY, 1.0 / 86400.0))
        add(factorUnit("fts", "ft/s", UnitFamily.VELOCITY, 0.3048))
        add(factorUnit("fpm", "ft/min", UnitFamily.VELOCITY, 0.3048 / 60.0))
        add(factorUnit("mph", "mph", UnitFamily.VELOCITY, 0.44704))

        // Kinematic viscosity (base: m²/s)
        add(factorUnit("m2s", "m²/s", UnitFamily.KINEMATIC_VISCOSITY, 1.0))
        add(factorUnit("st", "St", UnitFamily.KINEMATIC_VISCOSITY, 1e-4))
        add(factorUnit("cst", "cSt", UnitFamily.KINEMATIC_VISCOSITY, 1e-6))

        // Dynamic viscosity (base: Pa·s)
        add(factorUnit("pas", "Pa·s", UnitFamily.DYNAMIC_VISCOSITY, 1.0))
        add(factorUnit("mpas", "mPa·s", UnitFamily.DYNAMIC_VISCOSITY, 1e-3))
        add(factorUnit("cp", "cP", UnitFamily.DYNAMIC_VISCOSITY, 1e-3))

        // Rotational speed (base: rpm)
        add(factorUnit("rpm", "rpm", UnitFamily.ROTATIONAL_SPEED, 1.0))
        add(factorUnit("rads", "rad/s", UnitFamily.ROTATIONAL_SPEED, 60.0 / (2.0 * Math.PI)))

        // Revolutions (base: rev)
        add(factorUnit("rev", "rev", UnitFamily.REVOLUTIONS, 1.0))
        add(factorUnit("krev", "10³ rev", UnitFamily.REVOLUTIONS, 1e3))
        add(factorUnit("mrev", "10⁶ rev", UnitFamily.REVOLUTIONS, 1e6))

        // Torque (base: N·m)
        add(factorUnit("nm", "N·m", UnitFamily.TORQUE, 1.0))
        add(factorUnit("lbfft", "lbf·ft", UnitFamily.TORQUE, 1.3558179483314))
        add(factorUnit("kgfm", "kgf·m", UnitFamily.TORQUE, 9.80665))

        // Volume (base: m³)
        add(factorUnit("m3", "m³", UnitFamily.VOLUME, 1.0))
        add(factorUnit("liter", "L", UnitFamily.VOLUME, 1e-3))
        add(factorUnit("ft3", "ft³", UnitFamily.VOLUME, 0.028316846592))
        add(factorUnit("galus", "gal (US)", UnitFamily.VOLUME, 0.003785411784))

        // Specific volume (base: m³/kg)
        add(factorUnit("m3perkg", "m³/kg", UnitFamily.SPECIFIC_VOLUME, 1.0))
        add(factorUnit("lperkg", "L/kg", UnitFamily.SPECIFIC_VOLUME, 1e-3))
        add(factorUnit("ft3perlb", "ft³/lb", UnitFamily.SPECIFIC_VOLUME, 0.0624279606))

        // Time (base: s)
        add(factorUnit("s", "s", UnitFamily.TIME, 1.0))
        add(factorUnit("min", "min", UnitFamily.TIME, 60.0))
        add(factorUnit("h", "h", UnitFamily.TIME, 3600.0))
        add(factorUnit("day", "day", UnitFamily.TIME, 86400.0))

        // Mass flow (base: kg/s)
        add(factorUnit("kgs", "kg/s", UnitFamily.MASS_FLOW, 1.0))
        add(factorUnit("kgh", "kg/h", UnitFamily.MASS_FLOW, 1.0 / 3600.0))
        add(factorUnit("kgd", "kg/d", UnitFamily.MASS_FLOW, 1.0 / 86400.0))
        add(factorUnit("lbh", "lb/h", UnitFamily.MASS_FLOW, 0.45359237 / 3600.0))

        // Force (base: N)
        add(factorUnit("n", "N", UnitFamily.FORCE, 1.0))
        add(factorUnit("kn", "kN", UnitFamily.FORCE, 1e3))
        add(factorUnit("mn", "MN", UnitFamily.FORCE, 1e6))
        add(factorUnit("kgf", "kgf", UnitFamily.FORCE, 9.80665))
        add(factorUnit("lbf", "lbf", UnitFamily.FORCE, 4.4482216152605))

        // Line load (base: N/m)
        add(factorUnit("nlperm", "N/m", UnitFamily.LINE_LOAD, 1.0))
        add(factorUnit("knlperm", "kN/m", UnitFamily.LINE_LOAD, 1e3))
        add(factorUnit("lbfperft", "lbf/ft", UnitFamily.LINE_LOAD, 14.593902937))

        // Second moment of area (base: m⁴)
        add(factorUnit("m4", "m⁴", UnitFamily.SECOND_MOMENT, 1.0))
        add(factorUnit("cm4", "cm⁴", UnitFamily.SECOND_MOMENT, 1e-8))
        add(factorUnit("mm4", "mm⁴", UnitFamily.SECOND_MOMENT, 1e-12))
        add(factorUnit("in4", "in⁴", UnitFamily.SECOND_MOMENT, 4.162314256e-7))

        // Section modulus (base: m³)
        add(factorUnit("m3sect", "m³", UnitFamily.SECTION_MODULUS, 1.0))
        add(factorUnit("cm3sect", "cm³", UnitFamily.SECTION_MODULUS, 1e-6))
        add(factorUnit("mm3sect", "mm³", UnitFamily.SECTION_MODULUS, 1e-9))
        add(factorUnit("in3sect", "in³", UnitFamily.SECTION_MODULUS, 1.6387064e-5))

        // Energy (base: J)
        add(factorUnit("j", "J", UnitFamily.ENERGY, 1.0))
        add(factorUnit("kj", "kJ", UnitFamily.ENERGY, 1e3))
        add(factorUnit("mj", "MJ", UnitFamily.ENERGY, 1e6))
        add(factorUnit("kwh", "kWh", UnitFamily.ENERGY, 3.6e6))
        add(factorUnit("btu", "BTU", UnitFamily.ENERGY, 1055.05585262))
        add(factorUnit("kcal", "kcal", UnitFamily.ENERGY, 4184.0))

        // Specific heat (base: J/(kg·K))
        add(factorUnit("jkgk", "J/(kg·K)", UnitFamily.SPECIFIC_HEAT, 1.0))
        add(factorUnit("kjkgk", "kJ/(kg·K)", UnitFamily.SPECIFIC_HEAT, 1e3))
        add(factorUnit("btulb", "BTU/(lb·°F)", UnitFamily.SPECIFIC_HEAT, 4186.8))

        // Heat transfer coefficient (base: W/(m²·K))
        add(factorUnit("wm2k", "W/(m²·K)", UnitFamily.HEAT_TRANSFER_COEFF, 1.0))
        add(factorUnit("kwm2k", "kW/(m²·K)", UnitFamily.HEAT_TRANSFER_COEFF, 1e3))
        add(factorUnit("kcalh", "kcal/(h·m²·°C)", UnitFamily.HEAT_TRANSFER_COEFF, 1.163))
        add(factorUnit("btuhft2", "BTU/(h·ft²·°F)", UnitFamily.HEAT_TRANSFER_COEFF, 5.678263341))

        // Flow factor (sprinkler K-factor; base: L/min/bar^0.5)
        add(factorUnit("lpmbar", "L/min/bar^0.5", UnitFamily.FLOW_FACTOR, 1.0))
        add(factorUnit("gpmpsi", "gpm/psi^0.5", UnitFamily.FLOW_FACTOR, 14.41704))

        // Temperature difference (base: K) — difference units, no offsets
        add(factorUnit("delk", "K", UnitFamily.TEMPERATURE_DIFFERENCE, 1.0))
        add(factorUnit("delc", "°C", UnitFamily.TEMPERATURE_DIFFERENCE, 1.0))
        add(factorUnit("delf", "°F", UnitFamily.TEMPERATURE_DIFFERENCE, 5.0 / 9.0))

        // Stiffness (base: N/m)
        add(factorUnit("nperm", "N/m", UnitFamily.STIFFNESS, 1.0))
        add(factorUnit("npermm", "N/mm", UnitFamily.STIFFNESS, 1e3))
        add(factorUnit("knperm", "kN/m", UnitFamily.STIFFNESS, 1e3))
        add(factorUnit("lbfperin", "lbf/in", UnitFamily.STIFFNESS, 175.126835))

        // Area (base: m²)
        add(factorUnit("m2", "m²", UnitFamily.AREA, 1.0))
        add(factorUnit("mm2", "mm²", UnitFamily.AREA, 1e-6))
        add(factorUnit("cm2", "cm²", UnitFamily.AREA, 1e-4))
        add(factorUnit("in2", "in²", UnitFamily.AREA, 6.4516e-4))
        add(factorUnit("ft2", "ft²", UnitFamily.AREA, 0.09290304))

        // Molar mass (base: kg/mol)
        add(factorUnit("kgmol", "kg/mol", UnitFamily.MOLAR_MASS, 1.0))
        add(factorUnit("gmol", "g/mol", UnitFamily.MOLAR_MASS, 1e-3))

        // Dimensionless
        add(UnitDef("dash", "-", UnitFamily.DIMENSIONLESS, { it }, { it }))
        add(UnitDef("pct", "%", UnitFamily.DIMENSIONLESS, { it / 100.0 }, { it * 100.0 }))
        add(UnitDef("perh", "1/h", UnitFamily.DIMENSIONLESS, { it }, { it }))
        add(UnitDef("permk", "µm/(m·K)", UnitFamily.DIMENSIONLESS, { it * 1e-6 }, { it / 1e-6 }))
        add(UnitDef("mgm3", "mg/m³", UnitFamily.DIMENSIONLESS, { it }, { it }))
    }

    private val byId: Map<String, UnitDef> = all.associateBy { it.id }

    fun byId(id: String): UnitDef = byId.getValue(id)

    fun byFamily(family: UnitFamily): List<UnitDef> = all.filter { it.family == family }

    fun convert(value: Double, fromUnitId: String, toUnitId: String): Double =
        byId(fromUnitId).convert(value, byId(toUnitId))

    private val defaults: Map<UnitFamily, String> = mapOf(
        UnitFamily.PRESSURE to "bar",
        UnitFamily.PRESSURE_GRADIENT to "pam",
        UnitFamily.FLOW to "m3h",
        UnitFamily.POWER to "kw",
        UnitFamily.LENGTH to "m",
        UnitFamily.TEMPERATURE to "c",
        UnitFamily.MASS to "kg",
        UnitFamily.MASS_PER_LENGTH to "kgperm",
        UnitFamily.DENSITY to "kgm3",
        UnitFamily.VELOCITY to "ms",
        UnitFamily.KINEMATIC_VISCOSITY to "cst",
        UnitFamily.DYNAMIC_VISCOSITY to "mpas",
        UnitFamily.ROTATIONAL_SPEED to "rpm",
        UnitFamily.TORQUE to "nm",
        UnitFamily.VOLUME to "m3",
        UnitFamily.SPECIFIC_VOLUME to "m3perkg",
        UnitFamily.AREA to "m2",
        UnitFamily.MOLAR_MASS to "gmol",
        UnitFamily.REVOLUTIONS to "rev",
        UnitFamily.TIME to "h",
        UnitFamily.MASS_FLOW to "kgh",
        UnitFamily.STIFFNESS to "npermm",
        UnitFamily.FORCE to "kn",
        UnitFamily.LINE_LOAD to "knlperm",
        UnitFamily.SECOND_MOMENT to "mm4",
        UnitFamily.SECTION_MODULUS to "mm3sect",
        UnitFamily.ENERGY to "kj",
        UnitFamily.SPECIFIC_HEAT to "kjkgk",
        UnitFamily.HEAT_TRANSFER_COEFF to "wm2k",
        UnitFamily.TEMPERATURE_DIFFERENCE to "delk",
        UnitFamily.FLOW_FACTOR to "lpmbar",
        UnitFamily.DIMENSIONLESS to "pct",
    )

    /** Default display unit for a family (used when a spec does not pin one). */
    fun defaultUnit(family: UnitFamily): UnitDef = byId(defaults.getValue(family))

    private fun factorUnit(id: String, symbol: String, family: UnitFamily, factor: Double) =
        UnitDef(id, symbol, family, { it * factor }, { it / factor })
}
