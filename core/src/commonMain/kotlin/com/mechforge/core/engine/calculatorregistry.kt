package com.mechforge.core.engine

import com.mechforge.core.calcs.AirChangesCalculator
import com.mechforge.core.calcs.AirflowConverterCalculator
import com.mechforge.core.calcs.BeamCantileverPointCalculator
import com.mechforge.core.calcs.BeamSsUdlCalculator
import com.mechforge.core.calcs.BearingL10Calculator
import com.mechforge.core.calcs.BoltTorqueCalculator
import com.mechforge.core.calcs.CarnotEfficiencyCalculator
import com.mechforge.core.calcs.ChlorineDoseCalculator
import com.mechforge.core.calcs.CompressorPowerCalculator
import com.mechforge.core.calcs.DarcyWeisbachCalculator
import com.mechforge.core.calcs.DetentionTimeCalculator
import com.mechforge.core.calcs.DuctPressureLossCalculator
import com.mechforge.core.calcs.DuctSizingCalculator
import com.mechforge.core.calcs.DuctVelocityCalculator
import com.mechforge.core.calcs.EquivalentLengthCalculator
import com.mechforge.core.calcs.FanPowerCalculator
import com.mechforge.core.calcs.FlowConverterCalculator
import com.mechforge.core.calcs.FrictionFactorCalculator
import com.mechforge.core.calcs.GearRatioCalculator
import com.mechforge.core.calcs.HazenWilliamsCalculator
import com.mechforge.core.calcs.HydraulicLoadingCalculator
import com.mechforge.core.calcs.IdealGasCalculator
import com.mechforge.core.calcs.IsentropicRelationCalculator
import com.mechforge.core.calcs.LatentHeatCalculator
import com.mechforge.core.calcs.LengthConverterCalculator
import com.mechforge.core.calcs.LmtdCalculator
import com.mechforge.core.calcs.ManningCalculator
import com.mechforge.core.calcs.MinorLossCalculator
import com.mechforge.core.calcs.NpshAvailableCalculator
import com.mechforge.core.calcs.OrificeFlowCalculator
import com.mechforge.core.calcs.PeakFlowCalculator
import com.mechforge.core.calcs.PipeSizingCalculator
import com.mechforge.core.calcs.PipeVelocityCalculator
import com.mechforge.core.calcs.PipeWallThicknessCalculator
import com.mechforge.core.calcs.PipeWeightCalculator
import com.mechforge.core.calcs.PowerConverterCalculator
import com.mechforge.core.calcs.PowerEfficiencyConverterCalculator
import com.mechforge.core.calcs.PowerTorqueRpmCalculator
import com.mechforge.core.calcs.PressureConverterCalculator
import com.mechforge.core.calcs.PumpPowerCalculator
import com.mechforge.core.calcs.ReynoldsNumberCalculator
import com.mechforge.core.calcs.SensibleHeatCalculator
import com.mechforge.core.calcs.SpringRateCalculator
import com.mechforge.core.calcs.TankVolumeCalculator
import com.mechforge.core.calcs.TemperatureConverterCalculator
import com.mechforge.core.calcs.ThermalEfficiencyCalculator
import com.mechforge.core.calcs.ThermalExpansionCalculator
import com.mechforge.core.calcs.TorsionalStressCalculator
import com.mechforge.core.calcs.TotalCoolingLoadCalculator
import com.mechforge.core.calcs.ValveKvCalculator

/**
 * Registry of all calculators.
 * Pilot v0.1 (12 calculators, README v2 §7.1) + MVP expansion (README v2 §7.2) = 50.
 */
object CalculatorRegistry {

    val all: List<Calculator> = listOf(
        // Hydraulics (10)
        PumpPowerCalculator,
        PipeVelocityCalculator,
        ReynoldsNumberCalculator,
        DarcyWeisbachCalculator,
        MinorLossCalculator,
        FrictionFactorCalculator,
        OrificeFlowCalculator,
        ManningCalculator,
        HazenWilliamsCalculator,
        NpshAvailableCalculator,
        // HVAC (10)
        SensibleHeatCalculator,
        TotalCoolingLoadCalculator,
        AirflowConverterCalculator,
        PowerEfficiencyConverterCalculator,
        LatentHeatCalculator,
        DuctVelocityCalculator,
        DuctSizingCalculator,
        DuctPressureLossCalculator,
        FanPowerCalculator,
        AirChangesCalculator,
        // Thermodynamics (6)
        IdealGasCalculator,
        CarnotEfficiencyCalculator,
        ThermalEfficiencyCalculator,
        IsentropicRelationCalculator,
        CompressorPowerCalculator,
        LmtdCalculator,
        // Mechanical design (8)
        PowerTorqueRpmCalculator,
        TorsionalStressCalculator,
        BoltTorqueCalculator,
        BearingL10Calculator,
        SpringRateCalculator,
        BeamSsUdlCalculator,
        BeamCantileverPointCalculator,
        GearRatioCalculator,
        // Piping (6)
        PipeSizingCalculator,
        PipeWeightCalculator,
        PipeWallThicknessCalculator,
        ThermalExpansionCalculator,
        EquivalentLengthCalculator,
        ValveKvCalculator,
        // Water & wastewater (5)
        TankVolumeCalculator,
        DetentionTimeCalculator,
        ChlorineDoseCalculator,
        PeakFlowCalculator,
        HydraulicLoadingCalculator,
        // Unit conversion (5)
        PressureConverterCalculator,
        FlowConverterCalculator,
        PowerConverterCalculator,
        LengthConverterCalculator,
        TemperatureConverterCalculator,
    )

    val byCategory: Map<CalculatorCategory, List<Calculator>> =
        all.groupBy { it.def.category }

    fun byId(id: String): Calculator? = all.firstOrNull { it.def.id == id }

    fun byIdOrThrow(id: String): Calculator = byId(id)
        ?: throw IllegalArgumentException("Unknown calculator: $id")

    /** Global search over name, keywords and category (README §25). */
    fun search(query: String): List<Calculator> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return all
        return all.filter { c ->
            c.def.name.lowercase().contains(q) ||
                c.def.category.displayName.lowercase().contains(q) ||
                c.def.keywords.any { it.lowercase().contains(q) }
        }
    }
}
