package com.mechforge.app.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.mechforge.db.MechForgeDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** A reference dataset as shown in the UI (mapped from the generated table row). */
data class RefDataset(
    val id: Long,
    val name: String,
    val category: String,
    val source: String,
    val licenseType: String,
    val licenseNotes: String,
    val importedAt: Long,
    val rowCount: Long,
)

/** One row of a reference dataset. */
data class RefRow(val key: String, val value: String, val unit: String, val notes: String)

/** Result of one dataset import. */
data class ImportResult(val name: String, val rowsImported: Int, val errors: List<String>) {
    val ok: Boolean get() = errors.isEmpty() && rowsImported > 0
}

/**
 * Engineering reference library (README v2 16/17/18).
 *
 * The application ships NO copyrighted table: it ships (a) a small set of self-authored
 * generic values that are common engineering knowledge, and (b) a CSV importer so an
 * office can load its own licensed data. Every dataset carries source and licence
 * metadata, which the UI shows next to the data.
 */
class ReferencesRepository(private val db: MechForgeDatabase) {

    fun datasets(): Flow<List<RefDataset>> =
        db.referenceDatasetsQueries.selectAllDatasets()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows -> rows.map { toDataset(it.id, it.name, it.category, it.source, it.license_type, it.license_notes, it.imported_at, it.row_count) } }

    fun rows(datasetId: Long): List<RefRow> =
        db.referenceRowsQueries.selectRowsByDataset(datasetId).executeAsList()
            .map { RefRow(it.row_key, it.value_, it.unit, it.notes) }

    fun rowCount(datasetId: Long): Long =
        db.referenceRowsQueries.countRowsByDataset(datasetId).executeAsOne()

    fun deleteDataset(datasetId: Long) {
        db.referenceRowsQueries.deleteRowsByDataset(datasetId)
        db.referenceDatasetsQueries.deleteDataset(datasetId)
    }

    /**
     * Imports a CSV dataset. Expected columns:
     *
     *     key,value,unit,notes
     *     Standard motor rating,0.55,kW,IEC series
     *
     * The header row is optional; unit and notes may be empty; fields may be quoted.
     */
    fun importCsv(
        name: String,
        category: String,
        source: String,
        licenseType: String,
        licenseNotes: String,
        csv: String,
        now: Long,
    ): ImportResult {
        val errors = mutableListOf<String>()
        val parsed = mutableListOf<List<String>>()
        var lineNo = 0
        for (rawLine in csv.lineSequence()) {
            lineNo++
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            val fields = splitCsv(line)
            if (lineNo == 1 && fields.firstOrNull()?.trim()?.equals("key", ignoreCase = true) == true) continue
            if (fields.size < 2) {
                errors += "line $lineNo: needs at least key,value"
                continue
            }
            parsed += fields
        }
        if (parsed.isEmpty()) {
            errors += "no usable rows found"
            return ImportResult(name, 0, errors)
        }

        db.referenceDatasetsQueries.transaction {
            db.referenceDatasetsQueries.insertDataset(
                name, category, source, licenseType, licenseNotes, now, parsed.size.toLong(),
            )
            val datasetId = db.referenceDatasetsQueries.selectAllDatasets().executeAsList().first().id
            for (fields in parsed) {
                db.referenceRowsQueries.insertRow(
                    datasetId,
                    fields[0].trim(),
                    fields[1].trim(),
                    fields.getOrNull(2)?.trim() ?: "",
                    fields.getOrNull(3)?.trim() ?: "",
                )
            }
        }
        return ImportResult(name, parsed.size, errors)
    }

    /** Inserts the built-in generic datasets once (idempotent by name). */
    fun seedBuiltIn(now: Long) {
        for (dataset in BuiltInDatasets.all) {
            if (db.referenceDatasetsQueries.countDatasetByName(dataset.name).executeAsOne() > 0L) continue
            db.referenceDatasetsQueries.transaction {
                db.referenceDatasetsQueries.insertDataset(
                    dataset.name, dataset.category, dataset.source,
                    dataset.licenseType, dataset.licenseNotes, now, dataset.rows.size.toLong(),
                )
                val datasetId = db.referenceDatasetsQueries.selectAllDatasets().executeAsList().first().id
                for (row in dataset.rows) {
                    db.referenceRowsQueries.insertRow(datasetId, row[0], row[1], row[2], row[3])
                }
            }
        }
    }

    private fun splitCsv(line: String): List<String> {
        val out = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"'); i++
                }
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    out += current.toString(); current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        out += current.toString()
        return out
    }
}

internal fun toDataset(
    id: Long, name: String, category: String, source: String,
    licenseType: String, licenseNotes: String, importedAt: Long, rowCount: Long,
) = RefDataset(id, name, category, source, licenseType, licenseNotes, importedAt, rowCount)
