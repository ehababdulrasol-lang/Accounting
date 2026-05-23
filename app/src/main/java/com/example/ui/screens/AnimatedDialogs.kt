package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

@Composable
fun SelfDrawingCheckmark(modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "CheckmarkPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    Box(
        modifier = modifier.size(100.dp),
        contentAlignment = Alignment.Center
    ) {
        // Pulsating glowing backdrop
        Box(
            modifier = Modifier
                .size(72.dp)
                .scale(pulseScale)
                .background(
                    color = Color(0xFF10B981).copy(alpha = 0.12f), // Emerald Green
                    shape = CircleShape
                )
        )

        Canvas(modifier = Modifier.size(72.dp)) {
            val strokeWidthPx = 5.dp.toPx()
            val color = Color(0xFF10B981) // Emerald Green

            // Outer ring
            drawCircle(
                color = color.copy(alpha = 0.2f),
                radius = size.width / 2f,
                style = Stroke(width = 2.dp.toPx())
            )

            // Inner check drawing calculations
            val p = progress.value
            val startX1 = size.width * 0.25f
            val startY1 = size.height * 0.5f
            val endX1 = size.width * 0.45f
            val endY1 = size.height * 0.7f

            val startX2 = endX1
            val startY2 = endY1
            val endX2 = size.width * 0.75f
            val endY2 = size.height * 0.35f

            if (p > 0f) {
                val segment1Fraction = (p / 0.4f).coerceIn(0f, 1f)
                val currentEndX1 = startX1 + (endX1 - startX1) * segment1Fraction
                val currentEndY1 = startY1 + (endY1 - startY1) * segment1Fraction

                drawLine(
                    color = color,
                    start = Offset(startX1, startY1),
                    end = Offset(currentEndX1, currentEndY1),
                    strokeWidth = strokeWidthPx,
                    cap = StrokeCap.Round
                )
            }

            if (p > 0.4f) {
                val segment2Fraction = ((p - 0.4f) / 0.6f).coerceIn(0f, 1f)
                val currentEndX2 = startX2 + (endX2 - startX2) * segment2Fraction
                val currentEndY2 = startY2 + (endY2 - startY2) * segment2Fraction

                drawLine(
                    color = color,
                    start = Offset(startX2, startY2),
                    end = Offset(currentEndX2, currentEndY2),
                    strokeWidth = strokeWidthPx,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun AnimatedDeleteWarningIcon(modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "DeleteWarning")
    val rotationState by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(220, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Wobble"
    )

    val scaleState by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    Box(
        modifier = modifier.size(100.dp),
        contentAlignment = Alignment.Center
    ) {
        // Pulsating glowing backdrop
        Box(
            modifier = Modifier
                .size(72.dp)
                .scale(scaleState)
                .background(
                    color = Color(0xFFEF4444).copy(alpha = 0.1f), // Rose Red
                    shape = CircleShape
                )
        )

        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = Color(0xFFEF4444),
            modifier = Modifier
                .size(54.dp)
                .scale(progress.value)
                .graphicsLayer(rotationZ = rotationState)
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedDeleteConfirmDialog(
    title: String,
    message: String,
    lang: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var animateEntrance by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        animateEntrance = true
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.56f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = animateEntrance,
                enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(16.dp)
                        .clickable(enabled = false) {}, // prevent click propagation
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AnimatedDeleteWarningIcon()

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = message,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Cancel Button
                            var isCancelPressed by remember { mutableStateOf(false) }
                            val cancelScale by animateFloatAsState(if (isCancelPressed) 0.95f else 1f)

                            OutlinedButton(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .graphicsLayer {
                                        scaleX = cancelScale
                                        scaleY = cancelScale
                                    },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text(
                                    text = if (lang == "ar") "إلغاء" else "Cancel",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Confirm Button (Red delete action)
                            var isConfirmPressed by remember { mutableStateOf(false) }
                            val confirmScale by animateFloatAsState(if (isConfirmPressed) 0.95f else 1f)

                            Button(
                                onClick = onConfirm,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .graphicsLayer {
                                        scaleX = confirmScale
                                        scaleY = confirmScale
                                    },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFEF4444),
                                    contentColor = Color.White
                                )
                            ) {
                                Text(
                                    text = if (lang == "ar") "حذف" else "Delete",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SuccessTickDialog(
    message: String,
    lang: String,
    durationMs: Long = 1300L,
    onDismiss: () -> Unit
) {
    var animateEntrance by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        animateEntrance = true
        delay(durationMs)
        animateEntrance = false
        delay(150) // wait for exit animation
        onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = animateEntrance,
                enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .padding(16.dp)
                        .clickable(enabled = false) {},
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SelfDrawingCheckmark()

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (lang == "ar") "تم العملية بنجاح!" else "Operation Successful!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = message,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
