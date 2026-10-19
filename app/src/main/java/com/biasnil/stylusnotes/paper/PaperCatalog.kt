package com.biasnil.stylusnotes.paper

enum class PaperCategory(val title: String) {
    STANDARD("Standard"),
    GRID("Grid"),
    MATH("Math & Engineering"),
    MUSIC("Music"),
    SPORTS("Sports"),
    PLANNERS("Planners & Lists"),
}

data class PaperSpec(
    val id: String,
    val name: String,
    val category: PaperCategory,
    /** Sports fields default to a landscape page. */
    val landscape: Boolean = false,
)

/** Page sizes in points (1/72 inch). */
enum class PaperSize(val label: String, val width: Float, val height: Float) {
    LETTER("Letter (8.5 × 11 in)", 612f, 792f),
    A4("A4 (210 × 297 mm)", 595.28f, 841.89f),
}

/** Every paper background, in the order the picker shows them. Drawing lives in PaperRenderer. */
object PaperCatalog {

    const val DEFAULT_ID = "college"

    val all: List<PaperSpec> = buildList {
        fun put(id: String, name: String, cat: PaperCategory, landscape: Boolean = false) =
            add(PaperSpec(id, name, cat, landscape))

        val s = PaperCategory.STANDARD
        put("blank", "Blank", s)
        put("college", "College ruled", s)
        put("wide", "Wide ruled", s)
        put("narrow", "Narrow ruled", s)
        put("cornell_basic", "Cornell basic", s)
        put("cornell_basic_college", "Cornell basic (college ruled)", s)
        put("cornell_styled", "Cornell styled", s)
        put("cornell_styled_college", "Cornell styled (college ruled)", s)

        val g = PaperCategory.GRID
        put("graph_4", "Graph (4 sq/in)", g)
        put("graph_4_bold", "Graph (4 sq/in) - bold", g)
        put("graph_5", "Graph (5 sq/in)", g)
        put("graph_5_bold", "Graph (5 sq/in) - bold", g)
        put("graph_1mm_bold", "Graph (1 mm) - bold", g)
        put("graph_5mm", "Graph (5 mm)", g)
        put("graph_1cm", "Graph (1 cm)", g)
        put("cross_4", "Cross grid (4/in)", g)
        put("cross_5", "Cross grid (5/in)", g)
        put("dot_4", "Dot grid (4/in)", g)
        put("dot_5", "Dot grid (5/in)", g)
        put("iso_grid", "Isometric grid", g)
        put("iso_dots", "Isometric dots", g)

        val m = PaperCategory.MATH
        put("engineering", "Engineering", m)
        put("polar", "Polar", m)
        put("polar_deg", "Polar (degrees)", m)
        put("polar_rad", "Polar (radians)", m)
        put("loglog", "Log-log", m)
        put("semilogx_4", "Semi-log X (4/in)", m)
        put("semilogx_4_bold", "Semi-log X (4/in) - bold", m)
        put("semilogx_5", "Semi-log X (5/in)", m)
        put("semilogx_5_bold", "Semi-log X (5/in) - bold", m)
        put("semilogy_4", "Semi-log Y (4/in)", m)
        put("semilogy_4_bold", "Semi-log Y (4/in) - bold", m)
        put("semilogy_5", "Semi-log Y (5/in)", m)
        put("semilogy_5_bold", "Semi-log Y (5/in) - bold", m)

        val mu = PaperCategory.MUSIC
        put("staff", "Staff", mu)
        put("double_staff", "Double staff", mu)
        put("grand_staff", "Grand staff", mu)
        put("treble_staff", "Treble staff", mu)
        put("bass_staff", "Bass staff", mu)

        val sp = PaperCategory.SPORTS
        put("basketball_hs", "Basketball court HS", sp, true)
        put("basketball_half_hs", "Basketball half-court HS", sp, true)
        put("football", "Football field", sp, true)
        put("football_red_zone", "Football field red zone", sp, true)
        put("baseball", "Baseball field", sp, true)
        put("baseball_score", "Baseball stat/score sheet", sp, true)
        put("cricket", "Cricket field", sp, true)
        put("soccer", "Soccer field", sp, true)
        put("soccer_half", "Soccer half-field", sp, true)
        put("rugby_union", "Rugby union field", sp, true)
        put("rugby_league", "Rugby league field", sp, true)
        put("field_hockey", "Field hockey field", sp, true)
        put("hockey_intl", "Hockey rink international", sp, true)
        put("hockey_usa", "Hockey rink USA", sp, true)
        put("tennis", "Tennis court", sp, true)
        put("volleyball", "Volleyball court", sp, true)
        put("volleyball_half", "Volleyball half-court", sp, true)
        put("pickleball", "Pickleball court", sp, true)

        val p = PaperCategory.PLANNERS
        put("daily_blank", "Daily (blank)", p)
        put("daily_college", "Daily (college ruled)", p)
        put("daily_dot", "Daily (dot grid)", p)
        put("weekly_blank", "Weekly (blank)", p)
        put("weekly_college", "Weekly (college ruled)", p)
        put("weekly_dot", "Weekly (dot grid)", p)
        put("weekly_cols_5", "Weekly columns (5 days)", p)
        put("weekly_cols_7", "Weekly columns (7 days)", p)
        put("monthly_5", "Monthly (5 weeks)", p)
        put("monthly_6", "Monthly (6 weeks)", p)
        put("tasks", "Task list", p)
        put("tasks_2col", "Task list (2 columns)", p)
    }

    private val byId = all.associateBy { it.id }

    fun contains(id: String) = id in byId

    fun find(id: String): PaperSpec = byId[id] ?: byId.getValue("blank")

    fun inCategory(category: PaperCategory) = all.filter { it.category == category }

    /** Width and height for a new page of this paper. */
    fun pageSize(spec: PaperSpec, size: PaperSize): Pair<Float, Float> =
        if (spec.landscape) size.height to size.width else size.width to size.height
}
