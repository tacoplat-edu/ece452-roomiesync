package com.example.roomiesync.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roomiesync.ui.theme.PrimaryGreen

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = PrimaryGreen,
    contentColor: Color = Color.White
) {
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(64.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
    }
}
