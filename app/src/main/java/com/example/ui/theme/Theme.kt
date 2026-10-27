package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.data.model.JarvisTheme

@Composable
fun JarvisTheme(
    selectedTheme: JarvisTheme = JarvisTheme.CYAN,
    content: @Composable () -> Unit
) {
    val (primary, secondary, background, surface) = when (selectedTheme) {
        JarvisTheme.CYAN -> Quad(CyanCore, CyanGlow, HudDarkBg, HudSurface)
        JarvisTheme.CRIMSON -> Quad(CrimsonCore, GoldAccent, CrimsonDarkBg, CrimsonSurface)
        JarvisTheme.EMERALD -> Quad(EmeraldCore, EmeraldGlow, EmeraldDarkBg, EmeraldSurface)
        JarvisTheme.VIOLET -> Quad(VioletCore, VioletGlow, VioletDarkBg, VioletSurface)
        JarvisTheme.AMBER -> Quad(AmberCore, AmberGlow, AmberDarkBg, AmberSurface)
    }

    val colorScheme = darkColorScheme(
        primary = primary,
        onPrimary = Color.Black,
        primaryContainer = primary.copy(alpha = 0.2f),
        onPrimaryContainer = primary,
        secondary = secondary,
        onSecondary = Color.Black,
        background = background,
        onBackground = HudTextPrimary,
        surface = surface,
        onSurface = HudTextPrimary,
        surfaceVariant = surface.copy(alpha = 0.7f),
        onSurfaceVariant = HudTextSecondary,
        outline = primary.copy(alpha = 0.4f)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
