package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StaggeredItem(
    index: Int,
    content: @Composable () -> Unit
) {
    // Only animate the first 6 items for maximum stability and speed
    val animIndex = index.coerceAtMost(6)
    val alphaAnim = remember { Animatable(0f) }
    val yOffsetAnim = remember { Animatable(40f) } // starts 40dp downward
    
    LaunchedEffect(Unit) {
        delay(animIndex * 35L) // Fast, premium responsive stagger
        launch {
            alphaAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(280, easing = LinearOutSlowInEasing)
            )
        }
        launch {
            yOffsetAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(320, easing = LinearOutSlowInEasing)
            )
        }
    }

    Box(
        modifier = Modifier.graphicsLayer {
            alpha = alphaAnim.value
            translationY = yOffsetAnim.value * density
        }
    ) {
        content()
    }
}

@Composable
fun BreathingBadge(
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .size(8.dp)
            .background(color, shape = CircleShape)
    )
}

fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1100f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "translate"
    )

    val shimmerColors = listOf(
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
    )

    background(
        brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(10f, 10f),
            end = Offset(translateAnim, translateAnim)
        )
    )
}
