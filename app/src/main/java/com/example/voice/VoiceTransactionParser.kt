package com.example.voice

import com.example.domain.model.Category
import com.example.domain.model.ParsedOperationItem

/**
 * High-performance parser facade delegating to [VoiceParserHelper].
 */
object VoiceTransactionParser {

    fun parseUtterance(
        text: String,
        categories: List<Category>
    ): List<ParsedOperationItem> {
        return VoiceParserHelper.parseUtterance(text, categories)
    }

    fun parseSingleSegment(
        segment: String,
        categories: List<Category>
    ): ParsedOperationItem? {
        return VoiceParserHelper.parseSingleSegment(segment, categories)
    }
}
