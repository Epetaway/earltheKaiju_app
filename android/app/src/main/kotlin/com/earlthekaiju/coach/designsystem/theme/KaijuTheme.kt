package com.earlthekaiju.coach.designsystem.theme

import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import android.provider.Settings

/** Access point for the design system: `Kaiju.colors`, `Kaiju.type`. */
object Kaiju {
    val colors: KaijuColors
        @Composable @ReadOnlyComposable get() = LocalKaijuColors.current
    val type: KaijuType
        @Composable @ReadOnlyComposable get() = LocalKaijuType.current
    val reduceMotion: Boolean
        @Composable @ReadOnlyComposable get() = LocalReduceMotion.current
}

/** True when the system animator scale is 0 ("Remove animations"). Motion is skipped. */
val LocalReduceMotion = staticCompositionLocalOf { false }

@Composable
fun KaijuTheme(
    reduceMotion: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val colors = ShadowColors
    val type = DefaultKaijuType
    val context = LocalContext.current
    val systemReduceMotion = reduceMotion ?: runCatching {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }.getOrDefault(false)

    // Material 3 components (date/time pickers, snackbars, sheets) read this scheme.
    // `primary` is the accent because M3 draws it as text on surfaces; brand Emerald
    // (3.65:1 on Shadow) is reserved for fills with Ivory labels.
    val scheme = darkColorScheme(
        primary = colors.accent,
        onPrimary = colors.background,
        primaryContainer = colors.actionPressed,
        onPrimaryContainer = colors.ink,
        secondary = colors.gold,
        onSecondary = colors.background,
        secondaryContainer = colors.surfaceInset,
        onSecondaryContainer = colors.ink,
        tertiary = colors.live,
        onTertiary = colors.onLive,
        background = colors.background,
        onBackground = colors.ink,
        surface = colors.surface,
        onSurface = colors.ink,
        surfaceVariant = colors.surfaceInset,
        onSurfaceVariant = colors.inkMuted,
        surfaceContainerLowest = colors.background,
        surfaceContainerLow = colors.surface,
        surfaceContainer = colors.surface,
        surfaceContainerHigh = colors.surfaceInset,
        surfaceContainerHighest = colors.surfaceInset,
        inverseSurface = colors.ink,
        inverseOnSurface = colors.background,
        inversePrimary = colors.actionPressed,
        outline = colors.lineStrong,
        outlineVariant = colors.line,
        error = colors.signal,
        onError = colors.onSignal,
        scrim = colors.scrim,
    )
    val typography = Typography(
        displayLarge = type.displayL,
        displayMedium = type.displayM,
        headlineLarge = type.heading,
        headlineMedium = type.heading,
        headlineSmall = type.title,
        titleLarge = type.title,
        titleMedium = type.titleSmall,
        titleSmall = type.label,
        bodyLarge = type.body,
        bodyMedium = type.bodyS,
        bodySmall = type.caption,
        labelLarge = type.label,
        labelMedium = type.caption,
        labelSmall = type.caption,
    )
    val shapes = Shapes(
        extraSmall = KaijuShapes.Small,
        small = KaijuShapes.Small,
        medium = KaijuShapes.Small,
        large = KaijuShapes.Square,
        extraLarge = KaijuShapes.Square,
    )
    CompositionLocalProvider(
        LocalKaijuColors provides colors,
        LocalKaijuType provides type,
        LocalReduceMotion provides systemReduceMotion,
    ) {
        MaterialTheme(colorScheme = scheme, typography = typography, shapes = shapes) {
            CompositionLocalProvider(
                LocalTextSelectionColors provides TextSelectionColors(
                    handleColor = colors.live,
                    backgroundColor = colors.accent.copy(alpha = 0.35f),
                ),
                content = content,
            )
        }
    }
}
