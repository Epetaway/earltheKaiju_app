package com.earlthekaiju.coach.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.earlthekaiju.coach.designsystem.icon.KaijuIcons
import com.earlthekaiju.coach.designsystem.theme.Kaiju
import com.earlthekaiju.coach.designsystem.theme.KaijuShapes
import com.earlthekaiju.coach.designsystem.theme.KaijuSpace
import com.earlthekaiju.coach.domain.model.AttendanceStatus
import com.earlthekaiju.coach.domain.model.Belt
import com.earlthekaiju.coach.domain.model.ProgramColor
import com.earlthekaiju.coach.domain.model.SessionStatus

/** Raised surface with a hairline and one chamfered corner. Optional accent edge on the start side. */
@Composable
fun KaijuCard(
    modifier: Modifier = Modifier,
    shape: Shape = KaijuShapes.Card,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    accentEdge: Color? = null,
    contentPadding: androidx.compose.ui.unit.Dp = KaijuSpace.m,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = Kaiju.colors
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.line, shape)
            .then(if (onClick != null) Modifier.clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick) else Modifier)
            .height(IntrinsicSize.Min),
    ) {
        if (accentEdge != null) Box(Modifier.width(4.dp).fillMaxHeight().background(accentEdge))
        Column(Modifier.weight(1f).padding(contentPadding), content = content)
    }
}

/** Display data for a scheduled class, already formatted for the UI. */
data class ClassCardModel(
    val id: String,
    val timeLabel: String,
    val durationLabel: String,
    val title: String,
    val programName: String?,
    val programColor: ProgramColor?,
    val lessonTitle: String?,
    val status: SessionStatus,
    val expectedCount: Int,
)

@Composable
fun programColorValue(color: ProgramColor?): Color = when (color) {
    ProgramColor.EMERALD -> Kaiju.colors.action
    ProgramColor.GOLD -> Kaiju.colors.gold
    ProgramColor.IVORY -> Kaiju.colors.ink
    ProgramColor.ACCENT -> Kaiju.colors.accent
    null -> Kaiju.colors.lineStrong
}

/**
 * One scheduled class in a list: time column (tabular numerals), program accent edge,
 * title, lesson, expected students and a status badge.
 */
@Composable
fun ClassCard(model: ClassCardModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Kaiju.colors
    val canceled = model.status == SessionStatus.CANCELED
    KaijuCard(
        modifier = modifier,
        onClick = onClick,
        onClickLabel = "Open class",
        accentEdge = programColorValue(model.programColor),
        contentPadding = KaijuSpace.s,
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.widthIn(min = 64.dp).padding(start = KaijuSpace.xxs, end = KaijuSpace.s)) {
                Text(model.timeLabel, style = Kaiju.type.numeralM, color = if (canceled) colors.inkMuted else colors.ink)
                Text(model.durationLabel.uppercase(), style = Kaiju.type.eyebrow, color = colors.inkMuted)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    model.title,
                    style = Kaiju.type.titleSmall.copy(textDecoration = if (canceled) TextDecoration.LineThrough else null),
                    color = if (canceled) colors.inkMuted else colors.ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val sub = listOfNotNull(model.programName, model.lessonTitle?.let { "Lesson: $it" }).joinToString(" · ")
                if (sub.isNotEmpty()) {
                    Text(sub, style = Kaiju.type.bodyS, color = colors.inkMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(KaijuSpace.xs))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(KaijuSpace.xs),
                    verticalArrangement = Arrangement.spacedBy(KaijuSpace.xxs),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    SessionStatusBadge(model.status)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(KaijuIcons.Students, null, tint = colors.inkMuted, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (model.expectedCount == 1) "1 expected" else "${model.expectedCount} expected",
                            style = Kaiju.type.caption,
                            color = colors.inkMuted,
                        )
                    }
                }
            }
        }
    }
}

enum class SegmentState { Upcoming, Current, Done }

/**
 * Numbered lesson segment: big gold index, title, duration, techniques and notes.
 * Used in the lesson builder (with [trailing] edit controls), the Today preview and Class Mode.
 */
