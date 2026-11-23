package com.xingheyuzhuan.shiguangschedule.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

internal fun ColorScheme.nailoongSurfaces(dark: Boolean): ColorScheme = if (dark) {
    copy(
        background = Color(0xFF1E170F),
        surface = Color(0xFF241B11),
        surfaceContainerLowest = Color(0xFF1A140D),
        surfaceContainerLow = Color(0xFF2C2115),
        surfaceContainer = Color(0xFF342619),
        surfaceContainerHigh = Color(0xFF3D2D1C),
        surfaceContainerHighest = Color(0xFF4B3823),
        onSurface = Color(0xFFFFEFD5),
        onSurfaceVariant = Color(0xFFD9C2A1),
        outlineVariant = Color(0xFF5D4830),
        primaryContainer = Color(0xFF654706),
        onPrimaryContainer = Color(0xFFFFE3A0)
    )
} else {
    copy(
        background = Color(0xFFFFF8EA),
        surface = Color(0xFFFFFCF4),
        surfaceContainerLowest = Color(0xFFFFFDF8),
        surfaceContainerLow = Color(0xFFFFF5DF),
        surfaceContainer = Color(0xFFFFEFD0),
        surfaceContainerHigh = Color(0xFFFFE9C0),
        surfaceContainerHighest = Color(0xFFF8E1B4),
        onSurface = Color(0xFF402713),
        onSurfaceVariant = Color(0xFF6C543B),
        outlineVariant = Color(0xFFE8D5AE),
        primary = Color(0xFF795200),
        primaryContainer = Color(0xFFFFE398),
        onPrimaryContainer = Color(0xFF482D00),
        secondaryContainer = Color(0xFFFFE6A7),
        onSecondaryContainer = Color(0xFF482D00)
    )
}
