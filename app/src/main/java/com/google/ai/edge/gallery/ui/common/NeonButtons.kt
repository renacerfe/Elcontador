package com.google.ai.edge.gallery.ui.common

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.math.cos
import kotlin.math.sin

val NeonGreen = Color(0xFF00FF87)
val DeepBlack = Color(0xFF0A0A0A)
val DarkGray = Color(0xFF1A1A1A)

data class Particle(
    val x: Animatable<Float, AnimationVector1D>,
    val y: Animatable<Float, AnimationVector1D>,
    val alpha: Animatable<Float, AnimationVector1D>,
    val size: Float
)

@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    containerColor: Color = Color(0xFF1A1A1A).copy(alpha = 0.6f),
    contentColor: Color = Color.White,
    neonAccent: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scope = rememberCoroutineScope()

    // 1. Scale Effect
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    // 2. Scanning Line
    val infiniteTransition = rememberInfiniteTransition(label = "hologram")
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan"
    )

    // 3. Pulsing Icon Alpha
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // 4. Particles (Neon Ripple)
    val particles = remember { mutableStateListOf<Particle>() }
    var clickOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .scale(scale)
            .height(60.dp)
            .graphicsLayer {
                shadowElevation = 12.dp.toPx()
                shape = RoundedCornerShape(16.dp)
                clip = true
            }
            .clip(RoundedCornerShape(16.dp))
            // 5. Glassmorphism (Blur + Alpha)
            .background(containerColor)
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.4f),
                        if (neonAccent) NeonGreen.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.1f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .drawBehind {
                // Scanning Line
                val xPos = size.width * scanProgress
                drawRect(
                    brush = Brush.horizontalGradient(
                        0.0f to Color.Transparent,
                        0.5f to (if (neonAccent) NeonGreen.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f)),
                        1.0f to Color.Transparent
                    ),
                    topLeft = Offset(xPos - 50.dp.toPx(), 0f),
                    size = androidx.compose.ui.geometry.Size(100.dp.toPx(), size.height)
                )
            }
            .pointerInput(Unit) {
                // Capture touch point for particles
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        clickOffset = event.changes.first().position
                    }
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                // Launch Particles
                scope.launch {
                    val random = Random()
                    val newParticles = List(10) {
                        val angle = random.nextFloat() * 2 * Math.PI
                        val distance = random.nextFloat() * 100f + 50f
                        Particle(
                            x = Animatable(clickOffset.x),
                            y = Animatable(clickOffset.y),
                            alpha = Animatable(1f),
                            size = random.nextFloat() * 6f + 2f
                        ).also { p ->
                            launch {
                                p.x.animateTo(clickOffset.x + (cos(angle) * distance).toFloat(), tween(600, easing = LinearOutSlowInEasing))
                            }
                            launch {
                                p.y.animateTo(clickOffset.y + (sin(angle) * distance).toFloat(), tween(600, easing = LinearOutSlowInEasing))
                            }
                            launch {
                                p.alpha.animateTo(0f, tween(600))
                            }
                        }
                    }
                    particles.addAll(newParticles)
                    delay(650)
                    particles.removeAll(newParticles)
                }
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        // Particle Canvas Layer
        Canvas(modifier = Modifier.fillMaxSize()) {
            particles.forEach { p ->
                drawCircle(
                    color = NeonGreen.copy(alpha = p.alpha.value),
                    radius = p.size,
                    center = Offset(p.x.value, p.y.value)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (neonAccent) NeonGreen else contentColor.copy(alpha = pulseAlpha),
                    modifier = Modifier.size(26.dp)
                )
                Spacer(Modifier.width(14.dp))
            }
            Text(
                text = text.uppercase(),
                color = if (neonAccent) NeonGreen else contentColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                letterSpacing = 2.sp
            )
        }
    }
}
