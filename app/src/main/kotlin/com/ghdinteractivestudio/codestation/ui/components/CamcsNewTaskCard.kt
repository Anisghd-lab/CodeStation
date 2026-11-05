package com.ghdinteractivestudio.codestation.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghdinteractivestudio.codestation.domain.model.Bot
import com.ghdinteractivestudio.codestation.ui.theme.Capsule
import com.ghdinteractivestudio.codestation.ui.theme.LocalDark
import com.ghdinteractivestudio.codestation.ui.theme.Palette
import com.ghdinteractivestudio.codestation.ui.theme.Type
import com.ghdinteractivestudio.codestation.ui.theme.bold
import com.ghdinteractivestudio.codestation.ui.theme.medium
import com.ghdinteractivestudio.codestation.ui.theme.semibold

/**
 * Premium Glassmorphism Card for "Nouvelle tâche" with CAMCS orchestration.
 * Faithfully matches the visual reference:
 * 1. Generous rounded corners (28.dp) with frosted liquid glassmorphism.
 * 2. Top-left abstract dot illustration (CodeStation design language) with 4-pointed sparkle.
 * 3. Title "Nouvelle tâche" + subtitle "Décris ton objectif et laisse CAMCS orchestrer les bons agents." + circular chevron button.
 * 4. 4 capability capsules: Fichiers & projet, Recherche web, Commande CLI, Automatisation & outils.
 * 5. Wide action button "+ Nouvelle tâche ›".
 * 6. CAMCS identity footer: 4 mini-avatars + "Multi-agents · Hybride · Autonome" + Info (i) button.
 */