@Composable
fun LessonSegmentRow(
    index: Int,
    title: String,
    minutes: Int,
    modifier: Modifier = Modifier,
    kindLabel: String? = null,
    techniques: List<String> = emptyList(),
    notes: String? = null,
    state: SegmentState = SegmentState.Upcoming,
    compact: Boolean = false,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val colors = Kaiju.colors
    val indexColor = when (state) {
        SegmentState.Current -> colors.live
        SegmentState.Done -> colors.inkMuted
        SegmentState.Upcoming -> colors.gold
    }
    Row(
        modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                stateDescription = when (state) {
                    SegmentState.Current -> "Current segment"
                    SegmentState.Done -> "Done"
                    SegmentState.Upcoming -> ""
                }
            }
            .padding(vertical = if (compact) KaijuSpace.xs else KaijuSpace.s),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            indexLabel(index),
            style = if (compact) Kaiju.type.numeralM.copy(fontSize = Kaiju.type.title.fontSize) else Kaiju.type.numeralM,
            color = indexColor,
            modifier = Modifier.widthIn(min = if (compact) 32.dp else 44.dp),
        )
        Column(Modifier.weight(1f).padding(top = if (compact) 1.dp else 3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    style = Kaiju.type.titleSmall.copy(textDecoration = if (state == SegmentState.Done) TextDecoration.LineThrough else null),
                    color = if (state == SegmentState.Done) colors.inkMuted else colors.ink,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(KaijuSpace.xs))
                Text("$minutes MIN", style = Kaiju.type.eyebrow, color = colors.inkMuted, maxLines = 1)
            }
            if (!compact) {
                if (kindLabel != null) Text(kindLabel, style = Kaiju.type.caption, color = colors.accent)
                if (techniques.isNotEmpty()) {
                    Spacer(Modifier.height(KaijuSpace.xxs))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(KaijuSpace.xs), verticalArrangement = Arrangement.spacedBy(KaijuSpace.xxs)) {
                        techniques.forEach { TechniqueTag(it) }
                    }
                }
                if (!notes.isNullOrBlank()) {
                    Spacer(Modifier.height(KaijuSpace.xxs))
                    Text(notes, style = Kaiju.type.bodyS, color = colors.inkMuted, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (trailing != null) Row(verticalAlignment = Alignment.CenterVertically, content = trailing)
    }
}

