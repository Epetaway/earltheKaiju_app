package com.earlthekaiju.coach.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.earlthekaiju.coach.R

/** Barlow Condensed (SIL OFL): display, large numbers, section labels. */
val BarlowCondensed = FontFamily(
    Font(R.font.barlow_condensed_medium, FontWeight.Medium),
    Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_condensed_bold, FontWeight.Bold),
    Font(R.font.barlow_condensed_extrabold, FontWeight.ExtraBold),
)

/** Manrope (SIL OFL): body, forms, navigation labels, data. */
val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
    Font(R.font.manrope_extrabold, FontWeight.ExtraBold),
)

private val TightDisplay = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both,
)

/** Tabular figures so numbers never shift width while they count. */
const val TabularNumerals = "tnum"

/**
 * Type scale from brand guidelines v1.5 (page 17), extended with app-specific roles
 * (titles, data, numerals). All sizes are in sp so they follow the system font scale.
 */
@Immutable
data class KaijuType(
    val displayXL: TextStyle,
    val displayL: TextStyle,
    val displayM: TextStyle,
    val heading: TextStyle,
    val title: TextStyle,
    val titleSmall: TextStyle,
    val eyebrow: TextStyle,
    val index: TextStyle,
    val bodyL: TextStyle,
    val body: TextStyle,
    val bodyS: TextStyle,
    val label: TextStyle,
    val labelL: TextStyle,
    val caption: TextStyle,
    val navLabel: TextStyle,
    val numeralL: TextStyle,
    val numeralM: TextStyle,
    /** Timer hero. Size is set by the timer layout from the available space. */
    val numeralHero: TextStyle,
)

val DefaultKaijuType = KaijuType(
    displayXL = TextStyle(
        fontFamily = BarlowCondensed, fontWeight = FontWeight.ExtraBold,
        fontSize = 96.sp, lineHeight = 88.sp, letterSpacing = 0.01.em, lineHeightStyle = TightDisplay,
    ),
    displayL = TextStyle(
        fontFamily = BarlowCondensed, fontWeight = FontWeight.ExtraBold,
        fontSize = 64.sp, lineHeight = 61.sp, letterSpacing = 0.01.em, lineHeightStyle = TightDisplay,
    ),
    displayM = TextStyle(
        fontFamily = BarlowCondensed, fontWeight = FontWeight.Bold,
        fontSize = 40.sp, lineHeight = 40.sp, letterSpacing = 0.01.em, lineHeightStyle = TightDisplay,
    ),
    heading = TextStyle(
        fontFamily = BarlowCondensed, fontWeight = FontWeight.Bold,
        fontSize = 28.sp, lineHeight = 31.sp, letterSpacing = 0.01.em,
    ),
    title = TextStyle(
        fontFamily = BarlowCondensed, fontWeight = FontWeight.Bold,
        fontSize = 22.sp, lineHeight = 26.sp, letterSpacing = 0.015.em,
    ),
    titleSmall = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.Bold,
        fontSize = 16.sp, lineHeight = 22.sp,
    ),
    eyebrow = TextStyle(
        fontFamily = BarlowCondensed, fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = 0.22.em,
    ),
    index = TextStyle(
        fontFamily = BarlowCondensed, fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = 0.12.em,
        fontFeatureSettings = TabularNumerals,
    ),
    bodyL = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 28.sp),
    body = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyS = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    label = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelL = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp),
    caption = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    navLabel = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.01.em,
    ),
    numeralL = TextStyle(
        fontFamily = BarlowCondensed, fontWeight = FontWeight.Bold,
        fontSize = 48.sp, lineHeight = 48.sp, fontFeatureSettings = TabularNumerals, lineHeightStyle = TightDisplay,
    ),
    numeralM = TextStyle(
        fontFamily = BarlowCondensed, fontWeight = FontWeight.Bold,
        fontSize = 28.sp, lineHeight = 30.sp, fontFeatureSettings = TabularNumerals,
    ),
    numeralHero = TextStyle(
        fontFamily = BarlowCondensed, fontWeight = FontWeight.ExtraBold,
        fontSize = 200.sp, lineHeight = 200.sp, fontFeatureSettings = TabularNumerals, lineHeightStyle = TightDisplay,
    ),
)

val LocalKaijuType = staticCompositionLocalOf { DefaultKaijuType }
