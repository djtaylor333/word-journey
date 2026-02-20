package com.djtaylor.wordjourney.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val WordJourneysTypography = Typography(
    displayLarge = TextStyle(
        fontFamily   = FontFamily.Serif,
        fontWeight   = FontWeight.Bold,
        fontSize     = 57.sp,
        lineHeight   = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineLarge = TextStyle(
        fontFamily   = FontFamily.Serif,
        fontWeight   = FontWeight.Bold,
        fontSize     = 32.sp,
        lineHeight   = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily   = FontFamily.Serif,
        fontWeight   = FontWeight.Bold,
        fontSize     = 28.sp,
        lineHeight   = 36.sp
    ),
    titleLarge = TextStyle(
        fontFamily   = FontFamily.Serif,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 22.sp,
        lineHeight   = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily   = FontFamily.SansSerif,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 16.sp,
        lineHeight   = 24.sp,
        letterSpacing = 0.15.sp
    ),
    bodyLarge = TextStyle(
        fontFamily   = FontFamily.SansSerif,
        fontWeight   = FontWeight.Normal,
        fontSize     = 16.sp,
        lineHeight   = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily   = FontFamily.SansSerif,
        fontWeight   = FontWeight.Normal,
        fontSize     = 14.sp,
        lineHeight   = 20.sp,
        letterSpacing = 0.25.sp
    ),
    labelLarge = TextStyle(
        fontFamily   = FontFamily.SansSerif,
        fontWeight   = FontWeight.Bold,
        fontSize     = 14.sp,
        lineHeight   = 20.sp,
        letterSpacing = 2.sp     // generous tracking on game tile letters
    ),
    labelMedium = TextStyle(
        fontFamily   = FontFamily.SansSerif,
        fontWeight   = FontWeight.Bold,
        fontSize     = 12.sp,
        lineHeight   = 16.sp,
        letterSpacing = 0.5.sp
    )
)
