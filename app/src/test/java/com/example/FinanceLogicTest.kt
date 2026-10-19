package com.example

import com.example.domain.model.Budget
import com.example.domain.model.Category
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionType
import com.example.voice.VoiceParserHelper
import com.example.voice.VoiceTransactionParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.regex.Pattern

class FinanceLogicTest {

    private val sampleCategories = listOf(
        Category(id = 1, name = "Продукты", iconName = "shopping_cart", colorHex = "#FF7043"),
        Category(id = 2, name = "Кафе и рестораны", iconName = "restaurant", colorHex = "#FFA726"),
        Category(id = 3, name = "Транспорт", iconName = "directions_car", colorHex = "#42A5F5"),
        Category(id = 4, name = "Зарплата", iconName = "payments", colorHex = "#66BB6A")
    )

    @Test
    fun testVoiceTransactionParserMultiUtterance() {
        val utterance = "Такси 500 рублей, мясо 800, зарплата 50000"
        val parsed = VoiceTransactionParser.parseUtterance(utterance, sampleCategories)

        assertEquals(3, parsed.size)

        // 1. Taxi
        val taxi = parsed[0]
        assertEquals(500.0, taxi.amount, 0.01)
        assertEquals(TransactionType.EXPENSE, taxi.type)
        assertEquals("Транспорт", taxi.categoryName)

        // 2. Meat
        val meat = parsed[1]
        assertEquals(800.0, meat.amount, 0.01)
        assertEquals(TransactionType.EXPENSE, meat.type)
        assertEquals("Продукты", meat.categoryName)

        // 3. Salary
        val salary = parsed[2]
        assertEquals(50000.0, salary.amount, 0.01)
        assertEquals(TransactionType.INCOME, salary.type)
        assertEquals("Зарплата", salary.categoryName)
    }

    @Test
    fun testVoiceParserSpaceSeparatedThousands() {
        val utterance = "зарплата 65 000"
        val parsed = VoiceParserHelper.parseUtterance(utterance, sampleCategories)

        assertEquals(1, parsed.size)
        val item = parsed.first()
        assertEquals(65000.0, item.amount, 0.01)
        assertEquals(TransactionType.INCOME, item.type)
        assertEquals("Зарплата", item.title)
    }

    @Test
    fun testVoiceParserWordNumeralsAndSlang() {
        // Slang 1500
        val taxiUtterance = "такси полторы тысячи"
        val taxiParsed = VoiceParserHelper.parseUtterance(taxiUtterance, sampleCategories)
        assertEquals(1, taxiParsed.size)
        assertEquals(1500.0, taxiParsed.first().amount, 0.01)

        // Compound numeral 55000
        val salaryUtterance = "зарплата пятьдесят пять тысяч"
        val salaryParsed = VoiceParserHelper.parseUtterance(salaryUtterance, sampleCategories)
        assertEquals(1, salaryParsed.size)
        assertEquals(55000.0, salaryParsed.first().amount, 0.01)
        assertEquals(TransactionType.INCOME, salaryParsed.first().type)

        // Slang 500
        val coffeeUtterance = "кофе полтысячи"
        val coffeeParsed = VoiceParserHelper.parseUtterance(coffeeUtterance, sampleCategories)
        assertEquals(1, coffeeParsed.size)
        assertEquals(500.0, coffeeParsed.first().amount, 0.01)
    }

    @Test
    fun testVoiceParserMultipliers() {
        val utterance = "аванс 45к и премия 65 тысяч"
        val parsed = VoiceParserHelper.parseUtterance(utterance, sampleCategories)

        assertEquals(2, parsed.size)
        assertEquals(45000.0, parsed[0].amount, 0.01)
        assertEquals(TransactionType.INCOME, parsed[0].type)
        assertEquals(65000.0, parsed[1].amount, 0.01)
        assertEquals(TransactionType.INCOME, parsed[1].type)
    }

