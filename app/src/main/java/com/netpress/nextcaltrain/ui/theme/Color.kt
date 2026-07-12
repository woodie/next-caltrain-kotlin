package com.netpress.nextcaltrain.ui.theme

import androidx.compose.ui.graphics.Color

// MARK: - Colors (named after legacy CSS variables, mirroring iOS AppStyle.swift)

// Blue — past/inactive trips
val CalPastLight = Color(0xFF0000FF) // #00F
val CalPastDark = Color(0xFF338CFF) // #338CFF

// Green — arriving trains
val CalArriveLight = Color(0xFF009E66) // #009e0b approx
val CalArriveDark = Color(0xFF00FF00) // #0F0

// Yellow — departing trains
val CalDepartLight = Color(0xFFCC9E12) // mustard
val CalDepartDark = Color(0xFFFFFF00) // #FF0

// Gray — swapped/manual schedule
val CalSwappedLight = Color(0xFF8C8C8C) // white: 0.55
val CalSwappedDark = Color(0xFF666666) // white: 0.40

// Icon circle backgrounds
val IconCircleBackgroundLight = Color(0xFFE0E0E0) // white: 0.88
val IconCircleBackgroundDark = Color(0xFF262626) // white: 0.15

// App background and text
val AppBackgroundLight = Color.White
val AppBackgroundDark = Color.Black
val AppTextLight = Color.Black
val AppTextDark = Color.White
