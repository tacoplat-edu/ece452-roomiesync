package com.example.roomiesync.ui.components

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roomiesync.data.ExpenseWithDetails
import com.example.roomiesync.ui.theme.ErrorRed
import com.example.roomiesync.ui.theme.PrimaryGreen
import java.text.NumberFormat


@Composable
fun ExpenseDetail(
    expenseData: ExpenseWithDetails,
    currentUserId: String,
    formatter: NumberFormat,
    onMarkPaid: (String) -> Unit
) {
    val expense = expenseData.expense
    val iPaidTotal = expense.paidById == currentUserId

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = expense.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "${expenseData.paidByProfile.displayName} paid ${formatter.format(expense.amount)}",
            color = Color.Gray,
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Splits", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        expenseData.splits.forEach { detail ->
            // don't need to show the person who originally paid the bill owing themselves
            if (detail.profile.id != expense.paidById) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = detail.profile.displayName ?: "Someone",
                            fontSize = 16.sp,
                            textDecoration = if (detail.split.isPaid) TextDecoration.LineThrough else null,
                            color = if (detail.split.isPaid) Color.Gray else Color.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formatter.format(detail.split.amountOowed),
                            fontWeight = FontWeight.SemiBold,
                            color = if (detail.split.isPaid) Color.Gray else Color.Black
                        )
                    }

                    if (detail.split.isPaid) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Paid", tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Paid", color = PrimaryGreen, fontSize = 14.sp)
                        }
                    } else {
                        if (detail.profile.id == currentUserId) {
                            Button(
                                onClick = { onMarkPaid(detail.split.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Settle Up")
                            }
                        }
                        else if (iPaidTotal) {
                            TextButton(onClick = { onMarkPaid(detail.split.id) }) {
                                Text("Mark Received", color = PrimaryGreen)
                            }
                        }
                        else {
                            Text("Unpaid", color = ErrorRed, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}