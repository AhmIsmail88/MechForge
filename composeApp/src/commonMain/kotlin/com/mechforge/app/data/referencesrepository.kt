package com.mechforge.app.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.mechforge.db.MechForgeDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

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

        insertDataset(name, category, source, licenseType, licenseNotes, parsed, now)
        return ImportResult(name, parsed.size, errors)
    }

    /**
     * Imports a JSON dataset. Accepted shapes:
     *
     *     [ { "key": "Carbon steel", "value": "7850", "unit": "kg/m3", "notes": "" }, ... ]
     *     { "name": "...", "category": "...", "source": "...", "license": "...",
     *       "rows": [ { "key": ..., "value": ..., "unit": ..., "notes": ... } ] }
     */
    fun importJson(
        nameFallback: String,
        categoryFallback: String,
        sourceFallback: String,
        licenseFallback: String,
        text: String,
        now: Long,
    ): ImportResult {
        val element = runCatching { jsonFormat.parseToJsonElement(text) }.getOrNull()
            ?: return ImportResult(nameFallback, 0, listOf("the file is not valid JSON"))

        val root = element as? JsonObject
        val rowsArray = when {
            root?.get("rows") is JsonArray -> root["rows"] as JsonArray
            element is JsonArray -> element
            else -> return ImportResult(
                nameFallback, 0,
                listOf("expected an array of rows, or an object containing a \"rows\" array"),
            )
        }

        fun textOf(obj: JsonObject?, vararg keys: String): String? =
            keys.firstNotNullOfOrNull { (obj?.get(it) as? JsonPrimitive)?.contentOrNull }

        val name = textOf(root, "name") ?: nameFallback
        val category = textOf(root, "category") ?: categoryFallback
        val source = textOf(root, "source") ?: sourceFallback
        val licence = textOf(root, "license", "licence") ?: licenseFallback

        val rows = rowsArray.mapNotNull { it as? JsonObject }
            .map { obj ->
                listOf(
                    textOf(obj, "key", "name", "title") ?: "",
                    textOf(obj, "value", "val") ?: "",
                    textOf(obj, "unit", "units") ?: "",
                    textOf(obj, "notes", "note", "comment") ?: "",
                )
            }
            .filter { it[0].isNotBlank() && it[1].isNotBlank() }

        if (rows.isEmpty()) return ImportResult(name, 0, listOf("no usable rows found"))

        insertDataset(name, category, source, licence, "Imported by the user (JSON)", rows, now)
        return ImportResult(name, rows.size, emptyList())
    }

    /**
     * Inserts one dataset and its rows, and returns the new dataset id.
     *
     * last_insert_rowid() is connection-scoped: inside the transaction it is exactly the
     * dataset just inserted. (Resolving it by list order was wrong: the built-in datasets
     * shared one timestamp, so their order was undefined and every row ended up attached to
     * whichever dataset happened to sort first.)
     */
    private fun insertDataset(
        name: String,
        category: String,
        source: String,
        licenseType: String,
        licenseNotes: String,
        rows: List<List<String>>,
        now: Long,
    ) {
        db.referenceDatasetsQueries.transaction {
            db.referenceDatasetsQueries.insertDataset(
                name, category, source, licenseType, licenseNotes, now, rows.size.toLong(),
            )
            val datasetId = db.referenceDatasetsQueries.lastInsertRowId().executeAsOne()
            for (fields in rows) {
                db.referenceRowsQueries.insertRow(
                    datasetId,
                    fields[0].trim(),
                    fields[1].trim(),
                    fields.getOrNull(2)?.trim() ?: "",
                    fields.getOrNull(3)?.trim() ?: "",
                )
            }
        }
    }

    private companion object {
        val jsonFormat = Json { ignoreUnknownKeys = true; isLenient = true }
    }

    /**
     * Inserts the built-in generic datasets once (idempotent by name), and repairs a dataset
     * whose stored rows do not match the built-in definition (that is how a database seeded by
     * the earlier version - where every row was attached to the first dataset - heals itself).
     */
    fun seedBuiltIn(now: Long) {
        BuiltInDatasets.all.forEachIndexed { index, dataset ->
            val existing = db.referenceDatasetsQueries.selectAllDatasets().executeAsList()
                .firstOrNull { it.name == dataset.name }
            if (existing != null) {
                val storedRows = db.referenceRowsQueries.selectRowsByDataset(existing.id).executeAsList()
                val mismatched = storedRows.size != dataset.rows.size ||
                    storedRows.firstOrNull()?.row_key != dataset.rows.first()[0]
                if (mismatched) {
                    db.referenceDatasetsQueries.transaction {
                        db.referenceRowsQueries.deleteRowsByDataset(existing.id)
                        for (row in dataset.rows) {
                            db.referenceRowsQueries.insertRow(existing.id, row[0], row[1], row[2], row[3])
                        }
                        db.referenceDatasetsQueries.insertDataset(
                            dataset.name, dataset.category, dataset.source, dataset.licenseType,
                            dataset.licenseNotes, now + index, dataset.rows.size.toLong(),
                        )
                        val newId = db.referenceDatasetsQueries.lastInsertRowId().executeAsOne()
                        // move the repaired rows onto the freshly inserted (correct) dataset
                        val fresh = db.referenceRowsQueries.selectRowsByDataset(existing.id).executeAsList()
                        for (r in fresh) {
                            db.referenceRowsQueries.insertRow(newId, r.row_key, r.value_, r.unit, r.notes)
                        }
                        db.referenceRowsQueries.deleteRowsByDataset(existing.id)
                        db.referenceDatasetsQueries.deleteDataset(existing.id)
                    }
                }
                return@forEachIndexed
            }
            db.referenceDatasetsQueries.transaction {
                // distinct timestamps keep the list order stable and meaningful
                db.referenceDatasetsQueries.insertDataset(
                    dataset.name, dataset.category, dataset.source,
                    dataset.licenseType, dataset.licenseNotes, now + index, dataset.rows.size.toLong(),
                )
                val datasetId = db.referenceDatasetsQueries.lastInsertRowId().executeAsOne()
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