@Composable
fun CamcsNewTaskCard(
    onNewTaskClick: () -> Unit,
    onOpenControlCenter: (() -> Unit)? = null,
    bots: List<Bot> = emptyList(),
    modifier: Modifier = Modifier
) {
    val dark = LocalDark.current
    val (source, pressed) = rememberPress()
    val cardShape = RoundedCornerShape(28.dp)
    var showCamcsInfoDialog by remember { mutableStateOf(false) }

    // Glassmorphism background gradient
    val bgBrush = if (dark) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xDD121622),
                Color(0xDD181E2E),
                Color(0xDD1E1A2C)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xF5F0F6FF),
                Color(0xF5FFFFFF),
                Color(0xF5F6F3FD)
            )
        )
    }

    val rimBrush = if (dark) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.28f),
                Color.White.copy(alpha = 0.10f),
                Color.White.copy(alpha = 0.04f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.95f),
                Color(0x15000000),
                Color(0x0A000000)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(pressed)
            .shadow(
                elevation = if (dark) 8.dp else 12.dp,
                shape = cardShape,
                ambientColor = if (dark) Color.Black.copy(alpha = 0.5f) else Color(0x1A2563EB),
                spotColor = if (dark) Color.Black.copy(alpha = 0.6f) else Color(0x1F3B82F6)
            )
            .background(bgBrush, cardShape)
            .border(1.dp, rimBrush, cardShape)
            .clip(cardShape)
            .clickable(source, null, role = Role.Button, onClick = onNewTaskClick)
            .padding(horizontal = 16.dp, vertical = 18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. TOP HEADER SECTION
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Left: Abstract Dot Illustration (CodeStation Living Design)
                CamcsConstellationIllustration(
                    size = 78.dp
                )

                // Middle: Title and Description
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Nouvelle tâche",
                        style = Type.title2.copy(
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.4).sp
                        ),
                        color = Palette.text,
                        maxLines = 1
                    )

                    Text(
                        text = "Décris ton objectif et laisse CAMCS orchestrer les bons agents.",
                        style = Type.footnote.copy(
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        ),
                        color = Palette.secondary,
                        maxLines = 2,
                        softWrap = true
                    )
                }

                // Right: Circular Chevron Navigation Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (dark) Color(0x33FFFFFF) else Color.White.copy(alpha = 0.90f)
                        )
                        .border(
                            0.5.dp,
                            if (dark) Color.White.copy(alpha = 0.20f) else Color.Black.copy(alpha = 0.06f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Symbol("chevron.right", size = 13.dp, tint = Palette.text)
                }
            }

            // 2. CAPABILITIES CAPSULES (2 Rows x 2 Pills for zero word-cutting on mobile)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CamcsCapabilityCapsule(
                        iconType = CapsuleIconType.Folder,
                        label = "Fichiers & projet",
                        modifier = Modifier.weight(1f),
                        onClick = onNewTaskClick
                    )

                    CamcsCapabilityCapsule(
                        iconType = CapsuleIconType.Globe,
                        label = "Recherche web",
                        modifier = Modifier.weight(1f),
                        onClick = onNewTaskClick
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CamcsCapabilityCapsule(
                        iconType = CapsuleIconType.Terminal,
                        label = "Commande CLI",
                        modifier = Modifier.weight(1f),
                        onClick = onNewTaskClick
                    )

                    CamcsCapabilityCapsule(
                        iconType = CapsuleIconType.Automation,
                        label = "Automatisation & outils",
                        modifier = Modifier.weight(1f),
                        onClick = onNewTaskClick
                    )
                }
            }

            // 3. MAIN ACTION BUTTON (+ Nouvelle tâche ›) - PREMIUM GLASSMORPHISM
            val (buttonSource, buttonPressed) = rememberPress()

            // Translucent frosted glass fill with chromatic depth and inner specular reflection
            val buttonBgBrush = if (dark) {
                Brush.verticalGradient(
                    listOf(
                        Color(0x3567E8F9), // Luminous top specular cyan sheen
                        Color(0x281E293B), // Translucent slate graphite depth
                        Color(0x550F172A), // Frosted obsidian base
                        Color(0x750A0F1D)  // Deep frosted anchoring base
                    )
                )
            } else {
                Brush.verticalGradient(
                    listOf(
                        Color(0xE5FFFFFF), // Pure white liquid specular highlight
                        Color(0x90F0F9FF), // Iridescent crystal sky wash
                        Color(0x75E0F2FE), // Translucent frosted sapphire
                        Color(0x95F8FAFC)  // Platinum crystal backing
                    )
                )
            }

            // Fine, discrete specular glass border with top luster
            val buttonRimBrush = if (dark) {
                Brush.verticalGradient(
                    listOf(
                        Color(0x80FFFFFF), // Specular crystal rim top
                        Color(0x5038BDF8), // Electric cyan-blue refraction
                        Color(0x2094A3B8), // Discrete slate outline
                        Color(0x10FFFFFF)  // Subtle ambient bottom edge
                    )
                )
            } else {
                Brush.verticalGradient(
                    listOf(
                        Color(0xFAFFFFFF), // Crisp diamond highlight top
                        Color(0x6038BDF8), // Subtle azure crystal refraction
                        Color(0x250F172A), // Discrete defining outline
                        Color(0x150F172A)  // Soft ambient bottom edge
                    )
                )
            }

            val buttonTextColor = if (dark) Color(0xFFFFFFFF) else Color(0xFF0F172A)
            val buttonIconTint = if (dark) Color(0xFF38BDF8) else Palette.electricBlue
            val buttonChevronTint = if (dark) Color(0xB3FFFFFF) else Color(0x990F172A)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .pressScale(buttonPressed)
                    .shadow(
                        elevation = if (dark) 8.dp else 6.dp,
                        shape = Capsule(),
                        ambientColor = if (dark) Color.Black.copy(alpha = 0.55f) else Color(0x1A0F172A),
                        spotColor = if (dark) Color(0x4038BDF8) else Color(0x250284C7)
                    )
                    .background(buttonBgBrush, Capsule())
                    .border(1.dp, buttonRimBrush, Capsule())
                    .clip(Capsule())
                    .clickable(buttonSource, null, role = Role.Button, onClick = onNewTaskClick)
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                // Diagonal liquid specular meniscus sheen overlay
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(Capsule())
                ) {
                    val meniscusBrush = if (dark) {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.22f),
                                Color.White.copy(alpha = 0.05f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = size.height * 0.55f
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.85f),
                                Color.White.copy(alpha = 0.25f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = size.height * 0.50f
                        )
                    }
                    drawRect(brush = meniscusBrush)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Left: Luminous frosted circular badge behind plus icon
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (dark) Color(0x2538BDF8) else Color(0x200284C7)
                            )
                            .border(
                                0.8.dp,
                                if (dark) Color(0x5038BDF8) else Color(0x350284C7),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Symbol("plus", size = 16.dp, tint = buttonIconTint)
                    }

                    Text(
                        text = "Nouvelle tâche",
                        style = Type.body.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.2).sp
                        ),
                        color = buttonTextColor,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    // Right: Chevron right inside subtle circular pill
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                if (dark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Symbol("chevron.right", size = 13.dp, tint = buttonChevronTint)
                    }
                }
            }

            // 4. SUBTLE SEPARATOR
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(
                        if (dark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
                    )
            )

            // 5. CAMCS IDENTITY FOOTER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Dynamic Mini agent avatars with exact CodeStation colors and styles
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val displayBots = if (bots.isNotEmpty()) {
                        bots.take(5)
                    } else {
                        listOf(
                            Bot(id = "1", name = "Antigravity", avatarGlyph = "diamond", avatarColorHex = "#4285F4"),
                            Bot(id = "2", name = "OpenCode", avatarGlyph = "hexagon", avatarColorHex = "#A855F7"),
                            Bot(id = "3", name = "CodeBuddy", avatarGlyph = "circle", avatarColorHex = "#3B82F6"),
                            Bot(id = "4", name = "Codex", avatarGlyph = "circle", avatarColorHex = "#10B981")
                        )
                    }
                    displayBots.forEach { b ->
                        CharacterAvatar(shape = b.avatarGlyph, color = b.avatarColorHex, size = 20.dp)
                    }
                }

                // Text: Multi-agents · Hybride · Autonome
                Text(
                    text = "Multi-agents · Hybride · Autonome",
                    style = Type.caption.copy(
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Palette.secondary,
                    maxLines = 1,
                    softWrap = true,
                    modifier = Modifier.weight(1f)
                )

                // Info Icon Button (i)
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Button) { showCamcsInfoDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Symbol("info.circle", size = 18.dp, tint = Palette.secondary)
                }
            }
        }
    }

    // CAMCS Info Presentation Dialog
    if (showCamcsInfoDialog) {
        GlassAlertDialog(
            onDismissRequest = { showCamcsInfoDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Symbol("sparkles", size = 20.dp, tint = Palette.electricBlue)
                    Text("Système CAMCS", style = Type.title3.semibold, color = Palette.text)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "CodeStation Autonomous Multi-agent Coordination System (CAMCS) orchestre dynamiquement Google Antigravity, OpenCode, CodeBuddy Code et Codex.",
                        style = Type.subheadline,
                        color = Palette.text
                    )
                    Text(
                        text = "• Hybride & Autonome : sélectionne automatiquement les meilleurs modèles selon l'objectif.\n• Sécurité & Contrôle : validation humaine avant modifications critiques.\n• Outils intégrés : accès aux fichiers du projet, CLI Termux, recherche web et exécution.",
                        style = Type.footnote,
                        color = Palette.secondary
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (onOpenControlCenter != null) {
                        Box(
                            Modifier
                                .clip(Capsule())
                                .background(Palette.surface)
                                .border(0.5.dp, Palette.glassBorder, Capsule())
                                .clickable {
                                    showCamcsInfoDialog = false
                                    onOpenControlCenter()
                                }
                                .padding(horizontal = 14.dp, vertical = 9.dp)
                        ) {
                            Text("Centre de Contrôle", style = Type.caption.semibold, color = Palette.text)
                        }
                    }
                    Box(
                        Modifier
                            .clip(Capsule())
                            .background(Palette.accentFill)
                            .clickable { showCamcsInfoDialog = false }
                            .padding(horizontal = 18.dp, vertical = 9.dp)
                    ) {
                        Text("Fermer", style = Type.caption.bold, color = Palette.onAccent)
                    }
                }
            }
        )
    }
}

