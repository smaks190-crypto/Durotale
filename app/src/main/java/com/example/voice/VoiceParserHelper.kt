package com.example.voice

import com.example.domain.model.Category
import com.example.domain.model.ParsedOperationItem
import com.example.domain.model.TransactionType
import java.util.Locale
import java.util.regex.Pattern

/**
 * Smart local speech and text parser for financial transactions in Russian.
 * Handles space-separated numbers (65 000), word numbers, Russian slang,
 * morphology/case variations, stem matching for categories, and text cleaning.
 */
object VoiceParserHelper {

    // Stems for income detection
    private val INCOME_ROOTS = listOf(
        "зарплат", "зп", "аванс", "преми", "доход", "прибыл", "вернул",
        "кэшбэк", "кешбэк", "кешбек", "подар", "перевел", "перевели", "перевод",
        "пришл", "зачисл", "стипенд", "дивиденд", "фриланс", "подработ",
        "шабашк", "в плюс", "плюс "
    )

    // Stems for expense categories
    private val GROCERY_ROOTS = listOf(
        "продукт", "пятероч", "пятёроч", "перекрест", "перекрёст", "магнит", "лент",
        "ашан", "вкусвил", "дикси", "самокат", "лавк", "хлеб", "молок", "мяс", "куриц",
        "сыр", "овощ", "фрукт", "еда", "еду", "супермаркет", "магазин", "рынок", "колбас",
        "яйц", "вод", "чай", "сладост"
    )

    private val CAFE_ROOTS = listOf(
        "кафе", "кофе", "круассан", "ресторан", "бар", "пицц", "суши", "ролл",
        "бургер", "шаурм", "шаверм", "обед", "ужин", "завтрак", "фастфуд", "додо",
        "вкусно и точ", "ростикс", "кфс", "пекарн", "столов"
    )

    private val TRANSPORT_ROOTS = listOf(
        "такси", "яндекс го", "убер", "метро", "автобус", "трамва", "троллейбус",
        "проезд", "подорожник", "тройк", "бензин", "заправ", "азс", "лукойл",
        "газпром", "парков", "каршеринг", "мойк", "авто", "машин", "билет"
    )

    private val HOUSING_ROOTS = listOf(
        "жкх", "квартплат", "коммунал", "аренд", "квартир", "свет", "электр",
        "интернет", "связ", "мтс", "билайн", "мегафон", "теле2", "йота", "домофон",
        "ремонт", "мебел", "бытов"
    )

    private val HEALTH_ROOTS = listOf(
        "аптек", "лекарств", "таблет", "врач", "клиник", "стоматолог", "зуб",
        "анализ", "больниц", "витамин"
    )

    private val ENTERTAINMENT_ROOTS = listOf(
        "кино", "театр", "концерт", "игр", "стим", "steam", "подписк",
        "яндекс плюс", "клуб", "боулинг", "кальян", "тусов", "развлеч", "книг"
    )

    private val SHOPPING_ROOTS = listOf(
        "одежд", "обув", "кроссов", "куртк", "джинс", "футбол", "стрижк", "барбер",
        "салон", "маникюр", "косметик", "вайлдберриз", "wildberries", "озон", "ozon"
    )

    // Stop words and filler verbs to trim from titles
    private val FILLER_WORDS = listOf(
        "добавь", "добавить", "запиши", "записать", "купил", "купила", "купили",
        "потратил", "потратила", "потратили", "получил", "получила", "получили",
        "пришла", "пришло", "пришли", "заплатил", "заплатила", "отдал", "отдала",
        "на", "за", "в", "по", "мне", "себе", "рублей", "рубля", "руб", "р",
        "в плюс", "в минус"
    )

    // Russian word numerals mapping
    private val BASIC_NUMERALS = mapOf(
        "ноль" to 0L,
        "один" to 1L, "одна" to 1L, "одну" to 1L, "одного" to 1L,
        "два" to 2L, "две" to 2L, "двух" to 2L,
        "три" to 3L, "трех" to 3L, "трёх" to 3L,
        "четыре" to 4L, "четырех" to 4L, "четырёх" to 4L,
        "пять" to 5L, "шесть" to 6L, "семь" to 7L, "восемь" to 8L, "девять" to 9L,
        "десять" to 10L, "одиннадцать" to 11L, "двенадцать" to 12L,
        "тринадцать" to 13L, "четырнадцать" to 14L, "пятнадцать" to 15L,
        "шестнадцать" to 16L, "семнадцать" to 17L, "восемнадцать" to 18L,
        "девятнадцать" to 19L,
        "двадцать" to 20L, "тридцать" to 30L, "сорок" to 40L,
        "пятьдесят" to 50L, "шестьдесят" to 60L, "семьдесят" to 70L,
        "восемьдесят" to 80L, "девяносто" to 90L,
        "сто" to 100L, "двести" to 200L, "триста" to 300L,
        "четыреста" to 400L, "пятьсот" to 500L, "шестьсот" to 600L,
        "семьсот" to 700L, "восемьсот" to 800L, "девятьсот" to 900L
    )

