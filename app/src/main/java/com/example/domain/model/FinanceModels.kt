package com.example.domain.model

enum class TransactionType {
    EXPENSE,
    INCOME
}

data class Category(
    val id: Long = 0,
    val name: String,
    val iconName: String,
    val colorHex: String,
    val isDefault: Boolean = false
)

data class Transaction(
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColorHex: String,
    val date: Long = System.currentTimeMillis(),
    val note: String = ""
)

data class Budget(
    val id: Long = 0,
    val categoryId: Long,
    val categoryName: String,
    val monthlyLimit: Double,
    val spentAmount: Double = 0.0
) {
    val progress: Float
        get() = if (monthlyLimit > 0) (spentAmount / monthlyLimit).toFloat().coerceIn(0f, 1.5f) else 0f

    val isOverBudget: Boolean
        get() = spentAmount > monthlyLimit

    val remainingAmount: Double
        get() = (monthlyLimit - spentAmount).coerceAtLeast(0.0)
}

data class CategoryExpenseShare(
    val categoryId: Long,
    val categoryName: String,
    val amount: Double,
    val percentage: Float,
    val colorHex: String
)

data class FinancialSummary(
    val totalBalance: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val monthlyExpense: Double = 0.0,
    val savingsRate: Double = 0.0,
    val topCategories: List<CategoryExpenseShare> = emptyList()
)

data class ParsedTransactionDraft(
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val categoryName: String,
    val note: String
)

data class ParsedOperationItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val type: TransactionType = TransactionType.EXPENSE,
    val categoryId: Long? = null,
    val categoryName: String = "Прочее",
    val categoryIcon: String = "more_horiz",
    val categoryColorHex: String = "#78909C",
    val note: String = ""
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val parsedTransaction: ParsedTransactionDraft? = null,
    val isLoading: Boolean = false
)
