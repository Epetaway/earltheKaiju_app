package com.earlthekaiju.coach.domain.model

/** Lifecycle of one scheduled class. Stored by name; never reorder or rename without a migration. */
enum class SessionStatus(val label: String) {
    SCHEDULED("Scheduled"),
    IN_PROGRESS("In progress"),
    COMPLETED("Completed"),
    CANCELED("Canceled"),
}

enum class AttendanceStatus(val label: String) {
    PRESENT("Present"),
    ABSENT("Absent"),
}

enum class Audience(val label: String) {
    ADULTS("Adults"),
    KIDS("Kids"),
    TEENS("Teens"),
    ALL_AGES("All ages"),
    PRIVATE("Private lessons"),
}

/**
 * A light label for a lesson block. It only drives the icon/label in Class Mode;
 * segment titles are free text, so no teaching structure is imposed.
 */
enum class SegmentKind(val label: String) {
    WARM_UP("Warm-up"),
    INSTRUCTION("Instruction"),
    DRILLING("Drilling"),
    POSITIONAL("Positional sparring"),
    SPARRING("Sparring"),
    REVIEW("Talk / review"),
    OTHER("Other"),
}

enum class StudentStatus(val label: String) {
    ACTIVE("Active"),
    ARCHIVED("Archived"),
}

/**
 * Accent shown beside a program in the planner. Always paired with the program name,
 * so color never carries meaning alone.
 */
enum class ProgramColor(val label: String) {
    EMERALD("Emerald"),
    GOLD("Gold"),
    IVORY("Ivory"),
    ACCENT("Light emerald"),
}

/**
 * IBJJF belt system: the kids' belts (with white/black center bars) and the adult belts.
 * [maxStripes] is 4 for colored belts; black belts use degrees (up to 6 here).
 */
enum class Belt(
    val label: String,
    /** ARGB of the belt itself (a physical color, not a brand color). */
    val colorArgb: Long,
    /** ARGB of the kids' center bar, if any. */
    val centerBarArgb: Long? = null,
    val isKids: Boolean = false,
    val maxStripes: Int = 4,
) {
    WHITE("White", BeltColors.White),
    GREY_WHITE("Grey-white", BeltColors.Grey, BeltColors.White, isKids = true),
    GREY("Grey", BeltColors.Grey, isKids = true),
    GREY_BLACK("Grey-black", BeltColors.Grey, BeltColors.Black, isKids = true),
    YELLOW_WHITE("Yellow-white", BeltColors.Yellow, BeltColors.White, isKids = true),
    YELLOW("Yellow", BeltColors.Yellow, isKids = true),
    YELLOW_BLACK("Yellow-black", BeltColors.Yellow, BeltColors.Black, isKids = true),
    ORANGE_WHITE("Orange-white", BeltColors.Orange, BeltColors.White, isKids = true),
    ORANGE("Orange", BeltColors.Orange, isKids = true),
    ORANGE_BLACK("Orange-black", BeltColors.Orange, BeltColors.Black, isKids = true),
    GREEN_WHITE("Green-white", BeltColors.Green, BeltColors.White, isKids = true),
    GREEN("Green", BeltColors.Green, isKids = true),
    GREEN_BLACK("Green-black", BeltColors.Green, BeltColors.Black, isKids = true),
    BLUE("Blue", 0xFF2D5DA8),
    PURPLE("Purple", 0xFF6B3FA0),
    BROWN("Brown", 0xFF6E4A2F),
    BLACK("Black", BeltColors.Black, maxStripes = 6),
    ;

    /** Black belts carry a red rank bar; every other belt a black one. */
    val rankBarArgb: Long get() = if (this == BLACK) 0xFFB3261E else BeltColors.Black

    fun describe(stripes: Int): String = when {
        stripes <= 0 -> "$label belt"
        this == BLACK -> "$label belt, ${ordinal(stripes)} degree"
        stripes == 1 -> "$label belt, 1 stripe"
        else -> "$label belt, $stripes stripes"
    }

    companion object {
        val adults = listOf(WHITE, BLUE, PURPLE, BROWN, BLACK)
        val kids = entries.filter { it.isKids || it == WHITE }

        private fun ordinal(n: Int) = when (n) {
            1 -> "1st"; 2 -> "2nd"; 3 -> "3rd"; else -> "${n}th"
        }
    }
}

private object BeltColors {
    const val White = 0xFFEDEBE4
    const val Grey = 0xFF8C918E
    const val Yellow = 0xFFD9B92E
    const val Orange = 0xFFD9772B
    const val Green = 0xFF3E8C4A
    const val Black = 0xFF111312
}
