package com.example.data.repository

import com.example.BuildConfig
import com.example.data.remote.GeminiNetworkClient
import com.example.data.remote.dto.ContentDto
import com.example.data.remote.dto.GeminiRequestDto
import com.example.data.remote.dto.GenerationConfigDto
import com.example.data.remote.dto.PartDto
import com.example.domain.model.Budget
import com.example.domain.model.Category
import com.example.domain.model.FinancialSummary
import com.example.domain.model.ParsedTransactionDraft
import com.example.domain.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Locale
import java.util.regex.Pattern

class AiAssistantRepository {

    private val apiKey: String
        get() = BuildConfig.GEMINI_API_KEY

    private fun isKeyConfigured(): Boolean {
        return apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"
    }

    suspend fun getFinancialAdvice(
        userPrompt: String,
        summary: FinancialSummary,
        budgets: List<Budget>,
        categories: List<Category>
    ): String = withContext(Dispatchers.IO) {
        if (!isKeyConfigured()) {
            return@withContext generateLocalFinancialAdvice(userPrompt, summary, budgets)
        }

        try {
            val systemContext = buildString {
                appendLine("Ты — Дэвид (David), персональный ИИ-ассистент по управлению финансами и личным бюджетом.")
                appendLine("Отвечай на русском языке, вежливо, экспертно, конструктивно и конкретно.")
                appendLine("Актуальное финансовое положение пользователя:")
                appendLine("- Общий баланс: ${formatMoney(summary.totalBalance)} руб.")
                appendLine("- Доход за месяц: ${formatMoney(summary.monthlyIncome)} руб.")
                appendLine("- Расходы за месяц: ${formatMoney(summary.monthlyExpense)} руб.")
                appendLine("- Норма сбережений (Savings Rate): ${String.format(Locale.US, "%.1f", summary.savingsRate)}%")
                if (summary.topCategories.isNotEmpty()) {
                    appendLine("- Топ категорий расходов:")
                    summary.topCategories.take(4).forEach {
                        appendLine("  * ${it.categoryName}: ${formatMoney(it.amount)} руб. (${(it.percentage * 100).toInt()}%)")
                    }
                }
                val overBudgets = budgets.filter { it.isOverBudget }
                if (overBudgets.isNotEmpty()) {
                    appendLine("- ПРЕВЫШЕН БЮДЖЕТ в категориях:")
                    overBudgets.forEach {
                        appendLine("  * ${it.categoryName}: потрачено ${formatMoney(it.spentAmount)} из лимита ${formatMoney(it.monthlyLimit)} руб.")
                    }
                }
                appendLine("Давай практичные советы по методу 50/30/20, поиску неэффективных трат, формированию финансовой подушки безопасности и инвестициям.")
            }

            val request = GeminiRequestDto(
                contents = listOf(
                    ContentDto(
                        role = "user",
                        parts = listOf(PartDto(text = userPrompt))
                    )
                ),
                systemInstruction = ContentDto(
                    parts = listOf(PartDto(text = systemContext))
                ),
                generationConfig = GenerationConfigDto(
                    temperature = 0.7f,
                    maxOutputTokens = 1000
                )
            )

            val response = GeminiNetworkClient.service.generateContent(apiKey, request)
            val candidateText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!candidateText.isNullOrBlank()) {
                candidateText
            } else {
                "Не удалось сформировать ответ. Попробуйте переформулировать вопрос."
            }
        } catch (e: Exception) {
            "Ошибка при обращении к ИИ-ассистенту: ${e.localizedMessage ?: "проверьте интернет-соединение"}.\n\n" +
                    generateLocalFinancialAdvice(userPrompt, summary, budgets)
        }
    }

    suspend fun parseNaturalLanguageTransaction(
        input: String,
        categories: List<Category>
    ): ParsedTransactionDraft? = withContext(Dispatchers.IO) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return@withContext null

        if (!isKeyConfigured()) {
            return@withContext fallbackLocalParse(trimmed, categories)
        }

        try {
            val categoryNames = categories.joinToString(", ") { it.name }
            val prompt = """
                Распознай финансовую транзакцию из фразы пользователя на русском языке: "$trimmed"
                Доступные категории: $categoryNames.
                
                Верни СТРОГО JSON-объект без пояснений и без markdown-разметки:
                {
                  "title": "краткое описание транзакции",
                  "amount": 0.0,
                  "type": "EXPENSE" или "INCOME",
                  "categoryName": "название наиболее подходящей категории из списка",
                  "note": "дополнительная заметка или пустая строка"
                }
            """.trimIndent()

            val request = GeminiRequestDto(
                contents = listOf(
                    ContentDto(
                        parts = listOf(PartDto(text = prompt))
                    )
                ),
                generationConfig = GenerationConfigDto(
                    temperature = 0.1f,
                    responseMimeType = "application/json"
                )
            )

            val response = GeminiNetworkClient.service.generateContent(apiKey, request)
            val rawJson = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            val cleanJson = rawJson.replace("```json", "").replace("```", "").trim()
            val jsonObject = JSONObject(cleanJson)

            val title = jsonObject.optString("title", "Транзакция")
            val amount = jsonObject.optDouble("amount", 0.0)
            val typeStr = jsonObject.optString("type", "EXPENSE")
            val categoryName = jsonObject.optString("categoryName", "Прочее")
            val note = jsonObject.optString("note", "")

            if (amount > 0) {
                ParsedTransactionDraft(
                    title = title,
                    amount = amount,
                    type = if (typeStr.equals("INCOME", ignoreCase = true)) TransactionType.INCOME else TransactionType.EXPENSE,
                    categoryName = categoryName,
                    note = note
                )
            } else {
                fallbackLocalParse(trimmed, categories)
            }
        } catch (e: Exception) {
            fallbackLocalParse(trimmed, categories)
        }
    }

    private fun fallbackLocalParse(input: String, categories: List<Category>): ParsedTransactionDraft? {
        // Regex to extract numbers like 450, 1 500, 250.50
        val pattern = Pattern.compile("(\\d+[\\d\\s]*([.,]\\d+)?)")
        val matcher = pattern.matcher(input)
        if (!matcher.find()) return null

        val numStr = matcher.group(1)?.replace(" ", "")?.replace(",", ".") ?: return null
        val amount = numStr.toDoubleOrNull() ?: return null
        if (amount <= 0) return null

        val isIncome = input.contains("зарплат", ignoreCase = true) ||
                input.contains("аванс", ignoreCase = true) ||
                input.contains("доход", ignoreCase = true) ||
                input.contains("пришло", ignoreCase = true) ||
                input.contains("получил", ignoreCase = true)

        val lower = input.lowercase(Locale.ROOT)
        val matchedCategory = categories.firstOrNull { cat ->
            when (cat.name.lowercase(Locale.ROOT)) {
                "продукты" -> lower.contains("еда") || lower.contains("магазин") || lower.contains("супермаркет") || lower.contains("вкусвилл") || lower.contains("пятерочк")
                "кафе и рестораны" -> lower.contains("кофе") || lower.contains("кафе") || lower.contains("ресторан") || lower.contains("ланч") || lower.contains("обед")
                "транспорт" -> lower.contains("такси") || lower.contains("метро") || lower.contains("бензин") || lower.contains("автобус")
                "жилье и жкх" -> lower.contains("жкх") || lower.contains("аренд") || lower.contains("квартир") || lower.contains("свет")
                "здоровье" -> lower.contains("аптек") || lower.contains("лекарств") || lower.contains("врач")
                "развлечения" -> lower.contains("кино") || lower.contains("театр") || lower.contains("игр")
                else -> lower.contains(cat.name.lowercase(Locale.ROOT))
            }
        } ?: categories.firstOrNull { it.isDefault } ?: categories.firstOrNull()

        return ParsedTransactionDraft(
            title = input.take(30),
            amount = amount,
            type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE,
            categoryName = matchedCategory?.name ?: "Прочее",
            note = input
        )
    }

    private fun generateLocalFinancialAdvice(
        prompt: String,
        summary: FinancialSummary,
        budgets: List<Budget>
    ): String {
        return buildString {
            appendLine("Приветствую! Я Дэвид, ваш финансовый ассистент.")
            appendLine()
            appendLine("💡 Экспресс-анализ бюджета на текущий момент:")
            appendLine("• Текущий баланс: ${formatMoney(summary.totalBalance)} ₽")
            appendLine("• Доходы за месяц: ${formatMoney(summary.monthlyIncome)} ₽, Расходы: ${formatMoney(summary.monthlyExpense)} ₽")

            if (summary.monthlyIncome > 0) {
                val savings = summary.monthlyIncome - summary.monthlyExpense
                if (savings > 0) {
                    appendLine("• Ваша норма сбережений составляет ${String.format(Locale.US, "%.1f", summary.savingsRate)}%. Отличный показатель! Рекомендуется направлять не менее 20% в резервный фонд.")
                } else {
                    appendLine("• Внимание: расходы превышают доходы за текущий месяц на ${formatMoney(-savings)} ₽. Обратите внимание на оптимизацию переменных трат.")
                }
            }

            val overBudgets = budgets.filter { it.isOverBudget }
            if (overBudgets.isNotEmpty()) {
                appendLine()
                appendLine("⚠️ Превышение лимитов бюджета:")
                overBudgets.forEach { b ->
                    appendLine("  - ${b.categoryName}: превышен на ${formatMoney(b.spentAmount - b.monthlyLimit)} ₽")
                }
            }

            appendLine()
            appendLine("✨ Совет: вы можете вводить траты прямо в чат на естественном языке, например: «Купил кофе за 250 рублей» или «Аванс 45 000 руб». Я сразу предложу добавить их в учет!")
            if (!isKeyConfigured()) {
                appendLine()
                appendLine("ℹ️ (Для подключения расширенного ИИ на базе Gemini укажите GEMINI_API_KEY в панели Secrets AI Studio).")
            }
        }
    }

    private fun formatMoney(value: Double): String {
        return String.format(Locale.US, "%,.0f", value).replace(",", " ")
    }
}
