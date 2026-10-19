package com.ami.music.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable fun Artwork(style: Int, modifier: Modifier = Modifier, label: String? = null) {
    val palette = when (style % 4) {
        0 -> listOf(Color(0xFF174A49), Color(0xFF8BBDA6), Color(0xFFE7DBAD))
        1 -> listOf(Color(0xFFB43C27), Color(0xFFEA9F63), Color(0xFFF0DDC4))
        2 -> listOf(Color(0xFF493D84), Color(0xFFAC9AD2), Color(0xFFE6BCC7))
        else -> listOf(Color(0xFF17446A), Color(0xFF6BA4C3), Color(0xFFD4DDAF))
    }
    Box(modifier.clip(RoundedCornerShape(10.dp)).background(palette[0])) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.linearGradient(palette, Offset.Zero, Offset(size.width, size.height)))
            when (style % 4) {
                0 -> {
                    for (i in 0..13) {
                        val shift = i * size.width * .13f
                        drawOval(palette[0].copy(alpha = .48f), topLeft = Offset(-size.width * .55f + shift, -size.height * .3f), size = Size(size.width * .85f, size.height * 1.9f), style = androidx.compose.ui.graphics.drawscope.Stroke(size.width * .027f))
                    }
                }
                1 -> {
                    drawCircle(palette[2], size.width * .3f, Offset(size.width * .58f, size.height * .42f))
                    for (i in 0..10) drawRect(palette[0].copy(alpha = .7f), Offset(0f, size.height * (.46f + i * .055f)), Size(size.width, size.height * .022f))
                }
                2 -> rotate(-28f) {
                    for (i in -3..9) drawRoundRect(palette[if (i % 2 == 0) 0 else 2].copy(alpha = .45f), Offset(i * size.width * .18f, -size.height), Size(size.width * .09f, size.height * 3), androidx.compose.ui.geometry.CornerRadius(size.width * .1f))
                }
                else -> {
                    for (i in 0..7) drawCircle(palette[if (i % 2 == 0) 0 else 2].copy(alpha = .7f), size.width * (.7f - i * .08f), Offset(size.width * .8f, size.height * .7f))
                }
            }
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .32f))))
        }
        if (label != null) Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Bottom) {
            Text("AMI ORIGINALS", color = Color.White.copy(alpha = .8f), fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(6.dp))
            Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 26.sp)
        }
    }
}
