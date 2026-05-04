package com.jb.shoplist.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Immutable
data class ShopListCustomColors(
    val tacheNonFaite: Color = Color.Unspecified,
    val tacheFaite: Color = Color.Unspecified
)

val LocalShopListColors = staticCompositionLocalOf { ShopListCustomColors() }

object ShopListTheme {
    val customColors: ShopListCustomColors
        @Composable
        get() = LocalShopListColors.current
}

private val DarkColorScheme = darkColorScheme(
    primary = CouleurLibelle,
    onPrimary = CouleurFond,
    surface = CouleurFond,
    onSurface = CouleurLibelle,
    surfaceVariant = CouleurFond,
    onSurfaceVariant = CouleurLibelle,
    background = CouleurFond,
    onBackground = CouleurLibelle,
)

@Composable
fun ShopListTheme(
    content: @Composable () -> Unit
) {
    val customColors = ShopListCustomColors(
        tacheNonFaite = CouleurTacheNonFaite,
        tacheFaite = CouleurTacheFaite
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DarkColorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    CompositionLocalProvider(LocalShopListColors provides customColors) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = Typography,
            content = content
        )
    }
}
