package com.example.voice

import com.example.domain.model.Category
import com.example.domain.model.ParsedOperationItem
import com.example.domain.model.TransactionType
import java.util.Locale
import java.util.regex.Pattern

object VoiceTransactionParser {

    /**
     * Parses a spoken utterance containing one or multiple operations.
     * E.g.: "Такси 500 рублей, мясо 800, зарплата 50000"
     */
    fun parseUtterance(
        text: String,
        categories: List<Category>
    ): List<ParsedOperationItem> {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return emptyList()

        // Split text into potential operation segments by comma, semicolon, "и", "плюс", "а также", "затем", "потом"
        val rawSegments = splitIntoSegments(trimmed)
        val results = mutableListOf<ParsedOperationItem>()

        for (segment in rawSegments) {
            val op = parseSingleSegment(segment.trim(), categories)
            if (op != null) {
                results.add(op)
            }
        }

        // If segmenting yielded nothing but there is a number in the entire text, try whole text
        if (results.isEmpty()) {
            val single = parseSingleSegment(trimmed, categories)
            if (single != null) {
                results.add(single)
            }
        }

        return results
    }

    private fun splitIntoSegments(text: String): List<String> {
        // Delimiters: commas, semicolons, newlines, and conjunction words surrounded by spaces
        val regex = Regex("[,;\\n]+|\\s+(?:и|плюс|а также|затем|потом|еще|ещё)\\s+", RegexOption.IGNORE_CASE)
        val parts = text.split(regex)
        return parts.filter { it.isNotBlank() }
    }

    fun parseSingleSegment(segment: String, categories: List<Category>): ParsedOperationItem? {
        val cleaned = segment.trim()
        if (cleaned.isBlank()) return null

        // Match numbers: e.g. 500, 1500, 25000, 45.50, 10 000
        val numberPattern = Pattern.compile("(\\d[\\d\\s]*([.,]\\d+)?)")
        val matcher = numberPattern.matcher(cleaned)
        if (!matcher.find()) return null

        val rawNum = matcher.group(1)?.replace(" ", "")?.replace(",", ".") ?: return null
        val amount = rawNum.toDoubleOrNull() ?: return null
        if (amount <= 0.0) return null

        // Remove amount and currency terms ("руб", "рублей", "р", "к", "тысяч") from title
        val textWithoutNumber = cleaned.removeRange(matcher.start(), matcher.end())
            .replace(Regex("(?i)\\b(руб(?:лей|ля|ль)?|р|тыс(?:яч|яча|ячи)?)\\b"), "")
            .replace(Regex("[,:;\\-—]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        val lower = cleaned.lowercase(Locale.ROOT)

        val isIncome = lower.contains("зарплат") ||
                lower.contains("аванс") ||
                lower.contains("доход") ||
                lower.contains("преми") ||
                lower.contains("пришло") ||
                lower.contains("перевод от") ||
                lower.contains("кэшбэк") ||
                lower.contains("кешбэк") ||
                lower.contains("получ")

        val type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE

        // Find best matching category
        val matchedCategory = findBestCategory(lower, isIncome, categories)

        val finalTitle = if (textWithoutNumber.isNotBlank()) {
            capitalizeFirst(textWithoutNumber.take(35))
        } else {
            matchedCategory?.name ?: if (isIncome) "Доход" else "Расход"
        }

        return ParsedOperationItem(
            title = finalTitle,
            amount = amount,
            type = type,
            categoryId = matchedCategory?.id,
            categoryName = matchedCategory?.name ?: if (isIncome) "Зарплата" else "Прочее",
            categoryIcon = matchedCategory?.iconName ?: if (isIncome) "payments" else "receipt",
            categoryColorHex = matchedCategory?.colorHex ?: if (isIncome) "#66BB6A" else "#78909C",
            note = cleaned
        )
    }

    private fun findBestCategory(
        lower: String,
        isIncome: Boolean,
        categories: List<Category>
    ): Category? {
        if (isIncome) {
            val incomeCat = categories.firstOrNull {
                it.name.contains("Зарплат", ignoreCase = true) ||
                        it.name.contains("Доход", ignoreCase = true) ||
                        it.name.contains("Инвестиц", ignoreCase = true)
            }
            if (incomeCat != null) return incomeCat
        }

        for (cat in categories) {
            val catLower = cat.name.lowercase(Locale.ROOT)
            when {
                catLower.contains("продукт") && (
                        lower.contains("мясо") || lower.contains("хлеб") || lower.contains("молок") ||
                                lower.contains("сыр") || lower.contains("еда") || lower.contains("супермаркет") ||
                                lower.contains("магазин") || lower.contains("пятерочк") || lower.contains("магнит") ||
                                lower.contains("вкусвилл") || lower.contains("перекресток") || lower.contains("рыб") ||
                                lower.contains("овощ") || lower.contains("фрукт") || lower.contains("колбас")
                        ) -> return cat

                catLower.contains("кафе") && (
                        lower.contains("кофе") || lower.contains("капучино") || lower.contains("латте") ||
                                lower.contains("пицц") || lower.contains("суши") || lower.contains("бургер") ||
                                lower.contains("мак") || lower.contains("кфс") || lower.contains("обед") ||
                                lower.contains("ужин") || lower.contains("ланч") || lower.contains("ресторан") ||
                                lower.contains("бар") || lower.contains("чай") || lower.contains("доставка")
                        ) -> return cat

                catLower.contains("транспорт") && (
                        lower.contains("такси") || lower.contains("метро") || lower.contains("бензин") ||
                                lower.contains("заправк") || lower.contains("автобус") || lower.contains("проезд") ||
                                lower.contains("каршеринг") || lower.contains("самокат") || lower.contains("парковк")
                        ) -> return cat

                catLower.contains("жилье") && (
                        lower.contains("жкх") || lower.contains("квартир") || lower.contains("аренд") ||
                                lower.contains("коммуналк") || lower.contains("свет") || lower.contains("отоплен") ||
                                lower.contains("интернет") || lower.contains("домофон")
                        ) -> return cat

                catLower.contains("здоров") && (
                        lower.contains("аптек") || lower.contains("лекарств") || lower.contains("таблетк") ||
                                lower.contains("врач") || lower.contains("клиник") || lower.contains("стоматолог") ||
                                lower.contains("анализ") || lower.contains("витамин")
                        ) -> return cat

                catLower.contains("развлеч") && (
                        lower.contains("кино") || lower.contains("театр") || lower.contains("билет") ||
                                lower.contains("игр") || lower.contains("концерт") || lower.contains("подписк") ||
                                lower.contains("steam") || lower.contains("playstation")
                        ) -> return cat

                catLower.contains("шопинг") || catLower.contains("покупк") -> {
                    if (lower.contains("одежд") || lower.contains("кроссовк") || lower.contains("обув") ||
                        lower.contains("куртк") || lower.contains("джинс") || lower.contains("озон") ||
                        lower.contains("ozon") || lower.contains("вайлдберриз") || lower.contains("wildberries")
                    ) {
                        return cat
                    }
                }
            }

            if (lower.contains(catLower)) {
                return cat
            }
        }

        return categories.firstOrNull { it.isDefault } ?: categories.firstOrNull()
    }

    private fun capitalizeFirst(s: String): String {
        return s.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    }
}