/**
 * Capsule Icon Type for the 4 capability pills.
 */
enum class CapsuleIconType {
    Folder,
    Globe,
    Terminal,
    Automation
}

/**
 * Individual Capability Pill Capsule.
 */
@Composable
private fun CamcsCapabilityCapsule(
    iconType: CapsuleIconType,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val dark = LocalDark.current
    val capsuleShape = RoundedCornerShape(14.dp)

    val bgBrush = if (dark) {
        Color(0x331E2433)
    } else {
        Color.White.copy(alpha = 0.90f)
    }

    val borderStroke = if (dark) {
        Color.White.copy(alpha = 0.12f)
    } else {
        Color.Black.copy(alpha = 0.06f)
    }

    Row(
        modifier = modifier
            .clip(capsuleShape)
            .background(bgBrush)
            .border(0.5.dp, borderStroke, capsuleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Icon rendering
        when (iconType) {
            CapsuleIconType.Folder -> {
                // Blue Folder
                FolderIcon(size = 20.dp)
            }
            CapsuleIconType.Globe -> {
                // Purple Globe
                GlobeIcon(size = 20.dp)
            }
            CapsuleIconType.Terminal -> {
                // Green/Turquoise Terminal
                TerminalIcon(size = 20.dp)
            }
            CapsuleIconType.Automation -> {
                // Orange Gear
                GearIcon(size = 20.dp)
            }
        }

        Text(
            text = label,
            style = Type.caption.copy(
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 15.sp,
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.None
                )
            ),
            color = Palette.text,
            maxLines = 2,
            softWrap = true
        )
    }
}

