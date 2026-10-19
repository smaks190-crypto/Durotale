package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.domain.model.Budget
import com.example.domain.model.Category
import com.example.domain.model.CategoryExpenseShare
import com.example.domain.model.FinancialSummary
import com.example.domain.model.ParsedOperationItem
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Calendar

class FinanceRepository(private val database: AppDatabase) {

    private val categoryDao = database.categoryDao()
    private val transactionDao = database.transactionDao()
    private val budgetDao = database.budgetDao()

    fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories().map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun getCategoryByName(name: String): Category? {
        return withContext(Dispatchers.IO) {
            categoryDao.getCategoryByName(name)?.toDomain()
        }
    }

    fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions().map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getBudgetsWithProgress(): Flow<List<Budget>> {
        val (monthStart, monthEnd) = getCurrentMonthRange()
        return combine(
            budgetDao.getAllBudgets(),
            transactionDao.getTransactionsInRange(monthStart, monthEnd)
        ) { budgets, monthTransactions ->
            budgets.map { budgetDto ->
                val spent = monthTransactions
                    .filter { it.categoryId == budgetDto.categoryId && it.type == "EXPENSE" }
                    .sumOf { it.amount }
                budgetDto.toDomain(spentAmount = spent)
            }
        }
    }

    fun getFinancialSummary(): Flow<FinancialSummary> {
        val (monthStart, monthEnd) = getCurrentMonthRange()
        return combine(
            transactionDao.getAllTransactions(),
            transactionDao.getTransactionsInRange(monthStart, monthEnd),
            categoryDao.getAllCategories()
        ) { allTransactions, monthTransactions, categories ->
            val totalIncome = allTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
            val totalExpense = allTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            val totalBalance = totalIncome - totalExpense

            val monthIncome = monthTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
            val monthExpense = monthTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }

            val savingsRate = if (monthIncome > 0) {
                ((monthIncome - monthExpense) / monthIncome * 100.0).coerceAtLeast(0.0)
            } else 0.0

            val catMap = categories.associateBy { it.id }
            val expensesByCategory = monthTransactions
                .filter { it.type == "EXPENSE" }
                .groupBy { it.categoryId }
                .map { (catId, txList) ->
                    val sum = txList.sumOf { it.amount }
                    val cat = catMap[catId]
                    val catName = cat?.name ?: "Другое"
                    val colorHex = cat?.colorHex ?: "#78909C"
                    val pct = if (monthExpense > 0) (sum / monthExpense).toFloat() else 0f
                    CategoryExpenseShare(
                        categoryId = catId,
                        categoryName = catName,
                        amount = sum,
                        percentage = pct,
                        colorHex = colorHex
                    )
                }
                .sortedByDescending { it.amount }

            FinancialSummary(
                totalBalance = totalBalance,
                monthlyIncome = monthIncome,
                monthlyExpense = monthExpense,
                savingsRate = savingsRate,
                topCategories = expensesByCategory
            )
        }
    }

    suspend fun addTransaction(
        title: String,
        amount: Double,
        type: TransactionType,
        categoryId: Long,
        date: Long = System.currentTimeMillis(),
        note: String = ""
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            if (amount <= 0.0) {
                return@withContext Result.failure(IllegalArgumentException("Сумма должна быть больше нуля"))
            }
            val sanitizedTitle = title.trim().ifEmpty {
                if (type == TransactionType.INCOME) "Доход" else "Расход"
            }
            val id = transactionDao.insertTransaction(
                TransactionEntity(
                    title = sanitizedTitle,
                    amount = amount,
                    type = type.name,
                    categoryId = categoryId,
                    date = date,
                    note = note.trim()
                )
            )
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addTransactionsBatch(items: List<ParsedOperationItem>): Result<Int> = withContext(Dispatchers.IO) {
        try {
            if (items.isEmpty()) return@withContext Result.success(0)
            val defaultCats = categoryDao.getCount()
            val entities = items.map { item ->
                val type = item.type
                val title = item.title.trim().ifEmpty {
                    if (type == TransactionType.INCOME) "Доход" else "Расход"
                }
                TransactionEntity(
                    title = title,
                    amount = item.amount.coerceAtLeast(0.01),
                    type = type.name,
                    categoryId = item.categoryId ?: 1L,
                    date = System.currentTimeMillis(),
                    note = item.note.trim()
                )
            }
            val ids = transactionDao.insertTransactions(entities)
            Result.success(ids.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTransaction(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            transactionDao.deleteTransactionById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setBudget(categoryId: Long, limit: Double): Result<Long> = withContext(Dispatchers.IO) {
        try {
            if (limit <= 0.0) {
                return@withContext Result.failure(IllegalArgumentException("Лимит должен быть больше нуля"))
            }
            val existing = budgetDao.getBudgetByCategoryId(categoryId)
            val budgetEntity = BudgetEntity(
                id = existing?.id ?: 0,
                categoryId = categoryId,
                monthlyLimit = limit
            )
            val id = budgetDao.insertOrUpdateBudget(budgetEntity)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteBudget(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            budgetDao.deleteBudgetById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addCategory(name: String, iconName: String, colorHex: String): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val trimmed = name.trim()
            if (trimmed.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Название категории не может быть пустым"))
            }
            val id = categoryDao.insertCategory(
                CategoryEntity(name = trimmed, iconName = iconName, colorHex = colorHex)
            )
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getCurrentMonthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.add(Calendar.MONTH, 1)
        cal.add(Calendar.MILLISECOND, -1)
        val end = cal.timeInMillis
        return Pair(start, end)
    }
}
