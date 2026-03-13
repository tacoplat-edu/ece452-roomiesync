package com.example.roomiesync.billing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.roomiesync.data.ExpenseWithDetails
import com.example.roomiesync.ui.components.ExpenseDetail
import com.example.roomiesync.ui.components.ExpenseListItem
import com.example.roomiesync.ui.components.FullWidthButtonWithIcon
import com.example.roomiesync.ui.theme.ErrorRed
import com.example.roomiesync.ui.theme.PrimaryBackground
import com.example.roomiesync.ui.theme.PrimaryGreen
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingScreen(
    onAddExpenseClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BillingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)

    LaunchedEffect(Unit) {
        viewModel.loadExpenses()
    }

    var selectedExpense by remember { mutableStateOf<ExpenseWithDetails?>(null) }

    if (uiState.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryGreen)
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = PrimaryBackground)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Overall Balance", color = Color.Gray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))

                val balanceColor = when {
                    uiState.overallBalance > 0 -> PrimaryGreen
                    uiState.overallBalance < 0 -> ErrorRed
                    else -> Color.Black
                }
                val balancePrefix = if (uiState.overallBalance > 0) "+" else ""

                Text(
                    text = "$balancePrefix${currencyFormatter.format(uiState.overallBalance)}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = balanceColor
                )

                Text(
                    text = when {
                        uiState.overallBalance > 0 -> "You are owed"
                        uiState.overallBalance < 0 -> "You owe"
                        else -> "Settled up"
                    },
                    color = balanceColor.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Recent Expenses",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(uiState.expenses) { expenseData ->
                ExpenseListItem(
                    expenseData = expenseData,
                    currentUserId = uiState.currentUserId,
                    formatter = currencyFormatter,
                    onClick = { selectedExpense = expenseData }
                )
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        FullWidthButtonWithIcon(
            text = "Add an expense",
            icon = Icons.Outlined.Add,
            onClick = onAddExpenseClick,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (selectedExpense != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        val freshExpenseData = uiState.expenses.find { it.expense.id == selectedExpense!!.expense.id }

        if (freshExpenseData != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedExpense = null },
                sheetState = sheetState,
                containerColor = Color.White
            ) {
                ExpenseDetail(
                    expenseData = freshExpenseData,
                    currentUserId = uiState.currentUserId,
                    formatter = currencyFormatter,
                    onMarkPaid = { splitId ->
                        viewModel.markSplitAsPaid(freshExpenseData.expense.id, splitId)
                    }
                )
            }
        } else {
            selectedExpense = null
        }
    }
}