    /**
     * Parses spoken utterance containing one or multiple operations.
     * E.g.: "Такси 500 рублей, мясо 800, зарплата 65 000"
     */
    fun parseUtterance(
        text: String,
        categories: List<Category>
    ): List<ParsedOperationItem> {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return emptyList()

        // Step 1: Pre-process text (merge multidigit spaces, slang idioms, numerals to digits, multipliers)
        val normalized = normalizeFullText(trimmed)

        // Step 2: Split text into distinct operation segments
        val segments = splitIntoOperationSegments(normalized)

        val results = mutableListOf<ParsedOperationItem>()
        for (segment in segments) {
            val item = parseSingleSegment(segment, categories)
            if (item != null) {
                results.add(item)
            }
        }

        // If segmented pass found nothing, attempt parsing the full normalized text as a single operation
        if (results.isEmpty()) {
            val single = parseSingleSegment(normalized, categories)
            if (single != null) {
                results.add(single)
            }
        }

        return results
    }

    /**
     * Normalizes multidigit spaces, speech idioms, word numerals and multiplier suffixes.
     */
    fun normalizeFullText(input: String): String {
        var text = input.trim()

        // 1. Merge thousands with spaces: "65 000" -> "65000", "1 500" -> "1500"
        val spaceRegex = Regex("""(\d)\s+(?=\d{3}(?:\b|\D))""")
        while (spaceRegex.containsMatchIn(text)) {
            text = text.replace(spaceRegex, "$1")
        }

        // 2. Replace colloquial slang expressions using Unicode-aware letter boundaries
        val slangReplacements = listOf(
            """полторы\s+тысяч[\p{L}]*""" to "1500",
            """полтора\s+косар[\p{L}]*""" to "1500",
            """полторы\s+тыщи""" to "1500",
            """полтысяч[\p{L}]*""" to "500"
        )

        for ((pattern, rep) in slangReplacements) {
            val reg = Regex("""(?i)(?:^|(?<=[^\p{L}\p{N}]))$pattern(?:$|(?=[^\p{L}\p{N}]))""")
            text = text.replace(reg, rep)
        }

        // 3. Multiplier suffixes: "65 тысяч" -> 65000, "45к" -> 45000, "1.5к" -> 1500
        val thousandSuffix = Regex("""(?i)(?:^|(?<=[^\p{L}\p{N}]))(\d+(?:[.,]\d+)?)\s*(тыс(?:яч[\p{L}]*)?|к|k)(?:$|(?=[^\p{L}\p{N}]))""")
        text = thousandSuffix.replace(text) { match ->
            val num = match.groupValues[1].replace(",", ".").toDoubleOrNull()
            if (num != null) (num * 1000).toLong().toString() else match.value
        }

        val millionSuffix = Regex("""(?i)(?:^|(?<=[^\p{L}\p{N}]))(\d+(?:[.,]\d+)?)\s*(млн|миллион[\p{L}]*|лям[\p{L}]*)(?:$|(?=[^\p{L}\p{N}]))""")
        text = millionSuffix.replace(text) { match ->
            val num = match.groupValues[1].replace(",", ".").toDoubleOrNull()
            if (num != null) (num * 1000000).toLong().toString() else match.value
        }

        // 4. Convert compound word numerals ("пятьдесят пять тысяч" -> 55000, "двести пятьдесят" -> 250)
        text = convertWordNumeralsToDigits(text)

        return text
    }

    /**
     * Finds contiguous spans of Russian numeral words and converts them to digits.
     */
    private fun convertWordNumeralsToDigits(input: String): String {
        val words = input.split(Regex("\\s+"))
        val result = mutableListOf<String>()
        var i = 0

        while (i < words.size) {
            val lower = words[i].lowercase(Locale.ROOT).replace(Regex("[^а-яё]"), "")

            if (isNumeralWord(lower)) {
                // Collect sequence of numeral tokens
                var accumulator = 0L
                var currentNumber = 0L

                while (i < words.size) {
                    val w = words[i].lowercase(Locale.ROOT).replace(Regex("[^а-яё]"), "")
                    if (BASIC_NUMERALS.containsKey(w)) {
                        currentNumber += BASIC_NUMERALS[w]!!
                        i++
                    } else if (w.startsWith("тысяч") || w.startsWith("тыщ") || w.startsWith("косар") || w == "косарь") {
                        val factor = if (currentNumber == 0L) 1L else currentNumber
                        accumulator += factor * 1000L
                        currentNumber = 0L
                        i++
                    } else if (w.startsWith("миллион") || w.startsWith("лям")) {
                        val factor = if (currentNumber == 0L) 1L else currentNumber
                        accumulator += factor * 1000000L
                        currentNumber = 0L
                        i++
                    } else {
                        break
                    }
                }

                val finalNum = accumulator + currentNumber
                if (finalNum > 0) {
                    result.add(finalNum.toString())
                }
            } else {
                result.add(words[i])
                i++
            }
        }

        return result.joinToString(" ")
    }

