package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Budget
import com.example.domain.model.Category
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.formatCurrency
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.SlateBorderDark
import com.example.ui.theme.SlateCardDark
import com.example.ui.theme.SlateSurfaceDark
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.theme.WarningAmber
import com.example.viewmodel.FinanceUiState

@Composable
fun BudgetsScreen(
    state: FinanceUiState,
    onOpenSetBudget: (Category?) -> Unit,
    onDeleteBudget: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalBudgetLimit = state.budgets.sumOf { it.monthlyLimit }
    val totalBudgetSpent = state.budgets.sumOf { it.spentAmount }
    val totalProgress = if (totalBudgetLimit > 0) (totalBudgetSpent / totalBudgetLimit).toFloat().coerceIn(0f, 1.5f) else 0f

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Overview Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurfaceDark)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Бюджет на текущий месяц",
                        color = TextSecondaryDark,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "Потрачено ${formatCurrency(totalBudgetSpent)}",
                                color = TextPrimaryDark,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "из общего лимита ${formatCurrency(totalBudgetLimit)}",
                                color = TextSecondaryDark,
                                fontSize = 13.sp
                            )
                        }

                        Text(
                            text = "${(totalProgress * 100).toInt()}%",
                            color = if (totalProgress > 1f) ExpenseRed else if (totalProgress > 0.8f) WarningAmber else EmeraldLight,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val barColor = if (totalProgress > 1f) ExpenseRed else if (totalProgress > 0.8f) WarningAmber else EmeraldPrimary
                    LinearProgressIndicator(
                        progress = { totalProgress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = barColor,
                        trackColor = SlateBorderDark
                    )
                }
            }
        }

        // Action header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Лимиты по категориям",
                    color = TextPrimaryDark,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = { onOpenSetBudget(null) },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Budget",
                        tint = androidx.compose.ui.graphics.Color(0xFF0F172A),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Задать лимит",
                        color = androidx.compose.ui.graphics.Color(0xFF0F172A),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (state.budgets.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateSurfaceDark)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = TextMutedDark,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Лимиты бюджетов еще не установлены",
                            color = TextSecondaryDark,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Установите лимиты для контроля трат",
                            color = TextMutedDark,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(state.budgets, key = { it.id }) { budget ->
                val matchedCat = state.categories.firstOrNull { it.id == budget.categoryId }
                val iconName = matchedCat?.iconName ?: "shopping_cart"
                val colorHex = matchedCat?.colorHex ?: "#FF7043"

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateSurfaceDark)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryIconBadge(
                                iconName = iconName,
                                colorHex = colorHex
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = budget.categoryName,
                                    color = TextPrimaryDark,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                val progressPct = (budget.progress * 100).toInt()
                                Text(
                                    text = "Потрачено ${formatCurrency(budget.spentAmount)} из ${formatCurrency(budget.monthlyLimit)} ($progressPct%)",
                                    color = TextSecondaryDark,
                                    fontSize = 12.sp
                                )
                            }

                            IconButton(
                                onClick = { onOpenSetBudget(matchedCat) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = EmeraldLight,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val progressColor = if (budget.isOverBudget) ExpenseRed else if (budget.progress > 0.8f) WarningAmber else EmeraldPrimary
                        LinearProgressIndicator(
                            progress = { budget.progress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = progressColor,
                            trackColor = SlateBorderDark
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (budget.isOverBudget) {
                                val over = budget.spentAmount - budget.monthlyLimit
                                Text(
                                    text = "Перерасход: +${formatCurrency(over)}",
                                    color = ExpenseRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text(
                                    text = "Осталось: ${formatCurrency(budget.remainingAmount)}",
                                    color = IncomeGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Text(
                                text = "Лимит: ${formatCurrency(budget.monthlyLimit)}",
                                color = TextMutedDark,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}
