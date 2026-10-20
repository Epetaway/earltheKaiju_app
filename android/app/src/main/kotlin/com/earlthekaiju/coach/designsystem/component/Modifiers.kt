package com.earlthekaiju.coach.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import com.earlthekaiju.coach.designsystem.theme.KaijuSpace

/** Lays a child out rotated -90° (reading bottom-to-top), swapping its width and height. */
fun Modifier.verticalRotated(): Modifier = this.layout { measurable, constraints ->
    val placeable = measurable.measure(
        Constraints(maxWidth = constraints.maxHeight, maxHeight = constraints.maxWidth),
    )
    layout(placeable.height, placeable.width) {
        placeable.placeWithLayer(
            x = -(placeable.width - placeable.height) / 2,
            y = (placeable.width - placeable.height) / 2,
        ) { rotationZ = -90f }
    }
}

/** Keeps reading columns comfortable on tablets and in landscape. */
fun Modifier.readableWidth(): Modifier = this.widthIn(max = KaijuSpace.maxContentWidth).fillMaxWidth()
