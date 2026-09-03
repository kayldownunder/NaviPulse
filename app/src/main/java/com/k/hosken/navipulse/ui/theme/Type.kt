package com.k.hosken.navipulse.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isUnspecified
import com.k.hosken.navipulse.R
import com.k.hosken.navipulse.data.AppFont
import com.k.hosken.navipulse.data.AppTextColor
import com.k.hosken.navipulse.data.AppTextSize

private val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private fun googleFontFamily(name: String): FontFamily = FontFamily(
    Font(googleFont = GoogleFont(name), fontProvider = googleFontProvider)
)

/** [AppFont.label] is already the exact Google Fonts family name for every entry but
 * [AppFont.SYSTEM_DEFAULT], which opts out of a downloaded font entirely. */
fun AppFont.toFontFamily(): FontFamily =
    if (this == AppFont.SYSTEM_DEFAULT) FontFamily.Default else googleFontFamily(label)

fun AppTextColor.toColor(): Color = when (this) {
    AppTextColor.DEFAULT -> Color.Unspecified
    AppTextColor.WHITE -> Color.White
    AppTextColor.BLACK -> Color.Black
    AppTextColor.RED -> Color(0xFFE53935)
    AppTextColor.ORANGE -> Color(0xFFFB8C00)
    AppTextColor.YELLOW -> Color(0xFFFDD835)
    AppTextColor.GREEN -> Color(0xFF43A047)
    AppTextColor.BLUE -> Color(0xFF1E88E5)
    AppTextColor.PURPLE -> Color(0xFF8E24AA)
    AppTextColor.PINK -> Color(0xFFD81B60)
    AppTextColor.GRAY -> Color(0xFF757575)
}

fun AppTextSize.toScale(): Float = sp / AppTextSize.DEFAULT.sp.toFloat()

private fun TextUnit.scaled(scale: Float): TextUnit = if (isUnspecified) this else this * scale

/**
 * Builds the app's Typography from the user's chosen font/color/size. [textColor] is applied
 * only to content styles (body/title/headline/display) - label styles are left alone since
 * Material components like Button and NavigationBar use them with their own contentColor for
 * contrast (e.g. onPrimary/onError), and forcing a flat color there could make button text
 * unreadable against its container.
 */
fun typographyFor(family: FontFamily, textColor: Color, sizeScale: Float): Typography {
    val base = Typography()
    fun TextStyle.themedContent(): TextStyle = copy(
        fontFamily = family,
        color = textColor,
        fontSize = fontSize.scaled(sizeScale),
        lineHeight = lineHeight.scaled(sizeScale)
    )
    fun TextStyle.themedLabel(): TextStyle = copy(
        fontFamily = family,
        fontSize = fontSize.scaled(sizeScale),
        lineHeight = lineHeight.scaled(sizeScale)
    )
    return base.copy(
        displayLarge = base.displayLarge.themedContent(),
        displayMedium = base.displayMedium.themedContent(),
        displaySmall = base.displaySmall.themedContent(),
        headlineLarge = base.headlineLarge.themedContent(),
        headlineMedium = base.headlineMedium.themedContent(),
        headlineSmall = base.headlineSmall.themedContent(),
        titleLarge = base.titleLarge.themedContent(),
        titleMedium = base.titleMedium.themedContent(),
        titleSmall = base.titleSmall.themedContent(),
        bodyLarge = base.bodyLarge.themedContent(),
        bodyMedium = base.bodyMedium.themedContent(),
        bodySmall = base.bodySmall.themedContent(),
        labelLarge = base.labelLarge.themedLabel(),
        labelMedium = base.labelMedium.themedLabel(),
        labelSmall = base.labelSmall.themedLabel()
    )
}
