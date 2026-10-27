package com.ghdinteractivestudio.synapse.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghdinteractivestudio.synapse.ui.components.CharacterAvatar
import com.ghdinteractivestudio.synapse.ui.components.PrimaryButton
import com.ghdinteractivestudio.synapse.ui.components.Symbol
import com.ghdinteractivestudio.synapse.ui.components.plainButton
import com.ghdinteractivestudio.synapse.ui.theme.Motion
import com.ghdinteractivestudio.synapse.ui.theme.Palette
import com.ghdinteractivestudio.synapse.ui.theme.RoundedRectangle
import com.ghdinteractivestudio.synapse.ui.theme.Type
import com.ghdinteractivestudio.synapse.ui.theme.medium
import kotlinx.coroutines.delay
import kotlin.math.sin

@Composable
fun OnboardingScreen(
    onConfigureServer: () -> Unit = {},
    onExploreDemo: () -> Unit = {},
    onContinue: () -> Unit = onConfigureServer,
    modifier: Modifier = Modifier
) {
    var beat by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        for (next in 1..4) {
            delay(if (next == 1) 150 else 280)
            beat = next
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.background)
            .safeDrawingPadding()
    ) {
        val totalHeight = maxHeight
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = totalHeight)
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(Modifier.height(0.dp))

                // Bobbing 5-member character crew
                Crew(shown = beat >= 1, modifier = Modifier.padding(top = 16.dp))

                Column(Modifier.padding(top = 16.dp)) {
                    // Chat exchange preview
                    ChatGlimpse(shown = beat >= 2, modifier = Modifier.padding(bottom = 20.dp))

                    // Title & Description
                    Column(
                        modifier = Modifier.rise(beat >= 3),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Vos agents de\nprogrammation,\ncomme coéquipiers.",
                            style = Type.system(32f, FontWeight.SemiBold).copy(letterSpacing = (-0.6).sp),
                            color = Palette.text
                        )
                        Text(
                            text = "Donnez à chacun un nom et un projet. Ils travaillent sur votre ordinateur pendant votre absence.",
                            style = Type.body,
                            color = Palette.secondary
                        )
                        DemoButton(
                            onDemoClick = onExploreDemo,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Action button at bottom
                Column(
                    modifier = Modifier
                        .padding(top = 28.dp)
                        .rise(beat >= 4),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PrimaryButton(
                        title = "Commencer",
                        modifier = Modifier.fillMaxWidth(),
                        action = onContinue
                    )
                }
            }
        }
    }
}

private class CrewMember(
    val shape: String,
    val color: String,
    val size: Int,
    val x: Float,
    val y: Float,
    val mood: String
)

private val CREW = listOf(
    CrewMember("blob", "blue", 92, 0f, 0f, "working"),
    CrewMember("squircle", "orange", 64, -104f, -34f, "idle"),
    CrewMember("teardrop", "violet", 58, 100f, -46f, "working"),
    CrewMember("hex", "green", 48, -76f, 64f, "idle"),
    CrewMember("cloud", "magenta", 52, 84f, 58f, "needsInput")
)

@Composable
private fun Crew(shown: Boolean, modifier: Modifier = Modifier) {
    var time by remember { mutableFloatStateOf(0f) }
    if (shown) {
        LaunchedEffect(Unit) {
            while (true) withFrameNanos { time = it / 1e9f }
        }
    }
    val density = LocalDensity.current

    Box(modifier.fillMaxWidth().height(170.dp), contentAlignment = Alignment.Center) {
        for ((index, member) in CREW.withIndex()) {
            val entry = remember { Animatable(0f) }
            LaunchedEffect(shown) {
                if (shown) {
                    delay(index * 70L)
                    entry.animateTo(1f, Motion.spring(0.7f, 0.4f))
                } else {
                    entry.snapTo(0f)
                }
            }
            Box(Modifier.graphicsLayer {
                val bob = sin(time * 1.3f + index * 1.7f) * 5f
                translationX = with(density) { member.x.dp.toPx() }
                translationY = with(density) { (member.y + bob).dp.toPx() }
                val scale = 0.3f + 0.7f * entry.value
                scaleX = scale
                scaleY = scale
                alpha = entry.value.coerceIn(0f, 1f)
            }) {
                CharacterAvatar(member.shape, member.color, member.size.dp, member.mood)
            }
        }
    }
}

@Composable
private fun ChatGlimpse(shown: Boolean, modifier: Modifier = Modifier) {
    var replied by remember { mutableStateOf(false) }
    LaunchedEffect(shown) {
        if (shown) {
            delay(1200)
            replied = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.fillMaxWidth().rise(shown), contentAlignment = Alignment.CenterEnd) {
            Bubble("Fix the flaky login test", Palette.bubbleUser)
        }
        Row(Modifier.rise(shown), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CharacterAvatar("blob", "blue", 28.dp, if (replied) "idle" else "working")
            AnimatedContent(
                targetState = replied,
                transitionSpec = {
                    (fadeIn(Motion.spring(0.45f, 0.2f)) + scaleIn(Motion.spring(0.45f, 0.2f), 0.92f, TransformOrigin(0f, 1f))) togetherWith
                        fadeOut(Motion.fade())
                },
                label = "Reply"
            ) { done ->
                if (done) {
                    Bubble("Done. It raced the session refresh; tests pass on fix/login.", Palette.bubbleAgent)
                } else {
                    Text("Working…", style = Type.subheadline, color = Palette.tertiary)
                }
            }
        }
    }
}

@Composable
private fun Bubble(text: String, fill: Color) =
    Text(
        text = text,
        modifier = Modifier
            .clip(RoundedRectangle(18.dp))
            .background(fill)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        style = Type.subheadline,
        color = Palette.text
    )

@Composable
private fun DemoButton(
    onDemoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uri = LocalUriHandler.current
    Row(
        modifier = modifier.plainButton {
            try {
                uri.openUri("https://youtube.com/shorts/xtZ5WeyOewU")
            } catch (_: Exception) {
                onDemoClick()
            }
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Symbol("play.circle.fill", size = 17.dp, tint = Palette.text)
        Text("Watch the demo", style = Type.subheadline.medium, color = Palette.text)
    }
}

private fun Modifier.rise(shown: Boolean): Modifier =
    graphicsLayer {
        alpha = if (shown) 1f else 0f
        translationY = if (shown) 0f else 18f
    }
