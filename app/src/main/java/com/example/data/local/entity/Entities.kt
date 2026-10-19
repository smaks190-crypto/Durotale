package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.Category
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionType

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val iconName: String,
    val colorHex: String,
    val isDefault: Boolean = false
) {
    fun toDomain(): Category = Category(
        id = id,
        name = name,
        iconName = iconName,
        colorHex = colorHex,
        isDefault = isDefault
    )

    companion object {
        fun fromDomain(domain: Category): CategoryEntity = CategoryEntity(
            id = domain.id,
            name = domain.name,
            iconName = domain.iconName,
            colorHex = domain.colorHex,
            isDefault = domain.isDefault
        )
    }
}

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["date"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: String, // EXPENSE or INCOME
    val categoryId: Long,
    val date: Long = System.currentTimeMillis(),
    val note: String = ""
)

@Entity(
    tableName = "budgets",
    indices = [
        Index(value = ["categoryId"], unique = true)
    ]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long,
    val monthlyLimit: Double
)