    @Test
    fun testVoiceParserTitleCleaning() {
        val utterance = "купил кофе за 250 рублей"
        val parsed = VoiceParserHelper.parseUtterance(utterance, sampleCategories)

        assertEquals(1, parsed.size)
        val item = parsed.first()
        assertEquals(250.0, item.amount, 0.01)
        assertEquals("Кофе", item.title)
        assertEquals("Кафе и рестораны", item.categoryName)
        assertEquals(TransactionType.EXPENSE, item.type)
    }

    @Test
    fun testVoiceParserWithoutCommas() {
        val utterance = "такси 500 кофе 200"
        val parsed = VoiceParserHelper.parseUtterance(utterance, sampleCategories)

        assertEquals(2, parsed.size)
        assertEquals(500.0, parsed[0].amount, 0.01)
        assertEquals(200.0, parsed[1].amount, 0.01)
    }

    @Test
    fun testBudgetProgressAndOverbudget() {
        val normalBudget = Budget(
            id = 1,
            categoryId = 1,
            categoryName = "Продукты",
            monthlyLimit = 20000.0,
            spentAmount = 10000.0
        )
        assertEquals(0.5f, normalBudget.progress, 0.01f)
        assertFalse(normalBudget.isOverBudget)
        assertEquals(10000.0, normalBudget.remainingAmount, 0.01)

        val exceededBudget = Budget(
            id = 2,
            categoryId = 2,
            categoryName = "Кафе",
            monthlyLimit = 5000.0,
            spentAmount = 7500.0
        )
        assertEquals(1.5f, exceededBudget.progress, 0.01f)
        assertTrue(exceededBudget.isOverBudget)
        assertEquals(0.0, exceededBudget.remainingAmount, 0.01)
    }

    @Test
    fun testSavingsRateCalculation() {
        val income = 100000.0
        val expense = 70000.0
        val savingsRate = if (income > 0) ((income - expense) / income * 100.0) else 0.0
        assertEquals(30.0, savingsRate, 0.01)

        val zeroIncome = 0.0
        val zeroRate = if (zeroIncome > 0) ((zeroIncome - 5000.0) / zeroIncome * 100.0) else 0.0
        assertEquals(0.0, zeroRate, 0.01)
    }

    @Test
    fun testAmountExtractionRegex() {
        val sample = "Купил продукты в магазине на 2 450.50 рублей"
        val pattern = Pattern.compile("(\\d+[\\d\\s]*([.,]\\d+)?)")
        val matcher = pattern.matcher(sample)
        assertTrue(matcher.find())
        val rawNum = matcher.group(1)?.replace(" ", "")?.replace(",", ".")
        val amount = rawNum?.toDoubleOrNull()
        assertEquals(2450.50, amount ?: 0.0, 0.01)
    }

    @Test
    fun testTransactionFiltering() {
        val tx1 = Transaction(
            id = 1,
            title = "Обед в кафе",
            amount = 450.0,
            type = TransactionType.EXPENSE,
            categoryId = 1,
            categoryName = "Кафе",
            categoryIcon = "restaurant",
            categoryColorHex = "#FFA726"
        )
        val tx2 = Transaction(
            id = 2,
            title = "Аванс зарплата",
            amount = 60000.0,
            type = TransactionType.INCOME,
            categoryId = 2,
            categoryName = "Зарплата",
            categoryIcon = "wallet",
            categoryColorHex = "#66BB6A"
        )
        val list = listOf(tx1, tx2)

        val expenseOnly = list.filter { it.type == TransactionType.EXPENSE }
        assertEquals(1, expenseOnly.size)
        assertEquals("Обед в кафе", expenseOnly.first().title)

        val queryMatch = list.filter { it.title.contains("зарплата", ignoreCase = true) }
        assertEquals(1, queryMatch.size)
        assertEquals(2L, queryMatch.first().id)
    }
}