    private fun isNumeralWord(w: String): Boolean {
        return BASIC_NUMERALS.containsKey(w) ||
                w.startsWith("тысяч") || w.startsWith("тыщ") ||
                w.startsWith("косар") || w == "косарь" ||
                w.startsWith("миллион") || w.startsWith("лям")
    }

    /**
     * Splits text into operation segments using punctuation, conjunctions,
     * and boundary transitions before new operation titles.
     */
    private fun splitIntoOperationSegments(text: String): List<String> {
        // Delimiters: commas, semicolons, newlines, and conjunction words surrounded by spaces
        val splitRegex = Regex(
            "[,;\\n]+|\\s+(?:и|плюс|а также|затем|потом|еще|ещё|дальше)\\s+",
            RegexOption.IGNORE_CASE
        )
        val initialParts = text.split(splitRegex).map { it.trim() }.filter { it.isNotBlank() }

        val finalSegments = mutableListOf<String>()
        val numberPattern = Pattern.compile("\\b(\\d+(?:[.,]\\d+)?)\\b")

        for (part in initialParts) {
            val matcher = numberPattern.matcher(part)
            val matches = mutableListOf<Pair<Int, Int>>()
            while (matcher.find()) {
                matches.add(Pair(matcher.start(), matcher.end()))
            }

            // If this part contains multiple numbers separated by words (e.g. "такси 500 кофе 250")
            if (matches.size > 1) {
                var prevIndex = 0
                for (k in 0 until matches.size - 1) {
                    val currentEnd = matches[k].second
                    val nextStart = matches[k + 1].first
                    val intermediate = part.substring(currentEnd, nextStart)

                    // Find split position (after optional currency/filler)
                    val currencyMatch = Regex("""(?i)\s*(руб(?:лей|ля|ль)?|р)\s*""").find(intermediate)
                    val splitOffset = if (currencyMatch != null) {
                        currentEnd + currencyMatch.range.last + 1
                    } else {
                        // Split right before the description of next item
                        currentEnd
                    }

                    val seg = part.substring(prevIndex, splitOffset).trim()
                    if (seg.isNotBlank()) finalSegments.add(seg)
                    prevIndex = splitOffset
                }
                val tail = part.substring(prevIndex).trim()
                if (tail.isNotBlank()) finalSegments.add(tail)
            } else {
                finalSegments.add(part)
            }
        }

        return finalSegments
    }

    /**
     * Parses a single operation segment into a ParsedOperationItem.
     */
    fun parseSingleSegment(
        segment: String,
        categories: List<Category>
    ): ParsedOperationItem? {
        val trimmed = segment.trim()
        if (trimmed.isBlank()) return null

        // Find numbers: 500, 1500, 65000, 2450.50
        val numberPattern = Pattern.compile("(\\d+([.,]\\d+)?)")
        val matcher = numberPattern.matcher(trimmed)
        if (!matcher.find()) return null

        val rawNum = matcher.group(1)?.replace(",", ".") ?: return null
        val amount = rawNum.toDoubleOrNull() ?: return null
        if (amount <= 0.0) return null

        // Remove number from segment to extract text
        val rawTitleText = trimmed.removeRange(matcher.start(), matcher.end())
        val lowerSegment = trimmed.lowercase(Locale.ROOT)

        // 1. Income detection by roots
        val isIncome = INCOME_ROOTS.any { lowerSegment.contains(it) }
        val type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE

        // 2. Category matching by stem analysis
        val matchedCategory = findCategoryByStems(lowerSegment, isIncome, categories)

        // 3. Clean and polish the title
        val cleanedTitle = cleanTitle(rawTitleText, isIncome, matchedCategory?.name)

        return ParsedOperationItem(
            title = cleanedTitle,
            amount = amount,
            type = type,
            categoryId = matchedCategory?.id,
            categoryName = matchedCategory?.name ?: if (isIncome) "Доходы" else "Прочее",
            categoryIcon = matchedCategory?.iconName ?: if (isIncome) "account_balance_wallet" else "more_horiz",
            categoryColorHex = matchedCategory?.colorHex ?: if (isIncome) "#10B981" else "#78909C",
            note = trimmed
        )
    }

