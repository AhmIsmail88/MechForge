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
            name = "Pipe wall thickness by schedule (ANSI B36.10M by schedule)",
            category = "Piping",
            source = "Table supplied by the user, headed: nominal wall thickness for seamless and welded steel pipes according ANSI B36.10",
            licenseType = "supplied by the user - verify against the purchased standard",
            licenseNotes = "Carbon and alloy steel (B36.10M) schedules only: the stainless schedules (5S/10S/40S/80S of B36.19M) are not in this table. Values are the mm column of the supplied table. The full table, including schedules 20/30/STD/60/XS/100/120/140/160/XXS and the inch values, is in tools/reference/pipe-schedule-b3610.csv and can be imported from the References screen.",
            rows = listOf(
                row("NPS 1/8 - Sch 40", "1.73", "mm", "outside diameter 10.3 mm"),
                row("NPS 1/8 - Sch 80", "2.41", "mm", "outside diameter 10.3 mm"),
                row("NPS 1/4 - Sch 40", "2.24", "mm", "outside diameter 13.7 mm"),
                row("NPS 1/4 - Sch 80", "3.02", "mm", "outside diameter 13.7 mm"),
                row("NPS 3/8 - Sch 40", "2.31", "mm", "outside diameter 17.1 mm"),
                row("NPS 3/8 - Sch 80", "3.2", "mm", "outside diameter 17.1 mm"),
                row("NPS 1/2 - Sch 40", "2.77", "mm", "outside diameter 21.3 mm"),
                row("NPS 1/2 - Sch 80", "3.73", "mm", "outside diameter 21.3 mm"),
                row("NPS 3/4 - Sch 40", "2.87", "mm", "outside diameter 26.7 mm"),
                row("NPS 3/4 - Sch 80", "3.91", "mm", "outside diameter 26.7 mm"),
                row("NPS 1 - Sch 40", "3.38", "mm", "outside diameter 33.4 mm"),
                row("NPS 1 - Sch 80", "4.55", "mm", "outside diameter 33.4 mm"),
                row("NPS 1 1/4 - Sch 40", "3.56", "mm", "outside diameter 42.2 mm"),
                row("NPS 1 1/4 - Sch 80", "4.85", "mm", "outside diameter 42.2 mm"),
                row("NPS 1 1/2 - Sch 40", "3.68", "mm", "outside diameter 48.3 mm"),
                row("NPS 1 1/2 - Sch 80", "5.08", "mm", "outside diameter 48.3 mm"),
                row("NPS 2 - Sch 40", "3.91", "mm", "outside diameter 60.3 mm"),
                row("NPS 2 - Sch 80", "5.54", "mm", "outside diameter 60.3 mm"),
                row("NPS 2 1/2 - Sch 40", "5.16", "mm", "outside diameter 73 mm"),
                row("NPS 2 1/2 - Sch 80", "7.01", "mm", "outside diameter 73 mm"),
                row("NPS 3 - Sch 40", "5.49", "mm", "outside diameter 88.9 mm"),
                row("NPS 3 - Sch 80", "7.62", "mm", "outside diameter 88.9 mm"),
                row("NPS 3 1/2 - Sch 40", "5.74", "mm", "outside diameter 102 mm"),
                row("NPS 3 1/2 - Sch 80", "8.08", "mm", "outside diameter 102 mm"),
                row("NPS 4 - Sch 40", "6.02", "mm", "outside diameter 114 mm"),
                row("NPS 4 - Sch 80", "8.56", "mm", "outside diameter 114 mm"),
                row("NPS 5 - Sch 40", "6.55", "mm", "outside diameter 141 mm"),
                row("NPS 5 - Sch 80", "9.52", "mm", "outside diameter 141 mm"),
                row("NPS 6 - Sch 40", "7.11", "mm", "outside diameter 168 mm"),
                row("NPS 6 - Sch 80", "11", "mm", "outside diameter 168 mm"),
                row("NPS 8 - Sch 40", "8.18", "mm", "outside diameter 219 mm"),
                row("NPS 8 - Sch 80", "12.7", "mm", "outside diameter 219 mm"),
                row("NPS 10 - Sch 40", "9.27", "mm", "outside diameter 273 mm"),
                row("NPS 10 - Sch 80", "15.1", "mm", "outside diameter 273 mm"),
                row("NPS 12 - Sch 40", "10.3", "mm", "outside diameter 324 mm"),
                row("NPS 12 - Sch 80", "17.5", "mm", "outside diameter 324 mm"),
                row("NPS 14 - Sch 10", "6.35", "mm", "outside diameter 356 mm"),
                row("NPS 14 - Sch 40", "11.1", "mm", "outside diameter 356 mm"),
                row("NPS 14 - Sch 80", "19", "mm", "outside diameter 356 mm"),
                row("NPS 16 - Sch 10", "6.35", "mm", "outside diameter 406 mm"),
                row("NPS 16 - Sch 40", "12.7", "mm", "outside diameter 406 mm"),
                row("NPS 16 - Sch 80", "21.4", "mm", "outside diameter 406 mm"),
                row("NPS 18 - Sch 10", "6.35", "mm", "outside diameter 457 mm"),
                row("NPS 18 - Sch 40", "14.3", "mm", "outside diameter 457 mm"),
                row("NPS 18 - Sch 80", "23.8", "mm", "outside diameter 457 mm"),
                row("NPS 20 - Sch 10", "6.35", "mm", "outside diameter 508 mm"),
                row("NPS 20 - Sch 40", "15.1", "mm", "outside diameter 508 mm"),
                row("NPS 20 - Sch 80", "26.2", "mm", "outside diameter 508 mm"),
                row("NPS 22 - Sch 10", "6.35", "mm", "outside diameter 559 mm"),
                row("NPS 22 - Sch 80", "28.6", "mm", "outside diameter 559 mm"),
                row("NPS 24 - Sch 10", "6.35", "mm", "outside diameter 610 mm"),
                row("NPS 24 - Sch 40", "17.5", "mm", "outside diameter 610 mm"),
                row("NPS 24 - Sch 80", "31", "mm", "outside diameter 610 mm"),
                row("NPS 30 - Sch 10", "7.92", "mm", "outside diameter 762 mm"),
                row("NPS 32 - Sch 10", "7.92", "mm", "outside diameter 813 mm"),
                row("NPS 32 - Sch 40", "17.5", "mm", "outside diameter 813 mm"),
                row("NPS 34 - Sch 10", "7.92", "mm", "outside diameter 864 mm"),
                row("NPS 34 - Sch 40", "17.5", "mm", "outside diameter 864 mm"),
                row("NPS 36 - Sch 10", "7.92", "mm", "outside diameter 914 mm"),
                row("NPS 36 - Sch 40", "19", "mm", "outside diameter 914 mm"),
                row("NPS 42 - Sch 40", "19", "mm", "outside diameter 1067 mm"),
            ),
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
