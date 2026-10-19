package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionType
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDate
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
import com.example.viewmodel.FinanceUiState

@Composable
fun TransactionsScreen(
    state: FinanceUiState,
    onSearchQueryChange: (String) -> Unit,
    onTypeFilterChange: (TransactionType?) -> Unit,
    onCategoryFilterChange: (Long?) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Search bar
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Поиск по названию или заметке...", color = TextMutedDark, fontSize = 14.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TextSecondaryDark
                )
            },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = TextSecondaryDark
                        )
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SlateSurfaceDark,
                unfocusedContainerColor = SlateSurfaceDark,
                focusedBorderColor = EmeraldPrimary,
                unfocusedBorderColor = SlateBorderDark,
                focusedTextColor = TextPrimaryDark,
                unfocusedTextColor = TextPrimaryDark
            ),
            singleLine = true
        )

        // Type filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = state.selectedTypeFilter == null,
                onClick = { onTypeFilterChange(null) },
                label = { Text("Все", fontSize = 13.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EmeraldPrimary,
                    selectedLabelColor = ColorThemeBlack(),
                    containerColor = SlateCardDark,
                    labelColor = TextSecondaryDark
                )
            )
            FilterChip(
                selected = state.selectedTypeFilter == TransactionType.EXPENSE,
                onClick = { onTypeFilterChange(TransactionType.EXPENSE) },
                label = { Text("Расходы", fontSize = 13.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ExpenseRed,
                    selectedLabelColor = TextPrimaryDark,
                    containerColor = SlateCardDark,
                    labelColor = TextSecondaryDark
                )
            )
            FilterChip(
                selected = state.selectedTypeFilter == TransactionType.INCOME,
                onClick = { onTypeFilterChange(TransactionType.INCOME) },
                label = { Text("Доходы", fontSize = 13.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = IncomeGreen,
                    selectedLabelColor = ColorThemeBlack(),
                    containerColor = SlateCardDark,
                    labelColor = TextSecondaryDark
                )
            )
        }

        // Category filter horizontal chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = state.selectedCategoryIdFilter == null,
                onClick = { onCategoryFilterChange(null) },
                label = { Text("Любая категория", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SlateBorderDark,
                    selectedLabelColor = TextPrimaryDark,
                    containerColor = SlateSurfaceDark,
                    labelColor = TextMutedDark
                )
            )
            state.categories.forEach { cat ->
                FilterChip(
                    selected = state.selectedCategoryIdFilter == cat.id,
                    onClick = { onCategoryFilterChange(cat.id) },
                    label = { Text(cat.name, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SlateBorderDark,
                        selectedLabelColor = TextPrimaryDark,
                        containerColor = SlateSurfaceDark,
                        labelColor = TextSecondaryDark
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Transaction items list
        val filtered = state.filteredTransactions
        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        tint = TextMutedDark,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Операций по заданным фильтрам не найдено",
                        color = TextSecondaryDark,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { tx ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateSurfaceDark)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryIconBadge(
                                iconName = tx.categoryIcon,
                                colorHex = tx.categoryColorHex
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tx.title,
                                    color = TextPrimaryDark,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${tx.categoryName} • ${formatDate(tx.date)}",
                                    color = TextSecondaryDark,
                                    fontSize = 12.sp
                                )
                                if (tx.note.isNotBlank()) {
                                    Text(
                                        text = tx.note,
                                        color = TextMutedDark,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                            }

                            val isIncome = tx.type == TransactionType.INCOME
                            val amountText = (if (isIncome) "+ " else "- ") + formatCurrency(tx.amount)
                            val amountColor = if (isIncome) IncomeGreen else TextPrimaryDark

                            Text(
                                text = amountText,
                                color = amountColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )

                            IconButton(
                                onClick = { transactionToDelete = tx },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = TextMutedDark,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (transactionToDelete != null) {
        val target = transactionToDelete!!
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Удалить операцию?", color = TextPrimaryDark) },
            text = {
                Text(
                    "Вы действительно хотите удалить «${target.title}» (${formatCurrency(target.amount)})?",
                    color = TextSecondaryDark
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteTransaction(target.id)
                        transactionToDelete = null
                    }
                ) {
                    Text("Удалить", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("Отмена", color = TextSecondaryDark)
                }
            },
            containerColor = SlateSurfaceDark
        )
    }
}

@Composable
private fun ColorThemeBlack() = androidx.compose.ui.graphics.Color(0xFF0F172A)
