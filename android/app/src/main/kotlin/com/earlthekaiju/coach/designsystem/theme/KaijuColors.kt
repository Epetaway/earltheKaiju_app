package com.earlthekaiju.coach.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Raw brand values from Earl the Kaiju brand package v1.5 (06-design-tokens/tokens.json).
 * UI code never uses these directly; it reads the semantic roles in [KaijuColors].
 */
object KaijuPalette {
    val Shadow = Color(0xFF0B0F0E)
    val ShadowRaised = Color(0xFF17211D)
    val Facet = Color(0xFF1E2523)
    val Line = Color(0xFF26332D)
    val LineStrong = Color(0xFF5F6F67)
    val Emerald = Color(0xFF0B7A62)
    val EmeraldDeep = Color(0xFF0F5B4B)
    val SpectralEmerald = Color(0xFF17D190)
    val Gold = Color(0xFFD4AF37)
    val Ivory = Color(0xFFF4F1E9)
    val InkMuted = Color(0xFFA3B0A9)
    val Accent = Color(0xFF27A58B)
    val Signal = Color(0xFFF0775A)
}

/**
 * Semantic color roles. Every text/background pairing used in the app is listed with its
 * WCAG contrast ratio in docs/03-design-system.md; the comments note the ones that matter.
 */
@Immutable
data class KaijuColors(
    /** Page background (Shadow). */
    val background: Color,
    /** Cards and sheets (Raised Shadow). Ivory 14.6:1, muted 7.3:1, accent 5.4:1. */
    val surface: Color,
    /** Inset geometric fills: chips, steppers, rails (Facet). Ivory 13.8:1. */
    val surfaceInset: Color,
    /** Hairlines that separate surfaces (decorative, not meaningful). */
    val line: Color,
    /** Borders that carry meaning: input outlines, toggles. 3.6:1 on Shadow, 3.1:1 on Raised. */
    val lineStrong: Color,
    /** Primary text (Ivory). */
    val ink: Color,
    /** Secondary text. 8.6:1 on Shadow. */
    val inkMuted: Color,
    /** Primary action fill (Emerald). Ivory label 4.7:1. Never text on dark. */
    val action: Color,
    /** Pressed primary action (Deep Emerald). Ivory label 7.1:1. */
    val actionPressed: Color,
    val onAction: Color,
    /** Links, active text, selected icons on dark (UI-only accent). 6.3:1 on Shadow. */
    val accent: Color,
    /** Live states and focus rings (Spectral Emerald). Use in small doses. 9.7:1 on Shadow. */
    val live: Color,
    /** Text on a [live] fill. */
    val onLive: Color,
    /** Eyebrows, section indexes, rank. 9.2:1 on Shadow. Restrained. */
    val gold: Color,
    /** Errors and destructive actions. 6.9:1 on Shadow. Always paired with a word or icon. */
    val signal: Color,
    val onSignal: Color,
    /** Timer work-phase field (Deep Emerald). Ivory numerals 7.1:1. */
    val phaseWork: Color,
    /** Timer rest-phase field (Shadow) with gold labels. */
    val phaseRest: Color,
    /** Scrim behind sheets and dialogs. */
    val scrim: Color,
)

val ShadowColors = KaijuColors(
    background = KaijuPalette.Shadow,
    surface = KaijuPalette.ShadowRaised,
    surfaceInset = KaijuPalette.Facet,
    line = KaijuPalette.Line,
    lineStrong = KaijuPalette.LineStrong,
    ink = KaijuPalette.Ivory,
    inkMuted = KaijuPalette.InkMuted,
    action = KaijuPalette.Emerald,
    actionPressed = KaijuPalette.EmeraldDeep,
    onAction = KaijuPalette.Ivory,
    accent = KaijuPalette.Accent,
    live = KaijuPalette.SpectralEmerald,
    onLive = KaijuPalette.Shadow,
    gold = KaijuPalette.Gold,
    signal = KaijuPalette.Signal,
    onSignal = KaijuPalette.Shadow,
    phaseWork = KaijuPalette.EmeraldDeep,
    phaseRest = KaijuPalette.Shadow,
    scrim = Color(0xCC050707),
)

val LocalKaijuColors = staticCompositionLocalOf { ShadowColors }
