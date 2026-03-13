package com.example.roomiesync.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roomiesync.data.ExpenseWithDetails
import com.example.roomiesync.ui.theme.ErrorRed
import com.example.roomiesync.ui.theme.PrimaryGreen
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExpenseListItem(
    expenseData: ExpenseWithDetails,
    currentUserId: String,
    formatter: NumberFormat,
    onClick: () -> Unit
) {
    val expense = expenseData.expense
    val iPaid = expense.paidById == currentUserId

    val mySplit = expenseData.splits.firstOrNull { it.profile.id == currentUserId }
    val myShare = mySplit?.split?.amountOowed ?: 0.0
    val mySplitIsPaid = mySplit?.split?.isPaid == true
    // Only "settled" for payer when everyone else has paid; treat null/isPaid false as unpaid
    val allOtherSplitsPaid = expenseData.splits
        .filter { it.profile.id != expense.paidById }
        .all { it.split.isPaid }

    val statusText: String
    val statusColor: Color
    val amountText: String

    if (iPaid) {
        val totalLent = expense.amount - myShare
        val amountUnpaid = expenseData.splits
            .filter { it.profile.id != expense.paidById && !it.split.isPaid }
            .sumOf { it.split.amountOowed }

        if (amountUnpaid == 0.0 && allOtherSplitsPaid) {
            statusText = "settled"
            statusColor = Color.Gray
            amountText = formatter.format(totalLent)
        } else {
            statusText = "you are owed"
            statusColor = PrimaryGreen
            amountText = formatter.format(amountUnpaid)
        }
    } else {
        if (mySplit == null) {
            statusText = "Not involved"
            statusColor = Color.Gray
            amountText = "—"
        } else if (mySplitIsPaid) {
            statusText = "settled"
            statusColor = Color.Gray
            amountText = formatter.format(myShare)
        } else {
            statusText = "you owe ${expenseData.paidByProfile.displayName ?: "Someone"}"
            statusColor = ErrorRed
            amountText = formatter.format(myShare)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF0F0F0)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AttachMoney, contentDescription = null, tint = Color.Gray)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = expense.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                val dateStr = try {
                    val instant = java.time.Instant.parse(expense.createdAt)
                    SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(instant.toEpochMilli()))
                } catch (_: Exception) {
                    expense.createdAt.take(10)
                }
                Text(text = "${expenseData.paidByProfile.displayName} paid ${formatter.format(expense.amount)} • $dateStr", color = Color.Gray, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = statusText, color = Color.Gray, fontSize = 12.sp)
                Text(text = amountText, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}