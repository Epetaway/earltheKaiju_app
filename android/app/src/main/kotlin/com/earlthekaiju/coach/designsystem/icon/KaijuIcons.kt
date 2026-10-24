package com.earlthekaiju.coach.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Kaiju Coach icon set. Original, drawn for this app on a 24dp grid:
 * 1.75dp strokes, square caps, mitred joins and straight geometry to match the
 * crest's line-work. Filled shapes are reserved for transport controls (play, pause, skip)
 * so they read instantly at a distance. Icons are tinted by the caller.
 */
object KaijuIcons {
    val Today by lazy {
        icon("Today", stroke = listOf("M4 6h16v14H4z", "M4 10h16", "M8 3v4", "M16 3v4"),
            fill = listOf("M12 12.25l2.75 2.75-2.75 2.75-2.75-2.75z"))
    }
    val Planner by lazy {
        icon("Planner", stroke = listOf("M4 6h16v14H4z", "M4 10h16", "M8 3v4", "M16 3v4", "M9.33 10v10", "M14.67 10v10", "M4 15h16"))
    }
    val Timer by lazy {
        icon("Timer", stroke = listOf("M19 13a7 7 0 1 1-14 0a7 7 0 1 1 14 0z", "M10 3h4", "M12 3v3", "M12 13V9.25", "M18.25 6.25l-1.5 1.5"))
    }
    val Students by lazy {
        icon("Students", stroke = listOf(
            "M12 8a3 3 0 1 1-6 0a3 3 0 1 1 6 0z",
            "M3.5 19.5c0-3.1 2.5-5.25 5.5-5.25s5.5 2.15 5.5 5.25",
            "M15 5.2a3 3 0 0 1 0 5.6",
            "M17 14.4c2 .6 3.5 2.3 3.5 5.1",
        ))
    }
    val Library by lazy {
        icon("Library", stroke = listOf("M4 4h4v16H4z", "M10 4h4v16h-4z", "M16 5l3.4-.9 3.1 15.4-3.4.9z"))
    }
    val Lessons by lazy {
        icon("Lessons", stroke = listOf("M9.5 6h10.5", "M9.5 12h10.5", "M9.5 18h10.5"),
            fill = listOf("M4 5h3v3H4z", "M4 11h3v3H4z", "M4 17h3v3H4z"))
    }
    val Technique by lazy {
        icon("Technique", stroke = listOf(
            "M12 6.5C10 5 7 4.5 4 5v14c3-.5 6 0 8 1.5 2-1.5 5-2 8-1.5V5c-3-.5-6 0-8 1.5z",
            "M12 6.5v14",
        ))
    }
    val Add by lazy { icon("Add", stroke = listOf("M12 5v14", "M5 12h14")) }
    val Minus by lazy { icon("Minus", stroke = listOf("M5 12h14")) }
    val Close by lazy { icon("Close", stroke = listOf("M6 6l12 12", "M18 6L6 18")) }
    val Check by lazy { icon("Check", stroke = listOf("M5 12.5l4.5 4.5L19 7.5")) }
    val Back by lazy { icon("Back", stroke = listOf("M19 12H5.5", "M11 6l-6 6 6 6")) }
    val ChevronRight by lazy { icon("ChevronRight", stroke = listOf("M9 6l6 6-6 6")) }
    val ChevronLeft by lazy { icon("ChevronLeft", stroke = listOf("M15 6l-6 6 6 6")) }
    val ChevronDown by lazy { icon("ChevronDown", stroke = listOf("M6 9l6 6 6-6")) }
    val ArrowUp by lazy { icon("ArrowUp", stroke = listOf("M12 19V5.5", "M6 11l6-6 6 6")) }
    val ArrowDown by lazy { icon("ArrowDown", stroke = listOf("M12 5v13.5", "M6 13l6 6 6-6")) }
    val Play by lazy { icon("Play", fill = listOf("M7.5 4.5v15l12-7.5z")) }
    val Pause by lazy { icon("Pause", fill = listOf("M6.5 5h4v14h-4z", "M13.5 5h4v14h-4z")) }
    val SkipNext by lazy { icon("SkipNext", fill = listOf("M5 6l9 6-9 6z", "M16 6h2.5v12H16z")) }
    val SkipPrevious by lazy { icon("SkipPrevious", fill = listOf("M19 6l-9 6 9 6z", "M5.5 6H8v12H5.5z")) }
    val Reset by lazy { icon("Reset", stroke = listOf("M5 12a7 7 0 1 0 2.05-4.95", "M10.55 7.05H7.05V3.55")) }
    val Settings by lazy {
        icon("Settings", stroke = listOf("M4 7h8", "M14 4.5v5", "M16 7h4", "M4 17h4", "M10 14.5v5", "M12 17h8"))
    }
    val Search by lazy { icon("Search", stroke = listOf("M17 10.5a6.5 6.5 0 1 1-13 0a6.5 6.5 0 1 1 13 0z", "M15.25 15.25L20 20")) }
    val Edit by lazy { icon("Edit", stroke = listOf("M4 20h4L19.5 8.5l-4-4L4 16z", "M13.5 6.5l4 4")) }
    val Delete by lazy {
        icon("Delete", stroke = listOf("M5 7h14", "M10 4h4", "M7 7l1 13h8l1-13", "M10.5 11v5.5", "M13.5 11v5.5"))
    }
    val More by lazy { icon("More", fill = listOf("M11 4.5h2.25v2.25H11z", "M11 10.9h2.25v2.25H11z", "M11 17.25h2.25v2.25H11z")) }
    val Note by lazy { icon("Note", stroke = listOf("M5 4h10l4 4v12H5z", "M15 4v4h4", "M8 12h8", "M8 16h5.5")) }
    val Attendance by lazy {
        icon("Attendance", stroke = listOf("M7.5 5H5v15h14V5h-2.5", "M9 3h6v4H9z", "M8.5 13.5l2.5 2.5 4.5-5"))
    }
    val Clock by lazy { icon("Clock", stroke = listOf("M20 12a8 8 0 1 1-16 0a8 8 0 1 1 16 0z", "M12 7.5V12l3 2")) }
    val Sync by lazy {
        icon("Sync", stroke = listOf(
            "M5 10a7 7 0 0 1 12.6-3.2", "M19 14a7 7 0 0 1-12.6 3.2", "M17.75 3v4h-4", "M6.25 21v-4h4",
        ))
    }
    val Phone by lazy { icon("Phone", stroke = listOf("M7 3h10v18H7z", "M11 17.75h2")) }
    val Desktop by lazy { icon("Desktop", stroke = listOf("M3 5h18v11H3z", "M8 20h8", "M12 16v4")) }
    val Warning by lazy { icon("Warning", stroke = listOf("M12 4l9 16H3z", "M12 10v4.25", "M12 16.9v.6")) }
    val Info by lazy { icon("Info", stroke = listOf("M20 12a8 8 0 1 1-16 0a8 8 0 1 1 16 0z", "M12 11v5", "M12 7.9v.6")) }
    val Flag by lazy { icon("Flag", stroke = listOf("M6 21V4", "M6 4.5h12l-3 4.25 3 4.25H6")) }
    val Undo by lazy { icon("Undo", stroke = listOf("M9 5L4 10l5 5", "M4.5 10H14a5 5 0 0 1 0 10h-3")) }
    val Export by lazy { icon("Export", stroke = listOf("M12 15V4.5", "M7 9l5-5 5 5", "M5 15v5h14v-5")) }
    val Program by lazy { icon("Program", stroke = listOf("M12 4l9 5-9 5-9-5z", "M3 14.5l9 5 9-5")) }
    val Video by lazy { icon("Video", stroke = listOf("M3 5h18v14H3z"), fill = listOf("M10 8.75v6.5l5.25-3.25z")) }
    val Shield by lazy {
        icon("Shield", stroke = listOf("M12 3l8 3v6c0 4.5-3.5 8-8 9-4.5-1-8-4.5-8-9V6z", "M8.5 12l2.5 2.5 4.5-5"))
    }
    val SoundOn by lazy {
        icon("SoundOn", stroke = listOf("M4 9h4l5-4v14l-5-4H4z", "M16 9a4 4 0 0 1 0 6", "M18.5 6.5a7.5 7.5 0 0 1 0 11"))
    }
    val SoundOff by lazy { icon("SoundOff", stroke = listOf("M4 9h4l5-4v14l-5-4H4z", "M16 9.5l5 5", "M21 9.5l-5 5")) }
    val Vibrate by lazy { icon("Vibrate", stroke = listOf("M8 4h8v16H8z", "M4.5 8.5v7", "M19.5 8.5v7")) }
    val Location by lazy {
        icon("Location", stroke = listOf("M12 21s-6.5-6.2-6.5-11a6.5 6.5 0 0 1 13 0c0 4.8-6.5 11-6.5 11z", "M14 10a2 2 0 1 1-4 0a2 2 0 1 1 4 0z"))
    }
    val Archive by lazy { icon("Archive", stroke = listOf("M3.5 4h17v4h-17z", "M5 8v12h14V8", "M10 12h4")) }
    val Copy by lazy { icon("Copy", stroke = listOf("M8 8h12v12H8z", "M16 8V4H4v12h4")) }
}

private fun icon(
    name: String,
    stroke: List<String> = emptyList(),
    fill: List<String> = emptyList(),
): ImageVector {
    val builder = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    )
    stroke.forEach { d ->
        builder.addPath(
            pathData = addPathNodes(d),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.75f,
            strokeLineCap = StrokeCap.Square,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4f,
        )
    }
    fill.forEach { d ->
        builder.addPath(pathData = addPathNodes(d), fill = SolidColor(Color.Black))
    }
    return builder.build()
}
