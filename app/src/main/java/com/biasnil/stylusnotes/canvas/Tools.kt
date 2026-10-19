package com.biasnil.stylusnotes.canvas

enum class Tool { PEN, HIGHLIGHTER, ERASER, SELECT }

/** Pen-button actions that act on the open page rather than on the tool. */
enum class PenCommand { UNDO, REDO }

/** Widths are in page units (points), so ink scales with zoom. */
data class ToolSettings(
    val tool: Tool = Tool.PEN,
    /** Tool to go back to when the pen's side button toggles the eraser off. */
    val previous: Tool = Tool.PEN,
    val penColor: Int = 0xFF111111.toInt(),
    val penWidth: Float = 1.8f,
    val highlighterColor: Int = 0xFFFFD54F.toInt(),
    val highlighterWidth: Float = 12f,
    /** Colours picked with the colour picker, newest first. */
    val recentColors: List<Int> = emptyList(),
) {
    /** Remembers a picked colour (up to 8). */
    fun withRecent(color: Int): ToolSettings =
        copy(recentColors = (listOf(color) + recentColors.filter { it != color }).take(8))

    fun toggleEraser(): ToolSettings =
        if (tool == Tool.ERASER) copy(tool = previous) else copy(tool = Tool.ERASER, previous = tool)

    fun select(t: Tool): ToolSettings =
        copy(tool = t, previous = if (t == Tool.ERASER) tool else t)

    companion object {
        val PEN_WIDTHS = listOf(1.0f, 1.8f, 3.0f)
        val HIGHLIGHTER_WIDTHS = listOf(8f, 12f, 18f)
        val PEN_COLORS = listOf(
            0xFF111111.toInt(), 0xFF1F4FB8.toInt(), 0xFFC0392B.toInt(),
            0xFF1E8449.toInt(), 0xFF6A3FB5.toInt(), 0xFFE67E22.toInt(),
        )
        val HIGHLIGHTER_COLORS = listOf(
            0xFFFFD54F.toInt(), 0xFF81C784.toInt(), 0xFFF48FB1.toInt(), 0xFF64B5F6.toInt(),
        )
    }
}
