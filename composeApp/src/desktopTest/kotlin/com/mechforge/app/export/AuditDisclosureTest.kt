package com.mechforge.app.export

import com.mechforge.app.ui.CalcText
import com.mechforge.core.engine.CalculatorRegistry
import com.mechforge.core.engine.CalcOutput
import kotlin.test.Test
import kotlin.test.assertTrue

class AuditDisclosureTest {
    @Test fun arabicResultRetainsTheoreticalAndPreliminaryQualification() {
        assertTrue(CalcText.resultLabel("co2-agent-quantity", "w", "Theoretical final quantity", true).contains("غير معتمد"))
        assertTrue(CalcText.resultLabel("fire-pump-power", "motor", "Preliminary motor rating", true).contains("تحتاج تحققًا"))
        assertTrue(CalcText.resultLabel("duck-foot-bend-base", "dBoltReq", "Theoretical equivalent solid diameter", true).contains("ليس مقاس قلاووظ"))
    }

    @Test fun incompleteArabicWarningsDoNotHideEnglishEngineeringWarnings() {
        val def = CalculatorRegistry.byIdOrThrow("pump-power").def
        val output = CalcOutput(results = emptyList(), steps = emptyList(), warnings = listOf("Check the pump curve", "Missing supplier data"), warningsAr = listOf("تحقق من منحنى المضخة"))
        val visible = CalcText.warnings(def, emptyMap(), output, true)
        assertTrue("Missing supplier data" in visible)
        assertTrue("تحقق من منحنى المضخة" in visible)
    }
}
