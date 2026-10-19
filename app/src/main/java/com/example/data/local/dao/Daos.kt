package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Update
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.domain.model.Budget
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

data class TransactionWithCategoryDto(
    val id: Long,
    val title: String,
    val amount: Double,
    val type: String,
    val categoryId: Long,
    val categoryName: String?,
    val categoryIcon: String?,
    val categoryColorHex: String?,
    val date: Long,
    val note: String
) {
    fun toDomain(): Transaction {
        val transType = try {
            TransactionType.valueOf(type)
        } catch (_: Exception) {
            TransactionType.EXPENSE
        }
        return Transaction(
            id = id,
            title = title,
            amount = amount,
            type = transType,
            categoryId = categoryId,
            categoryName = categoryName ?: "Другое",
            categoryIcon = categoryIcon ?: "more_horiz",
            categoryColorHex = categoryColorHex ?: "#78909C",
            date = date,
            note = note
        )
    }
}

data class BudgetWithCategoryDto(
    val id: Long,
    val categoryId: Long,
    val categoryName: String?,
    val monthlyLimit: Double
) {
    fun toDomain(spentAmount: Double = 0.0): Budget {
        return Budget(
            id = id,
            categoryId = categoryId,
            categoryName = categoryName ?: "Категория",
            monthlyLimit = monthlyLimit,
            spentAmount = spentAmount
        )
    }
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: Long): CategoryEntity?

    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaultCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCount(): Int
}

@Dao
interface TransactionDao {
    @Query("""
        SELECT 
            t.id, t.title, t.amount, t.type, t.categoryId, 
            c.name AS categoryName, c.iconName AS categoryIcon, c.colorHex AS categoryColorHex,
            t.date, t.note
        FROM transactions t
        LEFT JOIN categories c ON t.categoryId = c.id
        ORDER BY t.date DESC
    """)
    fun getAllTransactions(): Flow<List<TransactionWithCategoryDto>>

    @Query("""
        SELECT 
            t.id, t.title, t.amount, t.type, t.categoryId, 
            c.name AS categoryName, c.iconName AS categoryIcon, c.colorHex AS categoryColorHex,
            t.date, t.note
        FROM transactions t
        LEFT JOIN categories c ON t.categoryId = c.id
        WHERE t.date >= :startDate AND t.date <= :endDate
        ORDER BY t.date DESC
    """)
    fun getTransactionsInRange(startDate: Long, endDate: Long): Flow<List<TransactionWithCategoryDto>>

    @Query("""
        SELECT 
            t.id, t.title, t.amount, t.type, t.categoryId, 
            c.name AS categoryName, c.iconName AS categoryIcon, c.colorHex AS categoryColorHex,
            t.date, t.note
        FROM transactions t
        LEFT JOIN categories c ON t.categoryId = c.id
        WHERE t.date >= :startDate AND t.date <= :endDate
        ORDER BY t.date DESC
    """)
    suspend fun getTransactionsInRangeDirect(startDate: Long, endDate: Long): List<TransactionWithCategoryDto>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>): List<Long>

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE' AND date >= :startDate AND date <= :endDate")
    fun getTotalExpenseInRange(startDate: Long, endDate: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'INCOME' AND date >= :startDate AND date <= :endDate")
    fun getTotalIncomeInRange(startDate: Long, endDate: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE' AND categoryId = :categoryId AND date >= :startDate AND date <= :endDate")
    suspend fun getSpentForCategoryInRange(categoryId: Long, startDate: Long, endDate: Long): Double?
}

@Dao
interface BudgetDao {
    @Query("""
        SELECT b.id, b.categoryId, c.name AS categoryName, b.monthlyLimit
        FROM budgets b
        LEFT JOIN categories c ON b.categoryId = c.id
        ORDER BY b.monthlyLimit DESC
    """)
    fun getAllBudgets(): Flow<List<BudgetWithCategoryDto>>

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId LIMIT 1")
    suspend fun getBudgetByCategoryId(categoryId: Long): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBudget(budget: BudgetEntity): Long

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudgetById(id: Long)

    @Query("DELETE FROM budgets WHERE categoryId = :categoryId")
    suspend fun deleteBudgetByCategoryId(categoryId: Long)
}
