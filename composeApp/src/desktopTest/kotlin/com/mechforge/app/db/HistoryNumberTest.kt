package com.mechforge.app.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.mechforge.app.data.HistoryRepository
import com.mechforge.app.export.ReportQa
import com.mechforge.db.MechForgeDatabase
import kotlin.test.*

class HistoryNumberTest {
    @Test fun savedRecordsReceiveStableDistinctNumbersForImmediateExport() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            MechForgeDatabase.Schema.create(driver)
            val repo = HistoryRepository(MechForgeDatabase(driver))
            val first = repo.add("pipe-velocity", "Phone test", 1, "{}", "{}")
            val second = repo.add("pipe-velocity", "Phone test", 1, "{}", "{}")
            assertEquals("CAL-000001", first.calculation_number)
            assertEquals("CAL-000002", second.calculation_number)
            assertEquals(first, repo.byId(first.id))
            assertTrue(ReportQa.check(null, first.calculation_number, null, null, emptyList(), true)
                .none { it.message.contains("not linked") })
            val explicit = repo.add("pipe-velocity", "Issued", 2, "{}", "{}", calculationNumber = "P-42")
            assertEquals("P-42", explicit.calculation_number)
        } finally { driver.close() }
    }
}