/**
 * Native vector icon matching Capsule 1 (Blue Folder).
 */
@Composable
private fun FolderIcon(size: Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val folderColor = Color(0xFF3B82F6) // Bright royal blue

        val path = Path().apply {
            moveTo(w * 0.10f, h * 0.30f)
            lineTo(w * 0.40f, h * 0.30f)
            lineTo(w * 0.50f, h * 0.42f)
            lineTo(w * 0.90f, h * 0.42f)
            quadraticTo(w * 0.95f, h * 0.42f, w * 0.95f, h * 0.48f)
            lineTo(w * 0.95f, h * 0.82f)
            quadraticTo(w * 0.95f, h * 0.88f, w * 0.90f, h * 0.88f)
            lineTo(w * 0.10f, h * 0.88f)
            quadraticTo(w * 0.05f, h * 0.88f, w * 0.05f, h * 0.82f)
            lineTo(w * 0.05f, h * 0.36f)
            quadraticTo(w * 0.05f, h * 0.30f, w * 0.10f, h * 0.30f)
            close()
        }
        drawPath(path, folderColor)

        // Front flap accent line
        drawLine(
            color = Color.White.copy(alpha = 0.5f),
            start = Offset(w * 0.12f, h * 0.46f),
            end = Offset(w * 0.88f, h * 0.46f),
            strokeWidth = w * 0.08f
        )
    }
}

/**
 * Native vector icon matching Capsule 2 (Purple Globe).
 */
@Composable
private fun GlobeIcon(size: Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val purpleColor = Color(0xFF8B5CF6) // Vivid violet/purple

        // Squircle background badge
        drawRoundRect(
            color = purpleColor,
            size = this.size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.30f, h * 0.30f)
        )

        val center = Offset(w * 0.5f, h * 0.5f)
        val radius = w * 0.36f

        // Globe outline
        drawCircle(
            color = Color.White,
            radius = radius,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.07f)
        )

        // Equator
        drawLine(
            color = Color.White,
            start = Offset(center.x - radius, center.y),
            end = Offset(center.x + radius, center.y),
            strokeWidth = w * 0.06f
        )

        // Central Meridian
        drawLine(
            color = Color.White,
            start = Offset(center.x, center.y - radius),
            end = Offset(center.x, center.y + radius),
            strokeWidth = w * 0.06f
        )

        // Side meridian curved lines
        val ovalPath = Path().apply {
            addOval(
                androidx.compose.ui.geometry.Rect(
                    center.x - radius * 0.5f,
                    center.y - radius,
                    center.x + radius * 0.5f,
                    center.y + radius
                )
            )
        }
        drawPath(
            path = ovalPath,
            color = Color.White,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.06f)
        )
    }
}

/**
 * Native vector icon matching Capsule 3 (Green Terminal badge `>_`).
 */
