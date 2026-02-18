package com.example.roomiesync.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roomiesync.ui.theme.ErrorRed
import com.example.roomiesync.ui.theme.PrimaryGreen
import com.example.roomiesync.ui.theme.WarningYellow

enum class ChoreStatus {
    NOT_URGENT,
    URGENT,
    OVERDUE
}

@Composable
fun ChoreListItem(
    choreName: String,
    timeLeft: String,
    status: ChoreStatus = ChoreStatus.NOT_URGENT,
    onComplete: () -> Unit,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val (primaryColor, secondaryColor) = when (status) {
        ChoreStatus.NOT_URGENT -> PrimaryGreen to Color.Black
        ChoreStatus.URGENT -> WarningYellow to WarningYellow
        ChoreStatus.OVERDUE -> ErrorRed to ErrorRed
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(0.65f)
        ) {
            Text(
                text = choreName,
                color = secondaryColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = timeLeft,
                color = if (status == ChoreStatus.NOT_URGENT) Color.Black else secondaryColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(100))
                    .background(Color(0xFFEADDFF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = "Assignee",
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Completion Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(100))
                    .background(primaryColor)
                    .clickable { onComplete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Complete Chore",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
