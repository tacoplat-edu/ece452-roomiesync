package com.example.roomiesync.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roomiesync.ui.theme.DarkSecondaryGrey
import com.example.roomiesync.ui.theme.LightSecondaryGrey

@Composable
fun HouseholdInfoCard(
    houseName: String,
    address: String,
    joinCode: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth(0.9f)
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSecondaryGrey)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = houseName,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                var isAddressVisible by remember { mutableStateOf(true) }
                Text(
                    text = if (isAddressVisible) address else "••••••••",
                    fontSize = 18.sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { isAddressVisible = !isAddressVisible },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isAddressVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (isAddressVisible) "Hide address" else "Show address",
                        tint = Color.White
                    )
                }
            }

            Row(
                modifier = Modifier.padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Join Code",
                    fontSize = 18.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.width(24.dp))

                Text(
                    text = joinCode,
                    fontSize = 18.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Normal,
                )

                Spacer(modifier = Modifier.width(8.dp))

                val clipboardManager = LocalClipboardManager.current
                IconButton(
                    onClick = { clipboardManager.setText(AnnotatedString(joinCode)) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Copy join code",
                        tint = Color.White
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))

        Icon(
            imageVector = Icons.Filled.Build,
            contentDescription = "Advanced Settings",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}
