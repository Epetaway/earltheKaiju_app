package com.earlthekaiju.coach.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.earlthekaiju.coach.designsystem.theme.KaijuSpace

/** True when the user's font size is large enough that side-by-side layouts should stack. */
@Composable
@ReadOnlyComposable
fun isLargeFont(): Boolean = LocalDensity.current.fontScale > 1.3f

/**
 * One line of text that shrinks (down to [minSize]) instead of truncating. For short labels in
 * fixed-width slots: navigation labels, quick actions, stat labels.
 */
@Composable
fun FitText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    minSize: TextUnit = 8.sp,
    textAlign: TextAlign = TextAlign.Unspecified,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.merge(TextStyle(color = color, textAlign = textAlign)),
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis, // only if it still doesn't fit at minSize
        autoSize = TextAutoSize.StepBased(minFontSize = minSize, maxFontSize = style.fontSize, stepSize = 0.5.sp),
    )
}

/** Two equal buttons side by side, stacked full-width when the font is large. */
@Composable
fun ButtonPair(
    modifier: Modifier = Modifier,
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit,
) {
    if (isLargeFont()) {
        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(KaijuSpace.s)) {
            first(Modifier.fillMaxWidth())
            second(Modifier.fillMaxWidth())
        }
    } else {
        Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(KaijuSpace.s)) {
            first(Modifier.weight(1f))
            second(Modifier.weight(1f))
        }
    }
}
