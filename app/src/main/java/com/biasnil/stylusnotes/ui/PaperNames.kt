package com.biasnil.stylusnotes.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.biasnil.stylusnotes.R
import com.biasnil.stylusnotes.paper.PaperCategory

// Translated names for paper backgrounds. Strings live in res/values*/strings.xml as paper_<id>.

@Composable
fun paperName(id: String): String = stringResource(paperNameRes(id))

fun paperNameRes(id: String): Int = when (id) {
    "blank" -> R.string.paper_blank
    "college" -> R.string.paper_college
    "wide" -> R.string.paper_wide
    "narrow" -> R.string.paper_narrow
    "cornell_basic" -> R.string.paper_cornell_basic
    "cornell_basic_college" -> R.string.paper_cornell_basic_college
    "cornell_styled" -> R.string.paper_cornell_styled
    "cornell_styled_college" -> R.string.paper_cornell_styled_college
    "graph_4" -> R.string.paper_graph_4
    "graph_4_bold" -> R.string.paper_graph_4_bold
    "graph_5" -> R.string.paper_graph_5
    "graph_5_bold" -> R.string.paper_graph_5_bold
    "graph_1mm_bold" -> R.string.paper_graph_1mm_bold
    "graph_5mm" -> R.string.paper_graph_5mm
    "graph_1cm" -> R.string.paper_graph_1cm
    "cross_4" -> R.string.paper_cross_4
    "cross_5" -> R.string.paper_cross_5
    "dot_4" -> R.string.paper_dot_4
    "dot_5" -> R.string.paper_dot_5
    "iso_grid" -> R.string.paper_iso_grid
    "iso_dots" -> R.string.paper_iso_dots
    "engineering" -> R.string.paper_engineering
    "polar" -> R.string.paper_polar
    "polar_deg" -> R.string.paper_polar_deg
    "polar_rad" -> R.string.paper_polar_rad
    "loglog" -> R.string.paper_loglog
    "semilogx_4" -> R.string.paper_semilogx_4
    "semilogx_4_bold" -> R.string.paper_semilogx_4_bold
    "semilogx_5" -> R.string.paper_semilogx_5
    "semilogx_5_bold" -> R.string.paper_semilogx_5_bold
    "semilogy_4" -> R.string.paper_semilogy_4
    "semilogy_4_bold" -> R.string.paper_semilogy_4_bold
    "semilogy_5" -> R.string.paper_semilogy_5
    "semilogy_5_bold" -> R.string.paper_semilogy_5_bold
    "staff" -> R.string.paper_staff
    "double_staff" -> R.string.paper_double_staff
    "grand_staff" -> R.string.paper_grand_staff
    "treble_staff" -> R.string.paper_treble_staff
    "bass_staff" -> R.string.paper_bass_staff
    "basketball_hs" -> R.string.paper_basketball_hs
    "basketball_half_hs" -> R.string.paper_basketball_half_hs
    "football" -> R.string.paper_football
    "football_red_zone" -> R.string.paper_football_red_zone
    "baseball" -> R.string.paper_baseball
    "baseball_score" -> R.string.paper_baseball_score
    "cricket" -> R.string.paper_cricket
    "soccer" -> R.string.paper_soccer
    "soccer_half" -> R.string.paper_soccer_half
    "rugby_union" -> R.string.paper_rugby_union
    "rugby_league" -> R.string.paper_rugby_league
    "field_hockey" -> R.string.paper_field_hockey
    "hockey_intl" -> R.string.paper_hockey_intl
    "hockey_usa" -> R.string.paper_hockey_usa
    "tennis" -> R.string.paper_tennis
    "volleyball" -> R.string.paper_volleyball
    "volleyball_half" -> R.string.paper_volleyball_half
    "pickleball" -> R.string.paper_pickleball
    "daily_blank" -> R.string.paper_daily_blank
    "daily_college" -> R.string.paper_daily_college
    "daily_dot" -> R.string.paper_daily_dot
    "weekly_blank" -> R.string.paper_weekly_blank
    "weekly_college" -> R.string.paper_weekly_college
    "weekly_dot" -> R.string.paper_weekly_dot
    "weekly_cols_5" -> R.string.paper_weekly_cols_5
    "weekly_cols_7" -> R.string.paper_weekly_cols_7
    "monthly_5" -> R.string.paper_monthly_5
    "monthly_6" -> R.string.paper_monthly_6
    "tasks" -> R.string.paper_tasks
    "tasks_2col" -> R.string.paper_tasks_2col
    else -> R.string.paper_blank
}

@Composable
fun categoryName(c: PaperCategory): String = stringResource(
    when (c) {
        PaperCategory.STANDARD -> R.string.cat_standard
        PaperCategory.GRID -> R.string.cat_grid
        PaperCategory.MATH -> R.string.cat_math
        PaperCategory.MUSIC -> R.string.cat_music
        PaperCategory.SPORTS -> R.string.cat_sports
        PaperCategory.PLANNERS -> R.string.cat_planners
    },
)
