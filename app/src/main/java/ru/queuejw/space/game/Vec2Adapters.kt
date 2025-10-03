package ru.queuejw.space.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

fun Vec2.toOffset(): Offset = Offset(x, y)

fun DrawScope.drawCircle(color: Color, radius: Float, center: Vec2, alpha: Float = 1f, style: Stroke? = null) {
    if (style != null) drawCircle(color = color, radius = radius, center = center.toOffset(), alpha = alpha, style = style)
    else drawCircle(color = color, radius = radius, center = center.toOffset(), alpha = alpha)
}

fun DrawScope.drawLine(color: Color, start: Vec2, end: Vec2, strokeWidth: Float = 1f) {
    drawLine(color = color, start = start.toOffset(), end = end.toOffset(), strokeWidth = strokeWidth)
}

fun Path.translate(v: Vec2) { translate(v.toOffset()) }

