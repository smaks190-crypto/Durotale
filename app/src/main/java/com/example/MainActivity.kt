package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.VoiceInputMorphingFab
import com.example.ui.dialogs.AddTransactionDialog
import com.example.ui.dialogs.SetBudgetDialog
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.BudgetsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.FinanceDavidTheme
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.SlateSurfaceDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.viewmodel.FinanceUiState
import com.example.viewmodel.FinanceViewModel
import com.example.viewmodel.NavigationTab

class MainActivity : ComponentActivity() {

    private val viewModel: FinanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FinanceDavidTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                FinanceApp(
                    state = uiState,
                    viewModel = viewModel
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceApp(
    state: FinanceUiState,
    viewModel: FinanceViewModel
) {
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(enabled = state.isVoiceInputActive) {
        viewModel.stopVoiceSession()
    }

    LaunchedEffect(state.userNotification) {
        state.userNotification?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissNotification()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (state.selectedTab) {
                            NavigationTab.DASHBOARD -> "Главная"
                            NavigationTab.TRANSACTIONS -> "Операции"
                            NavigationTab.BUDGETS -> "Бюджет"
                            NavigationTab.AI_ASSISTANT -> "Ассистент Дэвид"
                        },
                        color = TextPrimaryDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    )
                },
                actions = {
                    if (state.selectedTab == NavigationTab.AI_ASSISTANT) {
                        Box(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .background(PurpleAccent.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Gemini Flash",
                                color = EmeraldLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SlateSurfaceDark
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = SlateSurfaceDark,
                contentColor = TextPrimaryDark,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = state.selectedTab == NavigationTab.DASHBOARD,
                    onClick = { viewModel.selectTab(NavigationTab.DASHBOARD) },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Дашборд", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF0F172A),
                        selectedTextColor = EmeraldLight,
                        indicatorColor = EmeraldPrimary,
                        unselectedIconColor = TextSecondaryDark,
                        unselectedTextColor = TextSecondaryDark
                    )
                )

                NavigationBarItem(
                    selected = state.selectedTab == NavigationTab.TRANSACTIONS,
                    onClick = { viewModel.selectTab(NavigationTab.TRANSACTIONS) },
                    icon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = "Transactions") },
                    label = { Text("Операции", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF0F172A),
                        selectedTextColor = EmeraldLight,
                        indicatorColor = EmeraldPrimary,
                        unselectedIconColor = TextSecondaryDark,
                        unselectedTextColor = TextSecondaryDark
                    )
                )

                NavigationBarItem(
                    selected = state.selectedTab == NavigationTab.BUDGETS,
                    onClick = { viewModel.selectTab(NavigationTab.BUDGETS) },
                    icon = { Icon(Icons.Default.PieChart, contentDescription = "Budgets") },
                    label = { Text("Бюджет", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF0F172A),
                        selectedTextColor = EmeraldLight,
                        indicatorColor = EmeraldPrimary,
                        unselectedIconColor = TextSecondaryDark,
                        unselectedTextColor = TextSecondaryDark
                    )
                )

                NavigationBarItem(
                    selected = state.selectedTab == NavigationTab.AI_ASSISTANT,
                    onClick = { viewModel.selectTab(NavigationTab.AI_ASSISTANT) },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "David AI") },
                    label = { Text("Дэвид ИИ", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF0F172A),
                        selectedTextColor = EmeraldLight,
                        indicatorColor = EmeraldPrimary,
                        unselectedIconColor = TextSecondaryDark,
                        unselectedTextColor = TextSecondaryDark
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (state.selectedTab) {
                NavigationTab.DASHBOARD -> DashboardScreen(
                    state = state,
                    onNavigateToTab = { viewModel.selectTab(it) },
                    onOpenAddTransaction = { viewModel.openAddTransaction() },
                    onAskDavid = {
                        viewModel.selectTab(NavigationTab.AI_ASSISTANT)
                        viewModel.sendChatMessage(it)
                    }
                )
                NavigationTab.TRANSACTIONS -> TransactionsScreen(
                    state = state,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onTypeFilterChange = { viewModel.setTypeFilter(it) },
                    onCategoryFilterChange = { viewModel.setCategoryFilter(it) },
                    onDeleteTransaction = { viewModel.deleteTransaction(it) }
                )
                NavigationTab.BUDGETS -> BudgetsScreen(
                    state = state,
                    onOpenSetBudget = { viewModel.openSetBudget(it) },
                    onDeleteBudget = { viewModel.deleteBudget(it) }
                )
                NavigationTab.AI_ASSISTANT -> AiAssistantScreen(
                    state = state,
                    onInputTextChange = { viewModel.setChatInput(it) },
                    onSendMessage = { viewModel.sendChatMessage(it) },
                    onConfirmParsedTransaction = { viewModel.confirmParsedTransaction(it) },
                    onDismissParsedTransaction = { viewModel.dismissParsedTransaction() }
                )
            }

            // Morphing Floating Action Button / Voice Input Capsule
            val showMorphingFab = state.isVoiceInputActive ||
                    state.selectedTab == NavigationTab.DASHBOARD ||
                    state.selectedTab == NavigationTab.TRANSACTIONS

            if (showMorphingFab) {
                VoiceInputMorphingFab(
                    isVoiceActive = state.isVoiceInputActive,
                    isListening = state.isVoiceListening,
                    rmsLevel = state.voiceRmsLevel,
                    liveText = state.liveVoiceText,
                    parsedOperations = state.parsedVoiceOperations,
                    onStartVoice = { viewModel.startVoiceSession() },
                    onStopVoice = { viewModel.stopVoiceSession() },
                    onOpenManualAdd = { viewModel.openAddTransaction() },
                    onRmsChanged = { viewModel.updateVoiceRms(it) },
                    onStatusChanged = { viewModel.updateVoiceStatus(it) },
                    onPartialText = { viewModel.onVoicePartialText(it) },
                    onFinalText = { viewModel.onVoiceFinalText(it) },
                    onRemoveItem = { viewModel.removeParsedVoiceItem(it) },
                    onSaveBatch = { viewModel.saveBatchVoiceOperations() },
                    onError = { viewModel.showNotification(it) },
                    modifier = Modifier.align(
                        if (state.isVoiceInputActive) Alignment.BottomCenter else Alignment.BottomEnd
                    )
                )
            }
        }
    }

    // Add Transaction Dialog
    if (state.isAddTransactionOpen) {
        AddTransactionDialog(
            categories = state.categories,
            initialDraft = state.draftForAddTransaction,
            onDismiss = { viewModel.closeAddTransaction() },
            onSave = { title, amount, type, categoryId, date, note ->
                viewModel.saveTransaction(title, amount, type, categoryId, date, note)
            }
        )
    }

    // Set Budget Dialog
    if (state.isSetBudgetOpen) {
        SetBudgetDialog(
            categories = state.categories,
            initialCategory = state.selectedCategoryForBudget,
            onDismiss = { viewModel.closeSetBudget() },
            onSave = { categoryId, limit ->
                viewModel.saveBudget(categoryId, limit)
            }
        )
    }
}
