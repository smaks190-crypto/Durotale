package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.domain.model.Category
import com.example.domain.model.ParsedTransactionDraft
import com.example.domain.model.TransactionType
import com.example.ui.components.CategoryIconBadge
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.SlateBorderDark
import com.example.ui.theme.SlateCardDark
import com.example.ui.theme.SlateSurfaceDark
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@Composable
fun AddTransactionDialog(
    categories: List<Category>,
    initialDraft: ParsedTransactionDraft?,
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Double, type: TransactionType, categoryId: Long, date: Long, note: String) -> Unit
) {
    var type by remember {
        mutableStateOf(initialDraft?.type ?: TransactionType.EXPENSE)
    }
    var amountText by remember {
        mutableStateOf(initialDraft?.amount?.let { if (it > 0) it.toInt().toString() else "" } ?: "")
    }
    var title by remember {
        mutableStateOf(initialDraft?.title ?: "")
    }
    var note by remember {
        mutableStateOf(initialDraft?.note ?: "")
    }

    val defaultCat = categories.firstOrNull {
        initialDraft?.categoryName?.equals(it.name, ignoreCase = true) == true
    } ?: categories.firstOrNull()

    var selectedCategoryId by remember {
        mutableStateOf(defaultCat?.id ?: 1L)
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SlateSurfaceDark)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Новая операция",
                        color = TextPrimaryDark,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondaryDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Type Toggle (Расход / Доход)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SlateCardDark, shape = RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isExpense = type == TransactionType.EXPENSE
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isExpense) ExpenseRed else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { type = TransactionType.EXPENSE }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Расход",
                            color = if (isExpense) TextPrimaryDark else TextSecondaryDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (!isExpense) IncomeGreen else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { type = TransactionType.INCOME }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Доход",
                            color = if (!isExpense) Color(0xFF0F172A) else TextSecondaryDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Amount Field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() || it == '.' || it == ',' }
                        amountText = filtered
                        errorMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Сумма (₽)", color = TextSecondaryDark) },
                    placeholder = { Text("0", color = TextMutedDark) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SlateCardDark,
                        unfocusedContainerColor = SlateCardDark,
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = SlateBorderDark,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Title Field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Название операции", color = TextSecondaryDark) },
                    placeholder = { Text(if (type == TransactionType.INCOME) "Зарплата, Аванс..." else "Супермаркет, Кофе...", color = TextMutedDark) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SlateCardDark,
                        unfocusedContainerColor = SlateCardDark,
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = SlateBorderDark,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Category selector
                Text(
                    text = "Категория",
                    color = TextSecondaryDark,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories, key = { it.id }) { cat ->
                        val isSelected = cat.id == selectedCategoryId
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategoryId = cat.id },
                            leadingIcon = {
                                CategoryIconBadge(
                                    iconName = cat.iconName,
                                    colorHex = cat.colorHex,
                                    size = 24.dp,
                                    iconSize = 14.dp
                                )
                            },
                            label = { Text(cat.name, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color(0xFF0F172A),
                                containerColor = SlateCardDark,
                                labelColor = TextPrimaryDark
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Note Field
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Заметка (необязательно)", color = TextSecondaryDark) },
                    placeholder = { Text("Комментарий...", color = TextMutedDark) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SlateCardDark,
                        unfocusedContainerColor = SlateCardDark,
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = SlateBorderDark,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    maxLines = 2
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = ExpenseRed,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Отмена", color = TextSecondaryDark)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val cleanAmountStr = amountText.replace(",", ".").trim()
                            val amount = cleanAmountStr.toDoubleOrNull()
                            if (amount == null || amount <= 0) {
                                errorMessage = "Введите корректную сумму больше нуля"
                                return@Button
                            }
                            val cleanTitle = title.trim().ifEmpty {
                                if (type == TransactionType.INCOME) "Доход" else "Расход"
                            }
                            onSave(
                                cleanTitle,
                                amount,
                                type,
                                selectedCategoryId,
                                System.currentTimeMillis(),
                                note.trim()
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Сохранить",
                            color = Color(0xFF0F172A),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
