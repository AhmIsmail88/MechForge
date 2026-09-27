package com.mechforge.app.data

/**
 * Built-in reference datasets.
 *
 * Nothing here is a reproduction of a copyrighted table: these are generic engineering
 * values that are common knowledge (material densities, water properties, physical
 * constants, typical roughness) plus the IEC preferred motor-rating NUMBERS. Every
 * dataset still carries source and licence metadata, and the notes tell the user to
 * verify against the project's own data. Anything an office licenses is imported by the
 * user through the CSV importer instead.
 */
data class BuiltInDataset(
    val name: String,
    val category: String,
    val source: String,
    val licenseType: String,
    val licenseNotes: String,
    val rows: List<List<String>>,
) {
    companion object {
        private fun row(key: String, value: String, unit: String, notes: String = "") =
            listOf(key, value, unit, notes)
    }
}

object BuiltInDatasets {

    private fun row(key: String, value: String, unit: String, notes: String = "") =
        listOf(key, value, unit, notes)

    val all: List<BuiltInDataset> get() = listOf(
        BuiltInDataset(
            name = "IEC standard motor ratings",
            category = "Equipment",
            source = "IEC 60034-1 preferred rating numbers",
            licenseType = "public-domain values",
            licenseNotes = "Only the standard numerical series is listed; the standard itself is not reproduced.",
            rows = listOf(
                "0.55", "0.75", "1.1", "1.5", "2.2", "3", "4", "5.5", "7.5", "11", "15", "18.5",
                "22", "30", "37", "45", "55", "75", "90", "110", "132", "160", "200",
            ).map { row("Motor rating $it kW", it, "kW", "Standard IEC rating") },
        ),
        BuiltInDataset(
            name = "Pipe outside diameter by nominal size (DN / NPS)",
            category = "Piping",
            source = "ASME B36.10M / B36.19M nominal outside diameters (published values)",
            licenseType = "published standard dimensions - verify against the purchased standard",
            licenseNotes = "The outside diameter of a nominal pipe size is the same for carbon and stainless steel. Only the diameter is listed here: the WALL THICKNESS per schedule is a design input and is deliberately not reproduced - import your licensed schedule table through the CSV importer (see tools/reference/pipe-schedule-template.csv, which has the matching columns).",
            rows = listOf(
                row("DN 15 (1/2 in)", "21.3", "mm", "nominal outside diameter"),
                row("DN 20 (3/4 in)", "26.7", "mm", "nominal outside diameter"),
                row("DN 25 (1 in)", "33.4", "mm", "nominal outside diameter"),
                row("DN 32 (1 1/4 in)", "42.2", "mm", "nominal outside diameter"),
                row("DN 40 (1 1/2 in)", "48.3", "mm", "nominal outside diameter"),
                row("DN 50 (2 in)", "60.3", "mm", "nominal outside diameter"),
                row("DN 65 (2 1/2 in)", "73.0", "mm", "nominal outside diameter"),
                row("DN 80 (3 in)", "88.9", "mm", "nominal outside diameter"),
                row("DN 90 (3 1/2 in)", "101.6", "mm", "nominal outside diameter"),
                row("DN 100 (4 in)", "114.3", "mm", "nominal outside diameter"),
                row("DN 125 (5 in)", "141.3", "mm", "nominal outside diameter"),
                row("DN 150 (6 in)", "168.3", "mm", "nominal outside diameter"),
                row("DN 200 (8 in)", "219.1", "mm", "nominal outside diameter"),
                row("DN 250 (10 in)", "273.1", "mm", "nominal outside diameter"),
                row("DN 300 (12 in)", "323.9", "mm", "nominal outside diameter"),
                row("DN 350 (14 in)", "355.6", "mm", "nominal outside diameter"),
                row("DN 400 (16 in)", "406.4", "mm", "nominal outside diameter"),
                row("DN 450 (18 in)", "457.2", "mm", "nominal outside diameter"),
                row("DN 500 (20 in)", "508.0", "mm", "nominal outside diameter"),
                row("DN 550 (22 in)", "558.8", "mm", "nominal outside diameter"),
                row("DN 600 (24 in)", "609.6", "mm", "nominal outside diameter"),
            ),
        ),
        BuiltInDataset(
            name = "Material densities (typical)",
            category = "Materials",
            source = "Common engineering values",
            licenseType = "generic",
            licenseNotes = "Typical values for preliminary work - use the project's material certificates where they matter.",
            rows = listOf(
                row("Carbon steel", "7850", "kg/m3"),
                row("Stainless steel (304)", "8000", "kg/m3"),
                row("Aluminium", "2700", "kg/m3"),
                row("Copper", "8960", "kg/m3"),
                row("Cast iron", "7200", "kg/m3"),
                row("Concrete (normal)", "2400", "kg/m3"),
                row("Water (4 C)", "1000", "kg/m3"),
                row("Sea water", "1025", "kg/m3"),
                row("Air (20 C, 1 atm)", "1.204", "kg/m3"),
            ),
        ),
        BuiltInDataset(
            name = "Water properties at 1 atm",
            category = "Fluids",
            source = "Textbook-common values",
            licenseType = "generic",
            licenseNotes = "Rounded values for hand checks; for design use a validated property library.",
            rows = listOf(
                row("Density at 4 C", "1000", "kg/m3"),
                row("Density at 20 C", "998.2", "kg/m3"),
                row("Kinematic viscosity at 20 C", "1.004", "mm2/s", "1.004 x 10^-6 m2/s"),
                row("Kinematic viscosity at 40 C", "0.658", "mm2/s"),
                row("Specific heat at 20 C", "4.182", "kJ/(kg.K)"),
                row("Vapour pressure at 20 C", "2.339", "kPa"),
                row("Vapour pressure at 40 C", "7.384", "kPa"),
                row("Vapour pressure at 60 C", "19.94", "kPa"),
            ),
        ),
        BuiltInDataset(
            name = "Physical constants",
            category = "General",
            source = "SI / CODATA definitions",
            licenseType = "public-domain",
            licenseNotes = "Published definitions and CODATA values.",
            rows = listOf(
                row("Standard gravity g", "9.80665", "m/s2"),
                row("Universal gas constant R", "8.314462618", "J/(mol.K)"),
                row("Standard atmosphere", "101325", "Pa"),
                row("Water density (maximum)", "1000", "kg/m3"),
                row("Air density (20 C, 1 atm)", "1.204", "kg/m3"),
                row("Molar mass of dry air", "28.965", "g/mol"),
                row("Specific heat of air cp (20 C)", "1.005", "kJ/(kg.K)"),
                row("Specific heat ratio of air k", "1.4", "-"),
            ),
        ),
        BuiltInDataset(
            name = "Pipe absolute roughness (typical)",
            category = "Piping",
            source = "Common piping practice values",
            licenseType = "generic",
            licenseNotes = "Typical values used for preliminary head-loss work - verify against the project's pipe data.",
            rows = listOf(
                row("Drawn tubing (new)", "0.0015", "mm"),
                row("Commercial steel (new)", "0.045", "mm"),
                row("Galvanised steel", "0.15", "mm"),
                row("Cast iron (new)", "0.26", "mm"),
                row("Concrete (new)", "0.3", "mm"),
                row("Riveted steel", "1.0", "mm"),
                row("PVC / HDPE", "0.007", "mm"),
                row("Copper", "0.0015", "mm"),
            ),
        ),
    )
}