    /**
     * Matches categories based on Russian word stems.
     */
    private fun findCategoryByStems(
        lower: String,
        isIncome: Boolean,
        categories: List<Category>
    ): Category? {
        if (isIncome) {
            val incomeCat = categories.firstOrNull {
                it.name.contains("Зарплат", ignoreCase = true) ||
                        it.name.contains("Доход", ignoreCase = true) ||
                        it.name.contains("Фриланс", ignoreCase = true)
            }
            if (incomeCat != null) return incomeCat
        }

        // Expense stem checks
        when {
            GROCERY_ROOTS.any { lower.contains(it) } -> {
                val cat = categories.firstOrNull { it.name.contains("Продукт", ignoreCase = true) }
                if (cat != null) return cat
            }

            CAFE_ROOTS.any { lower.contains(it) } -> {
                val cat = categories.firstOrNull {
                    it.name.contains("Кафе", ignoreCase = true) ||
                            it.name.contains("Ресторан", ignoreCase = true)
                }
                if (cat != null) return cat
            }

            TRANSPORT_ROOTS.any { lower.contains(it) } -> {
                val cat = categories.firstOrNull { it.name.contains("Транспорт", ignoreCase = true) }
                if (cat != null) return cat
            }

            HOUSING_ROOTS.any { lower.contains(it) } -> {
                val cat = categories.firstOrNull {
                    it.name.contains("Жилье", ignoreCase = true) ||
                            it.name.contains("ЖКХ", ignoreCase = true) ||
                            it.name.contains("Связь", ignoreCase = true)
                }
                if (cat != null) return cat
            }

            HEALTH_ROOTS.any { lower.contains(it) } -> {
                val cat = categories.firstOrNull { it.name.contains("Здоров", ignoreCase = true) }
                if (cat != null) return cat
            }

            ENTERTAINMENT_ROOTS.any { lower.contains(it) } -> {
                val cat = categories.firstOrNull { it.name.contains("Развлеч", ignoreCase = true) }
                if (cat != null) return cat
            }

            SHOPPING_ROOTS.any { lower.contains(it) } -> {
                val cat = categories.firstOrNull {
                    it.name.contains("Покупк", ignoreCase = true) ||
                            it.name.contains("Шопинг", ignoreCase = true) ||
                            it.name.contains("Одежд", ignoreCase = true)
                }
                if (cat != null) return cat
            }
        }

        // Direct database category name check
        for (cat in categories) {
            if (lower.contains(cat.name.lowercase(Locale.ROOT))) {
                return cat
            }
        }

        // Fallback
        return categories.firstOrNull { it.isDefault } ?: categories.firstOrNull()
    }

    /**
     * Cleans parasite verbs, prepositions, currency symbols, and normalizes cases.
     */
    fun cleanTitle(
        rawTitle: String,
        isIncome: Boolean,
        categoryName: String?
    ): String {
        var s = rawTitle
            .replace(Regex("(?i)[,:;!?.—\\-_\"'«»]"), " ")
            .trim()

        // Strip filler words from tokens
        var tokens = s.split(Regex("\\s+")).filter { it.isNotBlank() }

        // Remove filler verbs and prepositions from head
        while (tokens.isNotEmpty() && FILLER_WORDS.contains(tokens.first().lowercase(Locale.ROOT))) {
            tokens = tokens.drop(1)
        }

        // Remove filler verbs and currencies from tail
        while (tokens.isNotEmpty() && FILLER_WORDS.contains(tokens.last().lowercase(Locale.ROOT))) {
            tokens = tokens.dropLast(1)
        }

        val cleaned = tokens.joinToString(" ").trim()

        if (cleaned.isBlank()) {
            return categoryName ?: if (isIncome) "Доход" else "Расход"
        }

        // Common Russian morphology normalizations
        val normalized = when (cleaned.lowercase(Locale.ROOT)) {
            "зарплату", "зарплаты", "зарплатой" -> "Зарплата"
            "авансы" -> "Аванс"
            "премию" -> "Премия"
            "такси" -> "Такси"
            "кофе" -> "Кофе"
            "обед" -> "Обед"
            "ужин" -> "Ужин"
            "завтрак" -> "Завтрак"
            "продукты", "продуктов" -> "Продукты"
            "бензин" -> "Бензин"
            "метро" -> "Метро"
            "квартплату", "коммуналку" -> "ЖКХ"
            else -> capitalizeFirst(cleaned)
        }

        return normalized.take(40)
    }

    private fun capitalizeFirst(s: String): String {
        return s.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
        }
    }
}
