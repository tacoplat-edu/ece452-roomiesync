@file:OptIn(ExperimentalTime::class)

package com.example.roomiesync.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.roomiesync.chore.ChoreAssignment
import com.example.roomiesync.ui.components.ChoreStatus
import com.example.roomiesync.ui.theme.ErrorRed
import com.example.roomiesync.ui.theme.PrimaryBackground
import com.example.roomiesync.ui.theme.PrimaryGreen
import com.example.roomiesync.ui.theme.WarningYellow
import com.example.roomiesync.utils.getRelativeTimeText
import io.github.jan.supabase.auth.user.UserInfo
import kotlin.time.ExperimentalTime

@Composable
fun HomeScreen(
    user: UserInfo,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    if (uiState.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryGreen)
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(color = PrimaryBackground)
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Hello,",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.Gray
                )
                Text(
                    text = uiState.profile?.displayName ?: "User",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(PrimaryGreen.copy(alpha = 0.1f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Home, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = uiState.house?.name ?: "No House Joined",
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryGreen
                    )
                }
            }
        }

        item {
            Column {
                Text(
                    text = "My Chores",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (uiState.chores.isEmpty()) {
                    Text("You've finished all your chores!", color = Color.Gray)
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(uiState.chores) { assignment ->
                            ChoreCard(assignment = assignment)
                        }
                    }
                }
            }
        }

        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "Recent Activity",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )

                uiState.recentActivity.forEach { activity ->
                    ActivityFeedCard(activity = activity)
                }
            }
        }
    }
}

@Composable
fun ChoreCard(assignment: ChoreAssignment) {
    val status = try { ChoreStatus.valueOf(assignment.status) } catch (e: Exception) { ChoreStatus.NOT_URGENT }

    val (cardColor, textColor, label) = when (status) {
        ChoreStatus.OVERDUE -> Triple(ErrorRed.copy(alpha = 0.1f), ErrorRed, "OVERDUE")
        ChoreStatus.URGENT -> Triple(WarningYellow.copy(alpha = 0.1f), WarningYellow, "DUE SOON")
        ChoreStatus.PENDING_APPROVAL -> Triple(Color(0xFF2196F3).copy(alpha = 0.1f), Color(0xFF2196F3), "PENDING")
        ChoreStatus.NOT_URGENT -> Triple(PrimaryGreen.copy(alpha = 0.1f), PrimaryGreen, "TO DO")
    }

    Card(
        modifier = Modifier
            .width(200.dp)
            .height(120.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (status == ChoreStatus.NOT_URGENT) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }

            Text(
                text = assignment.chore?.title ?: "Unknown",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = getRelativeTimeText(assignment.dueDate),
                fontSize = 12.sp,
                color = Color.DarkGray
            )
        }
    }
}

@Composable
fun ActivityFeedCard(activity: ActivityFeedItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            val (icon, color) = when (activity.iconType) {
                ActivityIconType.CHORE_COMPLETED -> Icons.Default.CheckCircle to PrimaryGreen
                ActivityIconType.CHORE_ADDED -> Icons.Outlined.AddCircle to PrimaryGreen
                ActivityIconType.BILL_PAID -> Icons.Default.AttachMoney to WarningYellow
                ActivityIconType.CHAT -> Icons.Outlined.ChatBubbleOutline to Color.Gray
            }
            Icon(imageVector = icon, contentDescription = null, tint = color)
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = activity.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(text = activity.description, color = Color.DarkGray, fontSize = 14.sp)
        }

        val timeAgo = android.text.format.DateUtils.getRelativeTimeSpanString(
            activity.timestampMillis,
            System.currentTimeMillis(),
            android.text.format.DateUtils.MINUTE_IN_MILLIS
        ).toString()

        Text(text = timeAgo, fontSize = 12.sp, color = Color.Gray)
    }
}