@Composable
private fun TerminalIcon(size: Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val greenColor = Color(0xFF00C49F) // Emerald / turquoise

        // Squircle background badge
        drawRoundRect(
            color = greenColor,
            size = this.size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.30f, h * 0.30f)
        )

        // `>` chevron
        val chevron = Path().apply {
            moveTo(w * 0.24f, h * 0.32f)
            lineTo(w * 0.46f, h * 0.48f)
            lineTo(w * 0.24f, h * 0.64f)
        }
        drawPath(
            path = chevron,
            color = Color.White,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = w * 0.09f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )

        // `_` underscore cursor
        drawLine(
            color = Color.White,
            start = Offset(w * 0.54f, h * 0.64f),
            end = Offset(w * 0.76f, h * 0.64f),
            strokeWidth = w * 0.09f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

/**
 * Native vector icon matching Capsule 4 (Orange Gear / Automation).
 */
@Composable
private fun GearIcon(size: Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val orangeColor = Color(0xFFFBA44A) // Warm amber orange

        val cx = w * 0.5f
        val cy = h * 0.5f
        val rOuter = w * 0.44f
        val rInner = w * 0.34f
        val teeth = 8

        val gearPath = Path()
        for (i in 0 until teeth * 2) {
            val angle = (i * Math.PI / teeth) - (Math.PI / 2)
            val r = if (i % 2 == 0) rOuter else rInner
            val x = (cx + Math.cos(angle) * r).toFloat()
            val y = (cy + Math.sin(angle) * r).toFloat()
            if (i == 0) gearPath.moveTo(x, y) else gearPath.lineTo(x, y)
        }
        gearPath.close()

        drawPath(gearPath, orangeColor)

        // Center circle hole
        drawCircle(
            color = Color.White,
            radius = w * 0.16f,
            center = Offset(cx, cy)
        )
    }
}

/**
 * Abstract CodeStation Dot Constellation Illustration.
 * - 80dp rounded container with frosted glass sheen.
 * - 37 precisely positioned dots with turquoise, blue, magenta, purple, and grey accents.
 * - 4-pointed sparkle in the top-right corner.
 * - Gentle harmonic living breathing motion.
 */
