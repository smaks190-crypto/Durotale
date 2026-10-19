package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.BudgetDao
import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "david_finance.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Seed default categories and initial sample budgets in background
                            CoroutineScope(Dispatchers.IO).launch {
                                populateDefaultData(getInstance(context))
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun populateDefaultData(database: AppDatabase) {
            val defaultCategories = listOf(
                CategoryEntity(name = "Продукты", iconName = "shopping_cart", colorHex = "#FF7043", isDefault = true),
                CategoryEntity(name = "Транспорт", iconName = "directions_car", colorHex = "#42A5F5", isDefault = true),
                CategoryEntity(name = "Кафе и рестораны", iconName = "restaurant", colorHex = "#FFA726", isDefault = true),
                CategoryEntity(name = "Жилье и ЖКХ", iconName = "home", colorHex = "#AB47BC", isDefault = true),
                CategoryEntity(name = "Здоровье", iconName = "favorite", colorHex = "#26A69A", isDefault = true),
                CategoryEntity(name = "Развлечения", iconName = "movie", colorHex = "#EC407A", isDefault = true),
                CategoryEntity(name = "Покупки", iconName = "shopping_bag", colorHex = "#7E57C2", isDefault = true),
                CategoryEntity(name = "Зарплата", iconName = "account_balance_wallet", colorHex = "#66BB6A", isDefault = true),
                CategoryEntity(name = "Фриланс", iconName = "laptop", colorHex = "#29B6F6", isDefault = true),
                CategoryEntity(name = "Прочее", iconName = "more_horiz", colorHex = "#78909C", isDefault = true)
            )
            database.categoryDao().insertDefaultCategories(defaultCategories)

            // Seed initial sensible budgets
            val foodCat = database.categoryDao().getCategoryByName("Продукты")
            if (foodCat != null) {
                database.budgetDao().insertOrUpdateBudget(
                    BudgetEntity(categoryId = foodCat.id, monthlyLimit = 25000.0)
                )
            }
            val cafeCat = database.categoryDao().getCategoryByName("Кафе и рестораны")
            if (cafeCat != null) {
                database.budgetDao().insertOrUpdateBudget(
                    BudgetEntity(categoryId = cafeCat.id, monthlyLimit = 10000.0)
                )
            }
            val transportCat = database.categoryDao().getCategoryByName("Транспорт")
            if (transportCat != null) {
                database.budgetDao().insertOrUpdateBudget(
                    BudgetEntity(categoryId = transportCat.id, monthlyLimit = 6000.0)
                )
            }

            // Seed initial demo transactions so the app opens with rich, realistic data right away
            val now = System.currentTimeMillis()
            val dayMillis = 24L * 60 * 60 * 1000

            val salaryCat = database.categoryDao().getCategoryByName("Зарплата")
            if (salaryCat != null) {
                database.transactionDao().insertTransaction(
                    TransactionEntity(
                        title = "Аванс за текущий месяц",
                        amount = 95000.0,
                        type = "INCOME",
                        categoryId = salaryCat.id,
                        date = now - 5 * dayMillis,
                        note = "Основная работа"
                    )
                )
            }
            if (foodCat != null) {
                database.transactionDao().insertTransaction(
                    TransactionEntity(
                        title = "ВкусВилл — недельные продукты",
                        amount = 4320.0,
                        type = "EXPENSE",
                        categoryId = foodCat.id,
                        date = now - 1 * dayMillis,
                        note = "Овощи, фрукты, рыба"
                    )
                )
                database.transactionDao().insertTransaction(
                    TransactionEntity(
                        title = "Перекресток у дома",
                        amount = 1750.0,
                        type = "EXPENSE",
                        categoryId = foodCat.id,
                        date = now - 3 * dayMillis,
                        note = "Молочные продукты и хлеб"
                    )
                )
            }
            if (transportCat != null) {
                database.transactionDao().insertTransaction(
                    TransactionEntity(
                        title = "Пополнение карты Тройка",
                        amount = 1200.0,
                        type = "EXPENSE",
                        categoryId = transportCat.id,
                        date = now - 4 * dayMillis,
                        note = "Метро"
                    )
                )
            }
            if (cafeCat != null) {
                database.transactionDao().insertTransaction(
                    TransactionEntity(
                        title = "Обед в кафе с коллегами",
                        amount = 890.0,
                        type = "EXPENSE",
                        categoryId = cafeCat.id,
                        date = now - 2 * dayMillis,
                        note = "Бизнес-ланч"
                    )
                )
            }
        }
    }
}