@Composable
fun TechniqueTag(name: String, modifier: Modifier = Modifier) {
    val colors = Kaiju.colors
    Row(
        modifier
            .background(colors.surfaceInset, KaijuShapes.Small)
            .padding(horizontal = KaijuSpace.xs, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LiveDot(color = colors.gold, size = 6.dp, pulse = false)
        Spacer(Modifier.width(5.dp))
        Text(name, style = Kaiju.type.caption, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun TechniqueCard(
    name: String,
    category: String,
    notes: String,
    usedInCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Kaiju.colors
    KaijuCard(modifier = modifier, onClick = onClick, onClickLabel = "Edit technique") {
        if (category.isNotBlank()) Eyebrow(category)
        Text(name, style = Kaiju.type.title, color = colors.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (notes.isNotBlank()) {
            Spacer(Modifier.height(KaijuSpace.xxs))
            Text(notes, style = Kaiju.type.bodyS, color = colors.inkMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(KaijuSpace.xs))
        Text(
            when (usedInCount) { 0 -> "Not in a lesson yet"; 1 -> "In 1 lesson segment"; else -> "In $usedInCount lesson segments" },
            style = Kaiju.type.caption,
            color = colors.inkMuted,
        )
    }
}

/**
 * Roster row: name, belt graphic + words, programs, recent attendance. No contact details or
 * other sensitive fields appear in the list.
 */
@Composable
fun StudentRow(
    name: String,
    belt: Belt,
    stripes: Int,
    detail: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    archived: Boolean = false,
    isMinor: Boolean = false,
    horizontalPadding: androidx.compose.ui.unit.Dp = KaijuSpace.gutter,
) {
    val colors = Kaiju.colors
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(onClickLabel = "Open student", role = Role.Button, onClick = onClick)
            .padding(horizontal = horizontalPadding, vertical = KaijuSpace.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    name,
                    style = Kaiju.type.titleSmall,
                    color = if (archived) colors.inkMuted else colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (isMinor) {
                    Spacer(Modifier.width(KaijuSpace.xs))
                    Text("KIDS", style = Kaiju.type.eyebrow, color = colors.gold)
                }
                if (archived) {
                    Spacer(Modifier.width(KaijuSpace.xs))
                    Text("ARCHIVED", style = Kaiju.type.eyebrow, color = colors.inkMuted)
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                BeltBar(belt, stripes, width = 44.dp, height = 11.dp)
                Spacer(Modifier.width(KaijuSpace.xs))
                Text(
                    belt.describe(stripes).removeSuffix(" belt").replace(" belt,", " ·"),
                    style = Kaiju.type.caption,
                    color = colors.inkMuted,
                    maxLines = 1,
                )
            }
            if (detail.isNotBlank()) {
                Text(detail, style = Kaiju.type.caption, color = colors.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Icon(KaijuIcons.ChevronRight, null, tint = colors.inkMuted, modifier = Modifier.size(20.dp))
    }
}

/**
 * Attendance control for one student: two large toggles (Present / Absent), each with an icon
 * and a word. Unmarked shows both as outlines. Rows are 72dp+ for quick taps mid-class.
 */
@Composable
fun AttendanceRow(
    name: String,
    belt: Belt,
    stripes: Int,
    status: AttendanceStatus?,
    onMark: (AttendanceStatus) -> Unit,
    modifier: Modifier = Modifier,
    note: String? = null,
) {
    val colors = Kaiju.colors
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .padding(horizontal = KaijuSpace.gutter, vertical = KaijuSpace.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = KaijuSpace.s)) {
            Text(name, style = Kaiju.type.titleSmall, color = colors.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                BeltBar(belt, stripes, width = 40.dp, height = 10.dp)
                if (note != null) {
                    Spacer(Modifier.width(KaijuSpace.xs))
                    Text(note, style = Kaiju.type.caption, color = colors.inkMuted, maxLines = 1)
                }
            }
        }
        Row(
            Modifier.selectableGroup().semantics { contentDescription = "Attendance for $name" },
            horizontalArrangement = Arrangement.spacedBy(KaijuSpace.xs),
        ) {
            AttendanceToggle(
                label = "Present",
                icon = KaijuIcons.Check,
                selected = status == AttendanceStatus.PRESENT,
                selectedFill = colors.action,
                selectedContent = colors.onAction,
                onClick = { onMark(AttendanceStatus.PRESENT) },
            )
            AttendanceToggle(
                label = "Absent",
                icon = KaijuIcons.Close,
                selected = status == AttendanceStatus.ABSENT,
                selectedFill = colors.surfaceInset,
                selectedContent = colors.signal,
                selectedBorder = colors.signal,
                onClick = { onMark(AttendanceStatus.ABSENT) },
            )
        }
    }
}

@Composable
private fun AttendanceToggle(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    selectedFill: Color,
    selectedContent: Color,
    onClick: () -> Unit,
    selectedBorder: Color? = null,
) {
    val colors = Kaiju.colors
    val shape = KaijuShapes.Action
    Row(
        Modifier
            .heightIn(min = 52.dp)
            .widthIn(min = 92.dp)
            .clip(shape)
            .background(if (selected) selectedFill else colors.background)
            .border(1.dp, if (selected) (selectedBorder ?: selectedFill) else colors.lineStrong, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = KaijuSpace.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = if (selected) selectedContent else colors.inkMuted, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = Kaiju.type.label, color = if (selected) selectedContent else colors.inkMuted)
    }
}

/** Generic navigation row for settings and lists. */
@Composable
fun KaijuListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    showChevron: Boolean = onClick != null,
) {
    val colors = Kaiju.colors
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(vertical = KaijuSpace.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = colors.inkMuted, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(KaijuSpace.m))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = Kaiju.type.titleSmall, color = colors.ink)
            if (subtitle != null) Text(subtitle, style = Kaiju.type.bodyS, color = colors.inkMuted)
        }
        if (value != null) {
            Spacer(Modifier.width(KaijuSpace.s))
            Text(value, style = Kaiju.type.label, color = colors.inkMuted, maxLines = 1)
        }
        if (showChevron) {
            Spacer(Modifier.width(KaijuSpace.xs))
            Icon(KaijuIcons.ChevronRight, null, tint = colors.inkMuted, modifier = Modifier.size(20.dp))
        }
    }
}
