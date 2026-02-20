package com.example.roomiesync.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

fun getRequiredLabel(text: String): AnnotatedString {
    return buildAnnotatedString {
        append(text)
        withStyle(style = SpanStyle(color = Color.Red)) {
            append(" *")
        }
    }
}
