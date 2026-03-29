package com.example.timerapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.example.timerapp.ui.theme.GlassColors
import kotlin.math.cos
import kotlin.math.sin

/**
 * Ein performanter, sanft animierter Mesh-Gradient-Hintergrund 
 * passend zum Glassmorphism-Thema. Bewegt große farbige Orbs.
 */
@Composable
fun MeshGradientBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "meshGradient")

    // Drei verschiedene Frequenz-Timer für organisch wirkende Bewegungen
    val timeA by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "timeA"
    )

    val timeB by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "timeB"
    )

    val timeC by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "timeC"
    )

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Orb 1 (Blau-Violett)
            val orb1X = width / 2 + (width * 0.3f * sin(timeA))
            val orb1Y = height / 2 + (height * 0.3f * cos(timeA))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(GlassColors.MeshBlue.copy(alpha = 0.5f), GlassColors.MeshBlue.copy(alpha = 0f)),
                    center = Offset(orb1X, orb1Y),
                    radius = width * 0.8f
                ),
                radius = width * 1.5f,
                center = Offset(orb1X, orb1Y)
            )

            // Orb 2 (Pink)
            val orb2X = width / 2 + (width * 0.4f * cos(timeB))
            val orb2Y = height / 2 + (height * 0.4f * sin(timeB))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(GlassColors.MeshPink.copy(alpha = 0.4f), GlassColors.MeshPink.copy(alpha = 0f)),
                    center = Offset(orb2X, orb2Y),
                    radius = width * 0.7f
                ),
                radius = width * 1.5f,
                center = Offset(orb2X, orb2Y)
            )

            // Orb 3 (Cyan-Grün)
            val orb3X = width / 2 + (width * 0.2f * cos(timeC + 1f))
            val orb3Y = height / 2 + (height * 0.5f * sin(timeB + 2f))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(GlassColors.MeshCyan.copy(alpha = 0.4f), GlassColors.MeshCyan.copy(alpha = 0f)),
                    center = Offset(orb3X, orb3Y),
                    radius = width * 0.8f
                ),
                radius = width * 1.5f,
                center = Offset(orb3X, orb3Y)
            )
            
            // Orb 4 (Gelb-Orange für Akzente)
            val orb4X = width * 0.8f + (width * 0.3f * sin(timeC))
            val orb4Y = height * 0.2f + (height * 0.2f * cos(timeA))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(GlassColors.MeshAmber.copy(alpha = 0.3f), GlassColors.MeshAmber.copy(alpha = 0f)),
                    center = Offset(orb4X, orb4Y),
                    radius = width * 0.6f
                ),
                radius = width * 1.2f,
                center = Offset(orb4X, orb4Y)
            )
        }
    }
}
