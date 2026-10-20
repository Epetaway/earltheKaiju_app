package com.earlthekaiju.coach.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.earlthekaiju.coach.designsystem.icon.KaijuIcons
import com.earlthekaiju.coach.designsystem.theme.Kaiju
import com.earlthekaiju.coach.designsystem.theme.KaijuSpace

/**
 * Top bar for secondary screens (editors, details): back, an optional gold eyebrow over the
 * title, trailing actions, and a hairline. Top-level screens use [ScreenHeader] instead.
 */
@Composable
fun KaijuTopBar(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    onBack: (() -> Unit)? = null,
    backDescription: String = "Back",
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(Kaiju.colors.background)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(start = if (onBack != null) KaijuSpace.xxs else KaijuSpace.gutter, end = KaijuSpace.xxs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                KaijuIconButton(icon = KaijuIcons.Back, contentDescription = backDescription, onClick = onBack)
                Spacer(Modifier.width(KaijuSpace.xxs))
            }
            Column(Modifier.weight(1f).padding(vertical = KaijuSpace.xs)) {
                if (eyebrow != null) Eyebrow(eyebrow)
                Text(
                    text = title,
                    style = Kaiju.type.title,
                    color = Kaiju.colors.ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
            }
            actions()
        }
        HairlineDivider(Modifier.fillMaxWidth())
    }
}

data class BottomNavItem(val label: String, val icon: ImageVector)

/**
 * Primary navigation: five labeled destinations, always visible labels. The selected item gets an
 * accent "index tab" along its top edge plus Ivory icon and label, so selection never relies on
 * color alone. Each item is at least 64dp tall.
 */
@Composable
fun KaijuBottomBar(
    items: List<BottomNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Kaiju.colors
    Column(modifier.fillMaxWidth().background(colors.background)) {
        HairlineDivider(Modifier.fillMaxWidth())
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 64.dp)
                        .selectable(
                            selected = selected,
                            onClick = { onSelect(index) },
                            role = Role.Tab,
                            interactionSource = null,
                            indication = ripple(bounded = true, color = colors.accent),
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .width(28.dp)
                            .height(3.dp)
                            .background(if (selected) colors.accent else colors.background),
                    )
                    Spacer(Modifier.height(KaijuSpace.xs))
                    Icon(
                        item.icon,
                        contentDescription = null,
                        tint = if (selected) colors.ink else colors.inkMuted,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.height(KaijuSpace.xxs))
                    Text(
                        item.label,
                        style = Kaiju.type.navLabel,
                        color = if (selected) colors.ink else colors.inkMuted,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 2.dp, vertical = 0.dp),
                    )
                    Spacer(Modifier.height(KaijuSpace.xs))
                }
            }
        }
    }
}

/**
 * Side rail used instead of the bottom bar when the window is 600dp or wider (tablets,
 * phones in landscape): same items, same selection treatment, accent tab on the leading edge.
 */
@Composable
fun KaijuNavRail(
    items: List<BottomNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit = {},
) {
    val colors = Kaiju.colors
    Row(modifier.background(colors.background)) {
        Column(
            modifier = Modifier
                .width(88.dp)
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(vertical = KaijuSpace.s)
                .selectableGroup(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(KaijuSpace.xxs),
        ) {
            header()
            Spacer(Modifier.height(KaijuSpace.s))
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .selectable(
                            selected = selected,
                            onClick = { onSelect(index) },
                            role = Role.Tab,
                            interactionSource = null,
                            indication = ripple(bounded = true, color = colors.accent),
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.width(3.dp).height(28.dp).background(if (selected) colors.accent else colors.background))
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(item.icon, contentDescription = null, tint = if (selected) colors.ink else colors.inkMuted, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.height(KaijuSpace.xxs))
                        Text(item.label, style = Kaiju.type.navLabel, color = if (selected) colors.ink else colors.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        Box(Modifier.width(1.dp).fillMaxHeight().background(colors.line))
    }
}
