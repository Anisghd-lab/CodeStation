package com.ghdinteractivestudio.codestation.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ghdinteractivestudio.codestation.ui.theme.Capsule
import com.ghdinteractivestudio.codestation.ui.theme.Motion
import com.ghdinteractivestudio.codestation.ui.theme.Palette
import com.ghdinteractivestudio.codestation.ui.theme.Type
import com.ghdinteractivestudio.codestation.ui.theme.semibold

@Composable
fun ModalHeader(
    title: String,
    onClose: () -> Unit = {},
    trailing: @Composable RowScope.() -> Unit = {}
) = ModalHeader(
    title = {
        Text(
            tr(title),
            style = Type.body.semibold,
            color = Palette.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    },
    onClose = onClose,
    trailing = trailing
)

@Composable
fun ModalHeader(
    title: @Composable RowScope.() -> Unit,
    onClose: () -> Unit = {},
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(start = 20.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) { heading() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            title()
        }
        trailing()
        IconButton("Close", "xmark", action = onClose)
    }
}

@Composable
fun ScreenHeader(
    leading: @Composable RowScope.() -> Unit = {},
    title: @Composable () -> Unit = {},
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(Palette.background)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.semantics(mergeDescendants = true) { heading() }) {
            title()
        }
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            leading()
            Spacer(Modifier.weight(1f))
            trailing()
        }
    }
}

@Composable
fun BackButton(action: () -> Unit) = IconButton("Back", "chevron.left", action = action)

@Composable
fun PageHeader(
    title: String,
    pop: (() -> Unit)? = null,
    icon: String? = null,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    val heading: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (icon != null) Symbol(icon, size = 24.dp, tint = Palette.text)
            Text(
                tr(title),
                style = Type.body.semibold,
                color = Palette.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    if (pop != null) ScreenHeader({ BackButton(pop) }, heading, trailing)
    else ModalHeader({ heading() }, trailing = trailing)
}

class TabItem<T>(
    val value: T,
    val title: String,
    val systemImage: String,
    val iconSize: Dp = 28.dp
)

private val TAB_WIDTH = 90.dp

@Composable
fun <T> GlassTabBar(
    selection: T,
    tabs: List<TabItem<T>>,
    select: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val index = tabs.indexOfFirst { it.value == selection }.coerceAtLeast(0)
    val pill by animateDpAsState(TAB_WIDTH * index, Motion.layout(), label = "Tab selection")

    Box(
        modifier
            .glass(Capsule(), style = GlassStyle.Regular)
            .padding(4.dp)
    ) {
        Box(
            Modifier
                .offset(x = pill)
                .size(TAB_WIDTH, 54.dp)
                .clip(Capsule())
                .background(if (com.ghdinteractivestudio.codestation.ui.theme.LocalDark.current) Palette.emeraldAccent.copy(alpha = 0.16f) else Palette.text.copy(alpha = 0.08f))
                .border(0.5.dp, if (com.ghdinteractivestudio.codestation.ui.theme.LocalDark.current) Palette.emeraldAccent.copy(alpha = 0.35f) else Palette.glassBorder, Capsule())
        )
        Row {
            for (tab in tabs) {
                val on = tab.value == selection
                androidx.compose.foundation.layout.Column(
                    Modifier
                        .size(TAB_WIDTH, 54.dp)
                        .clip(Capsule())
                        .selectable(on, role = Role.Tab) { select(tab.value) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Symbol(
                        tab.systemImage,
                        size = tab.iconSize,
                        tint = if (on) (if (com.ghdinteractivestudio.codestation.ui.theme.LocalDark.current) Palette.emeraldAccent else Palette.text) else Palette.secondary
                    )
                    Text(
                        tr(tab.title),
                        style = Type.system(10f),
                        color = if (on) (if (com.ghdinteractivestudio.codestation.ui.theme.LocalDark.current) Palette.emeraldAccent else Palette.text) else Palette.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
