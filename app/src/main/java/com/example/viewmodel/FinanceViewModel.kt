package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.repository.AiAssistantRepository
import com.example.data.repository.FinanceRepository
import com.example.domain.model.Budget
import com.example.domain.model.Category
import com.example.domain.model.ChatMessage
import com.example.domain.model.FinancialSummary
import com.example.domain.model.ParsedOperationItem
import com.example.domain.model.ParsedTransactionDraft
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionType
import com.example.voice.VoiceTransactionParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class NavigationTab {
    DASHBOARD,
    TRANSACTIONS,
    BUDGETS,
    AI_ASSISTANT
}

data class FinanceUiState(
    val isLoading: Boolean = false,
    val summary: FinancialSummary = FinancialSummary(),
    val transactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val budgets: List<Budget> = emptyList(),
    val selectedTab: NavigationTab = NavigationTab.DASHBOARD,
    val searchQuery: String = "",
    val selectedTypeFilter: TransactionType? = null,
    val selectedCategoryIdFilter: Long? = null,
    val isAddTransactionOpen: Boolean = false,
    val draftForAddTransaction: ParsedTransactionDraft? = null,
    val isSetBudgetOpen: Boolean = false,
    val selectedCategoryForBudget: Category? = null,
    val chatMessages: List<ChatMessage> = listOf(
        ChatMessage(
            text = "Здравствуйте! Я Дэвид — ваш персональный финансовый ассистент на базе искусственного интеллекта.\n\nВы можете спросить меня об оптимизации расходов, аудите бюджета или просто написать: «Купил кроссовки за 4500 руб», и я помогу занести это в учет!",
            isFromUser = false
        )
    ),
    val isAiThinking: Boolean = false,
    val chatInputText: String = "",
    val pendingParsedTransaction: ParsedTransactionDraft? = null,
    val userNotification: String? = null,
    // Voice Input State
    val isVoiceInputActive: Boolean = false,
    val isVoiceListening: Boolean = false,
    val voiceRmsLevel: Float = 0f,
    val liveVoiceText: String = "",
    val parsedVoiceOperations: List<ParsedOperationItem> = emptyList()
) {
    val filteredTransactions: List<Transaction>
        get() {
            return transactions.filter { tx ->
                val matchesQuery = searchQuery.isBlank() ||
                        tx.title.contains(searchQuery, ignoreCase = true) ||
                        tx.note.contains(searchQuery, ignoreCase = true) ||
                        tx.categoryName.contains(searchQuery, ignoreCase = true)
                val matchesType = selectedTypeFilter == null || tx.type == selectedTypeFilter
                val matchesCategory = selectedCategoryIdFilter == null || tx.categoryId == selectedCategoryIdFilter
                matchesQuery && matchesType && matchesCategory
            }
        }
}

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val financeRepository = FinanceRepository(database)
    private val aiRepository = AiAssistantRepository()

    private val _uiState = MutableStateFlow(FinanceUiState(isLoading = true))
    val uiState: StateFlow<FinanceUiState> = _uiState.asStateFlow()

    init {
        observeDataStreams()
    }

    private fun observeDataStreams() {
        viewModelScope.launch {
            combine(
                financeRepository.getFinancialSummary(),
                financeRepository.getAllTransactions(),
                financeRepository.getAllCategories(),
                financeRepository.getBudgetsWithProgress()
            ) { summary, transactions, categories, budgets ->
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        summary = summary,
                        transactions = transactions,
                        categories = categories,
                        budgets = budgets
                    )
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = Unit
            )
        }
    }

    fun selectTab(tab: NavigationTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setTypeFilter(type: TransactionType?) {
        _uiState.update { it.copy(selectedTypeFilter = type) }
    }

    fun setCategoryFilter(categoryId: Long?) {
        _uiState.update { it.copy(selectedCategoryIdFilter = categoryId) }
    }

    fun openAddTransaction(draft: ParsedTransactionDraft? = null) {
        _uiState.update {
            it.copy(
                isAddTransactionOpen = true,
                draftForAddTransaction = draft
            )
        }
    }

    fun closeAddTransaction() {
        _uiState.update {
            it.copy(
                isAddTransactionOpen = false,
                draftForAddTransaction = null
            )
        }
    }

    fun openSetBudget(category: Category? = null) {
        _uiState.update {
            it.copy(
                isSetBudgetOpen = true,
                selectedCategoryForBudget = category
            )
        }
    }

    fun closeSetBudget() {
        _uiState.update {
            it.copy(
                isSetBudgetOpen = false,
                selectedCategoryForBudget = null
            )
        }
    }

    fun setChatInput(text: String) {
        _uiState.update { it.copy(chatInputText = text) }
    }

    fun dismissNotification() {
        _uiState.update { it.copy(userNotification = null) }
    }

    fun showNotification(message: String) {
        _uiState.update { it.copy(userNotification = message) }
    }

    // Voice Input State Management
    fun startVoiceSession() {
        _uiState.update {
            it.copy(
                isVoiceInputActive = true,
                isVoiceListening = true,
                voiceRmsLevel = 0f,
                liveVoiceText = "Слушаю вас...",
                parsedVoiceOperations = emptyList()
            )
        }
    }

    fun stopVoiceSession() {
        _uiState.update {
            it.copy(
                isVoiceInputActive = false,
                isVoiceListening = false,
                voiceRmsLevel = 0f,
                liveVoiceText = "",
                parsedVoiceOperations = emptyList()
            )
        }
    }

    fun updateVoiceRms(rms: Float) {
        _uiState.update { it.copy(voiceRmsLevel = rms) }
    }

    fun updateVoiceStatus(isListening: Boolean) {
        _uiState.update { it.copy(isVoiceListening = isListening) }
    }

    fun onVoicePartialText(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        _uiState.update { it.copy(liveVoiceText = trimmed) }

        // Fast streaming extraction of operations
        val categories = _uiState.value.categories
        val extracted = VoiceTransactionParser.parseUtterance(trimmed, categories)
        if (extracted.isNotEmpty()) {
            mergeVoiceOperations(extracted)
        }
    }

    fun onVoiceFinalText(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        val categories = _uiState.value.categories
        val extracted = VoiceTransactionParser.parseUtterance(trimmed, categories)
        if (extracted.isNotEmpty()) {
            mergeVoiceOperations(extracted)
        }
        _uiState.update { it.copy(liveVoiceText = "Продолжайте говорить...") }
    }

    private fun mergeVoiceOperations(newItems: List<ParsedOperationItem>) {
        _uiState.update { current ->
            val existing = current.parsedVoiceOperations.toMutableList()
            for (newItem in newItems) {
                // Check if already captured with similar amount and title to avoid duplicates
                val alreadyExists = existing.any {
                    it.amount == newItem.amount &&
                            (it.title.equals(newItem.title, ignoreCase = true) ||
                                    it.categoryName.equals(newItem.categoryName, ignoreCase = true))
                }
                if (!alreadyExists) {
                    existing.add(newItem)
                }
            }
            current.copy(parsedVoiceOperations = existing)
        }
    }

    fun removeParsedVoiceItem(id: String) {
        _uiState.update { current ->
            current.copy(parsedVoiceOperations = current.parsedVoiceOperations.filter { it.id != id })
        }
    }

    fun saveBatchVoiceOperations() {
        val operationsToSave = _uiState.value.parsedVoiceOperations
        if (operationsToSave.isEmpty()) {
            stopVoiceSession()
            return
        }

        viewModelScope.launch {
            val totalAmount = operationsToSave.sumOf { it.amount }
            val count = operationsToSave.size
            val result = financeRepository.addTransactionsBatch(operationsToSave)

            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isVoiceInputActive = false,
                        isVoiceListening = false,
                        parsedVoiceOperations = emptyList(),
                        userNotification = "Сохранено $count операций на сумму ${totalAmount.toInt()} ₽"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        userNotification = result.exceptionOrNull()?.message ?: "Ошибка сохранения операций"
                    )
                }
            }
        }
    }

    fun saveTransaction(
        title: String,
        amount: Double,
        type: TransactionType,
        categoryId: Long,
        date: Long,
        note: String
    ) {
        viewModelScope.launch {
            val result = financeRepository.addTransaction(title, amount, type, categoryId, date, note)
            if (result.isSuccess) {
                closeAddTransaction()
                _uiState.update { it.copy(userNotification = "Транзакция успешно сохранена") }
            } else {
                _uiState.update { it.copy(userNotification = result.exceptionOrNull()?.message ?: "Ошибка сохранения") }
            }
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            val result = financeRepository.deleteTransaction(id)
            if (result.isSuccess) {
                _uiState.update { it.copy(userNotification = "Транзакция удалена") }
            } else {
                _uiState.update { it.copy(userNotification = "Не удалось удалить запись") }
            }
        }
    }

    fun saveBudget(categoryId: Long, limit: Double) {
        viewModelScope.launch {
            val result = financeRepository.setBudget(categoryId, limit)
            if (result.isSuccess) {
                closeSetBudget()
                _uiState.update { it.copy(userNotification = "Бюджет обновлен") }
            } else {
                _uiState.update { it.copy(userNotification = result.exceptionOrNull()?.message ?: "Ошибка") }
            }
        }
    }

    fun deleteBudget(id: Long) {
        viewModelScope.launch {
            financeRepository.deleteBudget(id)
            _uiState.update { it.copy(userNotification = "Бюджет сброшен") }
        }
    }

    fun sendChatMessage(customPrompt: String? = null) {
        val messageText = (customPrompt ?: _uiState.value.chatInputText).trim()
        if (messageText.isBlank()) return

        val userMsg = ChatMessage(text = messageText, isFromUser = true)
        val currentState = _uiState.value

        _uiState.update {
            it.copy(
                chatMessages = it.chatMessages + userMsg,
                chatInputText = if (customPrompt == null) "" else it.chatInputText,
                isAiThinking = true
            )
        }

        viewModelScope.launch {
            // First: try NLP parsing for expense/income detection
            val parsedDraft = aiRepository.parseNaturalLanguageTransaction(messageText, currentState.categories)

            // Second: request conversational guidance from David
            val aiResponseText = aiRepository.getFinancialAdvice(
                userPrompt = messageText,
                summary = currentState.summary,
                budgets = currentState.budgets,
                categories = currentState.categories
            )

            val davidMsg = ChatMessage(
                text = aiResponseText,
                isFromUser = false,
                parsedTransaction = parsedDraft
            )

            _uiState.update {
                it.copy(
                    chatMessages = it.chatMessages + davidMsg,
                    isAiThinking = false,
                    pendingParsedTransaction = parsedDraft
                )
            }
        }
    }

    fun confirmParsedTransaction(draft: ParsedTransactionDraft) {
        viewModelScope.launch {
            val matchedCategory = _uiState.value.categories.firstOrNull {
                it.name.equals(draft.categoryName, ignoreCase = true)
            } ?: _uiState.value.categories.firstOrNull()

            val catId = matchedCategory?.id ?: 1L
            val result = financeRepository.addTransaction(
                title = draft.title,
                amount = draft.amount,
                type = draft.type,
                categoryId = catId,
                note = draft.note
            )

            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        pendingParsedTransaction = null,
                        userNotification = "Транзакция «${draft.title}» (${draft.amount.toInt()} ₽) добавлена в ${matchedCategory?.name}!"
                    )
                }
            }
        }
    }

    fun dismissParsedTransaction() {
        _uiState.update { it.copy(pendingParsedTransaction = null) }
    }
}
