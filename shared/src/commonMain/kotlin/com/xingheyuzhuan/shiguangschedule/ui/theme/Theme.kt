package com.xingheyuzhuan.shiguangschedule.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.material3.rememberDynamicColorScheme
import com.xingheyuzhuan.shiguangschedule.data.model.AppSettingsModel
import com.xingheyuzhuan.shiguangschedule.data.model.AppThemeMode
import com.xingheyuzhuan.shiguangschedule.data.model.AppThemePreset

/**
 * 定义一个用于全局同步深色模式状态的 Local 变量。
 * 供 UI 层判断"当前整体是不是深色"，用于换图、Canvas 绘制等场景。
 */
val LocalIsDarkTheme = staticCompositionLocalOf { false }
val LocalAppThemePreset = staticCompositionLocalOf { AppThemePreset.DEFAULT }

/**
 * 平台能力：获取当前系统深色状态（响应式）。
 */
@Composable
expect fun rememberSystemDark(): Boolean

/**
 * 外部调用的快捷主题函数
 * 自动根据 AppSettingsModel 处理所有主题逻辑
 */
@Composable
fun ShiguangScheduleTheme(
    settings: AppSettingsModel,
    content: @Composable () -> Unit
) {
    val systemDark = rememberSystemDark()

    val darkTheme = when (settings.themeMode) {
        AppThemeMode.FOLLOW_SYSTEM -> systemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    CompositionLocalProvider(
        LocalIsDarkTheme provides darkTheme,
        LocalAppThemePreset provides settings.themePreset
    ) {
        ShiguangScheduleTheme(
            darkTheme = darkTheme,
            dynamicColor = settings.useDynamicColor,
            customLightPrimary = Color(settings.customPrimaryColor.light),
            customDarkPrimary = Color(settings.customPrimaryColor.dark),
            themeMode = settings.themeMode,
            themePreset = settings.themePreset,
            content = content
        )
    }
}

/**
 * 核心主题实现函数（跨平台通用）
 */
@Composable
fun ShiguangScheduleTheme(
    darkTheme: Boolean = rememberSystemDark(),
    dynamicColor: Boolean = true,
    customLightPrimary: Color = DefaultThemeColor,
    customDarkPrimary: Color = DefaultThemeColor,
    themeMode: AppThemeMode = AppThemeMode.FOLLOW_SYSTEM,
    themePreset: AppThemePreset = AppThemePreset.DEFAULT,
    content: @Composable () -> Unit
) {
    val baseScheme = if (themePreset == AppThemePreset.NAILOONG) {
        rememberMaterialKolorScheme(darkTheme, Color(0xFFF9B72E))
    } else {
        rememberColorScheme(darkTheme, dynamicColor, customLightPrimary, customDarkPrimary)
    }
    val seed = if (darkTheme) customDarkPrimary else customLightPrimary
    val colorScheme = when {
        themePreset == AppThemePreset.NAILOONG -> baseScheme.nailoongSurfaces(darkTheme)
        dynamicColor || seed != DefaultThemeColor -> baseScheme
        darkTheme -> baseScheme.copy(
            primary = Color(0xFF61E7D0),
            onPrimary = Color(0xFF00382E),
            primaryContainer = Color(0xFF133E39),
            onPrimaryContainer = Color(0xFFB1F5E9),
            background = Color(0xFF101519),
            surface = Color(0xFF101519),
            surfaceContainerLowest = Color(0xFF0D1115),
            surfaceContainerLow = Color(0xFF171E24),
            surfaceContainer = Color(0xFF1D262D),
            surfaceContainerHigh = Color(0xFF232D35),
            surfaceContainerHighest = Color(0xFF2B3740),
            outlineVariant = Color(0xFF374952),
            onSurface = Color(0xFFE9F0F3),
            onSurfaceVariant = Color(0xFFB8C8D0)
        )
        else -> baseScheme.copy(
            primary = Color(0xFF3F6351),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFDDEBDF),
            onPrimaryContainer = Color(0xFF264938),
            background = Color(0xFFFBFAF7),
            surface = Color.White,
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = Color(0xFFF2F3ED),
            surfaceContainer = Color(0xFFEDEFE7),
            surfaceContainerHigh = Color(0xFFE5E8DE),
            surfaceContainerHighest = Color(0xFFDDE2D7),
            outlineVariant = Color(0xFFD1D9CC),
            onSurface = Color(0xFF25312A),
            onSurfaceVariant = Color(0xFF546257)
        )
    }

    // 应用平台特定的窗口与系统栏外观控制
    SetupPlatformThemeEffects(
        colorScheme = colorScheme,
        darkTheme = darkTheme,
        themeMode = themeMode
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * 共享的 MaterialKolor 动态配色方案生成函数
 */
@Composable
fun rememberMaterialKolorScheme(
    darkTheme: Boolean,
    seedColor: Color,
    style: PaletteStyle = PaletteStyle.TonalSpot
): ColorScheme {
    return rememberDynamicColorScheme(
        seedColor = seedColor,
        isDark = darkTheme,
        style = style
    )
}

/**
 * 平台特定的配色生成声明
 */
@Composable
expect fun rememberColorScheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    customLightPrimary: Color,
    customDarkPrimary: Color
): ColorScheme

/**
 * 平台特定的窗口与系统栏外观控制声明
 */
@Composable
expect fun SetupPlatformThemeEffects(
    colorScheme: ColorScheme,
    darkTheme: Boolean,
    themeMode: AppThemeMode
)

/**
 * 平台特定的能力：当前系统/平台是否支持 Dynamic Color (动态取色)
 */
expect val supportsDynamicColor: Boolean