@Composable
fun CamcsConstellationIllustration(
    size: Dp = 78.dp,
    modifier: Modifier = Modifier
) {
    val dark = LocalDark.current
    val containerShape = RoundedCornerShape(20.dp)

    // Breathing shimmer animation
    val transition = rememberInfiniteTransition(label = "ConstellationBreathing")
    val breath by transition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreathScale"
    )

    // Sparkle shimmer
    val sparkleAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SparkleRotation"
    )

    // 37 Normalized dot positions & colors (extracted from reference image)
    val turquoise = Color(0xFF00D2B4)
    val blue = Color(0xFF2563EB)
    val magenta = Color(0xFFEC4899)
    val grey = Color(0xFF94A3B8)
    val purple = Color(0xFFA855F7)

    // (relX, relY, color) normalized to 190.0
    val dots = remember {
        listOf(
            Triple(105.6f / 190f, 45.0f / 190f, turquoise),
            Triple(88.0f / 190f, 61.0f / 190f, turquoise),
            Triple(103.7f / 190f, 60.9f / 190f, turquoise),
            Triple(119.0f / 190f, 61.2f / 190f, turquoise),
            Triple(70.4f / 190f, 75.7f / 190f, grey),
            Triple(85.8f / 190f, 75.8f / 190f, turquoise),
            Triple(100.9f / 190f, 75.8f / 190f, turquoise),
            Triple(116.1f / 190f, 75.9f / 190f, turquoise),
            Triple(130.9f / 190f, 76.2f / 190f, grey),
            Triple(52.4f / 190f, 90.5f / 190f, blue),
            Triple(67.8f / 190f, 90.2f / 190f, blue),
            Triple(82.8f / 190f, 90.3f / 190f, turquoise),
            Triple(114.7f / 190f, 90.3f / 190f, turquoise),
            Triple(129.2f / 190f, 90.8f / 190f, purple),
            Triple(144.0f / 190f, 90.5f / 190f, magenta),
            Triple(34.6f / 190f, 104.8f / 190f, blue),
            Triple(50.5f / 190f, 105.0f / 190f, blue),
            Triple(65.7f / 190f, 104.6f / 190f, blue),
            Triple(127.0f / 190f, 105.0f / 190f, magenta),
            Triple(141.7f / 190f, 104.6f / 190f, magenta),
            Triple(156.8f / 190f, 105.0f / 190f, magenta),
            Triple(48.2f / 190f, 119.0f / 190f, blue),
            Triple(64.2f / 190f, 119.0f / 190f, blue),
            Triple(79.7f / 190f, 119.4f / 190f, blue),
            Triple(109.5f / 190f, 119.5f / 190f, purple),
            Triple(124.7f / 190f, 119.2f / 190f, magenta),
            Triple(140.2f / 190f, 119.0f / 190f, magenta),
            Triple(46.0f / 190f, 131.6f / 190f, blue),
            Triple(62.0f / 190f, 132.0f / 190f, blue),
            Triple(78.2f / 190f, 133.0f / 190f, grey),
            Triple(92.5f / 190f, 132.5f / 190f, grey),
            Triple(107.5f / 190f, 133.0f / 190f, magenta),
            Triple(122.7f / 190f, 132.3f / 190f, magenta),
            Triple(75.9f / 190f, 146.5f / 190f, grey),
            Triple(91.0f / 190f, 146.6f / 190f, magenta),
            Triple(105.6f / 190f, 146.4f / 190f, magenta),
            Triple(88.5f / 190f, 160.5f / 190f, magenta)
        )
    }

    val dotInk = if (dark) Color.White else Palette.text

    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = if (dark) 4.dp else 6.dp,
                shape = containerShape,
                ambientColor = if (dark) Color.Black else Color(0x103B82F6),
                spotColor = if (dark) Color.Black else Color(0x153B82F6)
            )
            .background(
                if (dark) Color(0x331E2330) else Color.White.copy(alpha = 0.95f),
                containerShape
            )
            .border(
                1.dp,
                if (dark) Color.White.copy(alpha = 0.15f) else Color.White,
                containerShape
            )
            .clip(containerShape)
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height

            // Match bot avatar dot dimensions exactly: 44.dp avatar with 13 cells
            val avatarCells = 13
            val avatarStepPx = (44.dp / avatarCells.toFloat()).toPx()
            // 15.2 units in the normalized 190 coordinate system equals 1 avatar grid step
            val unitToPx = avatarStepPx / 15.2f

            val canvasCenterX = w / 2f
            val canvasCenterY = h / 2f
            val centerUnitX = 95.7f
            val centerUnitY = 102.75f

            val half = w / 2f
            val yaw = -0.7
            val lx = kotlin.math.sin(yaw) * 0.8
            val ly = 0.55
            val lz = kotlin.math.cos(yaw) * 0.5 + 0.6
            val ll = kotlin.math.sqrt(lx * lx + ly * ly + lz * lz)
            val ink = dotInk

            // Draw all 37 dots using the exact same size, diameter, spacing and 3D sphere lighting as bot avatars
            for ((rx, ry, col) in dots) {
                val unitX = rx * 190f
                val unitY = ry * 190f
                val cx = canvasCenterX + (unitX - centerUnitX) * unitToPx
                val cy = canvasCenterY + (unitY - centerUnitY) * unitToPx

                // 3D Sphere lighting math matching CharacterAvatar.kt
                val u = (cx - half) / half
                val v = (half - cy) / half
                val z = kotlin.math.sqrt(kotlin.math.max(0.2, 1.0 - u * u - v * v))
                val nl = kotlin.math.sqrt(u * u + v * v + z * z)
                val shade = 0.3 + 0.7 * kotlin.math.max(0.0, (u * lx + v * ly + z * lz) / (nl * ll))
                val r = (avatarStepPx * 0.42f * (0.55f + 0.45f * shade.toFloat()) * breath)

                // Dual-pass rendering: base ink depth + vibrant avatar tint
                val inkAlpha = (0.2 + 0.4 * kotlin.math.min(1.0, shade / 0.7)).toFloat()
                drawCircle(ink.copy(alpha = inkAlpha), r, Offset(cx, cy))
                if (shade > 0.6) {
                    val tintAlpha = ((shade - 0.6) / 0.4).toFloat().coerceIn(0f, 1f)
                    drawCircle(col.copy(alpha = tintAlpha), r, Offset(cx, cy))
                } else {
                    drawCircle(col.copy(alpha = 0.4f), r, Offset(cx, cy))
                }
            }

            // Draw 4-pointed Star Sparkle in top-right positioned harmoniously
            val spX = canvasCenterX + (158f - centerUnitX) * unitToPx
            val spY = canvasCenterY + (40f - centerUnitY) * unitToPx
            val spR = avatarStepPx * 2.2f

            val sparkleColor = Color(0xFF7C5CFC) // Lilas / lavender sparkle
            val starPath = Path().apply {
                moveTo(spX, spY - spR)
                quadraticTo(spX, spY, spX + spR, spY)
                quadraticTo(spX, spY, spX, spY + spR)
                quadraticTo(spX, spY, spX - spR, spY)
                quadraticTo(spX, spY, spX, spY - spR)
                close()
            }
            drawPath(starPath, sparkleColor)
        }
    }
}
