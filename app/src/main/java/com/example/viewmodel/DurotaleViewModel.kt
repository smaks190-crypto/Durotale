package com.example.viewmodel

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.RetroSoundEngine
import com.example.model.BattlePhase
import com.example.model.BattleState
import com.example.model.Bullet
import com.example.model.DialogueLine
import com.example.model.DoorAction
import com.example.model.DustParticle
import com.example.model.GameChoice
import com.example.model.GameMode
import com.example.model.OrderlyState
import com.example.model.PlayerState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class UiState(
    val mode: GameMode = GameMode.CUTSCENE_DRAG,
    val playerHP: Int = 20,
    val maxHP: Int = 20,
    val endingsUnlocked: Set<String> = emptySet(), // "escape", "vegetable"
    val isMuted: Boolean = false,
    val isDruggedMode: Boolean = false,
    val screenShakeTick: Int = 0,
    val screenShakeIntensity: Float = 0f,
    val doorActionHistory: List<DoorAction> = emptyList(),
    val doorOpen: Boolean = true, // Open initially during drag cutscene!
    val orderlyAtWindow: Boolean = false,
    val orderlyInside: Boolean = false,
    val isVegetable: Boolean = false,
    val currentSpeaker: String = "",
    val currentPortrait: String? = null,
    val displayedDialogueText: String = "",
    val isTyping: Boolean = false,
    val canAdvanceDialogue: Boolean = false,
    val choices: List<GameChoice> = emptyList(),
    val selectedChoiceIndex: Int = 0,
    val promptText: String = "",
    val interactPrompt: String? = null,
    val player: PlayerState = PlayerState(),
    val orderly: OrderlyState = OrderlyState(),
    val orderly2: OrderlyState = OrderlyState(x = 345f, y = 330f, hasCoat = true),
    val orderly2Visible: Boolean = false,
    val battle: BattleState = BattleState(),
    val dustParticles: List<DustParticle> = emptyList(),
    val tick: Long = 0L,
    val showHintsModal: Boolean = false
)

class DurotaleViewModel(application: Application) : AndroidViewModel(application) {
    val soundEngine = RetroSoundEngine(application)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val secretCombo = listOf(
        DoorAction.BANG,
        DoorAction.SCREAM,
        DoorAction.BANG,
        DoorAction.BANG,
        DoorAction.SCREAM
    )

    private var typingJob: Job? = null
    private var gameLoopJob: Job? = null

    private var dialogueQueue: MutableList<DialogueLine> = mutableListOf()
    private var currentDialogueLine: DialogueLine? = null
    private var onDialogueFinished: (() -> Unit)? = null

    // Scripted Orderly Entry cutscene step counter
    private var orderlyEnterStep = 0
    private var orderlyEnterTimer = 0

    // Input directional flags
    var inputUp = false
    var inputDown = false
    var inputLeft = false
    var inputRight = false

    init {
        startGameLoop()
        startIntroCutscene()
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            while (isActive) {
                delay(16) // ~60fps
                updateTick()
            }
        }
    }

    fun toggleMute() {
        soundEngine.isMuted = !soundEngine.isMuted
        _uiState.update { it.copy(isMuted = soundEngine.isMuted) }
    }

    fun toggleHintsModal() {
        soundEngine.sfxMenuSelect()
        _uiState.update { it.copy(showHintsModal = !it.showHintsModal) }
    }

    fun triggerScreenShake(duration: Int = 18, intensity: Float = 14f) {
        _uiState.update {
            it.copy(screenShakeTick = duration, screenShakeIntensity = intensity)
        }
    }

    private fun spawnDoorDustParticles() {
        val particles = mutableListOf<DustParticle>()
        val colors = listOf(Color.White, Color(0xFFFFD54F), Color(0xFFB0BEC5), Color(0xFFFFCC80))
        for (i in 0 until 24) {
            val px = 320f + Random.nextInt(-35, 36).toFloat()
            val py = 308f + Random.nextInt(-4, 5).toFloat()
            val vx = Random.nextDouble(-3.8, 3.8).toFloat()
            val vy = Random.nextDouble(-4.5, -1.0).toFloat()
            particles.add(
                DustParticle(
                    x = px,
                    y = py,
                    vx = vx,
                    vy = vy,
                    alpha = 1f,
                    size = Random.nextInt(2, 5).toFloat(),
                    color = colors[Random.nextInt(colors.size)]
                )
            )
        }
        _uiState.update { it.copy(dustParticles = it.dustParticles + particles) }
    }

    /* ==========================================
       INTRO CUTSCENE & RESTART
       ========================================== */
    fun restartFullGame() {
        typingJob?.cancel()
        dialogueQueue.clear()
        onDialogueFinished = null
        orderlyEnterStep = 0

        _uiState.update {
            it.copy(
                mode = GameMode.CUTSCENE_DRAG,
                playerHP = 20,
                isDruggedMode = false,
                doorActionHistory = emptyList(),
                doorOpen = true, // Door is OPEN during drag
                orderlyAtWindow = false,
                orderlyInside = false,
                isVegetable = false,
                choices = emptyList(),
                player = PlayerState(x = 306f, y = 375f, dir = "down", hasCoat = false),
                orderly = OrderlyState(x = 272f, y = 365f, state = "dragging", hasCoat = true),
                orderly2 = OrderlyState(x = 338f, y = 365f, hasCoat = true),
                orderly2Visible = true,
                battle = BattleState()
            )
        }
        startIntroCutscene()
    }

    fun skipToCell() {
        typingJob?.cancel()
        dialogueQueue.clear()
        onDialogueFinished = null
        orderlyEnterStep = 0

        _uiState.update {
            it.copy(
                mode = GameMode.EXPLORE,
                playerHP = 20,
                isDruggedMode = false,
                doorActionHistory = emptyList(),
                doorOpen = false,
                orderlyAtWindow = false,
                orderlyInside = false,
                isVegetable = false,
                choices = emptyList(),
                currentSpeaker = "",
                currentPortrait = null,
                displayedDialogueText = "* Вы заперты в мягкой одиночной камере №6. Подойдите к двери внизу, чтобы действовать.",
                canAdvanceDialogue = false,
                player = PlayerState(x = 306f, y = 215f, dir = "down", hasCoat = false),
                orderly = OrderlyState(x = 304f, y = 340f, state = "outside", hasCoat = true),
                orderly2Visible = false,
                battle = BattleState()
            )
        }
    }

    private fun startIntroCutscene() {
        // Door is open while orderlies drag player inside
        _uiState.update { it.copy(doorOpen = true) }

        startDialogue(
            listOf(
                DialogueLine(
                    name = "САНИТАР МИХАЛЫЧ",
                    portrait = "orderly_angry",
                    voice = "orderly",
                    text = "* Держи его крепче, Петрович! Ишь как дёргается, опять про свои «игры и коды» кричит!"
                ),
                DialogueLine(
                    name = "ПАЦИЕНТ",
                    portrait = "player_shock",
                    voice = "player",
                    text = "* Отпустите!! Я вообще-то нормальный! Меня сюда по ошибке привезли!"
                )
            )
        ) {
            startDialogue(
                listOf(
                    DialogueLine(
                        name = "САНИТАР МИХАЛЫЧ",
                        portrait = "orderly_smirk",
                        voice = "orderly",
                        text = "* Ага-ага, у нас тут каждый второй — Наполеон, а каждый первый — «абсолютно здоров». Посиди-ка в мягкой одиночке!"
                    ),
                    DialogueLine(
                        name = "",
                        portrait = null,
                        voice = "narrator",
                        text = "* ЛЯЗГ! Стальная дверь захлопнулась на тяжелый засов. Вокруг вас только мягкие белые стены."
                    )
                )
            ) {
                // When slammed shut: sound, screen shake, dust sparks, door closed!
                soundEngine.sfxBangDoor()
                triggerScreenShake(duration = 26, intensity = 22f)
                spawnDoorDustParticles()

                _uiState.update {
                    it.copy(
                        doorOpen = false,
                        orderly2Visible = false,
                        player = it.player.copy(x = 306f, y = 200f, dir = "down"),
                        orderly = it.orderly.copy(y = 340f, state = "outside")
                    )
                }
                skipToCell()
            }
        }
    }

    /* ==========================================
       DIALOGUE & CHOICE ENGINE
       ========================================== */
    fun startDialogue(lines: List<DialogueLine>, onFinished: (() -> Unit)? = null) {
        typingJob?.cancel()
        dialogueQueue = lines.toMutableList()
        onDialogueFinished = onFinished
        _uiState.update {
            it.copy(
                mode = GameMode.DIALOGUE,
                choices = emptyList(),
                canAdvanceDialogue = true
            )
        }
        nextDialogueLine()
    }

    private fun nextDialogueLine() {
        typingJob?.cancel()
        if (dialogueQueue.isEmpty()) {
            val callback = onDialogueFinished
            onDialogueFinished = null
            if (callback != null) {
                callback()
            } else if (_uiState.value.mode == GameMode.DIALOGUE) {
                _uiState.update {
                    it.copy(
                        mode = if (it.orderlyInside) GameMode.ORDERLY_SEARCH else GameMode.EXPLORE,
                        currentSpeaker = "",
                        currentPortrait = null,
                        canAdvanceDialogue = false,
                        displayedDialogueText = if (it.orderlyInside) {
                            "* Санитар Михалыч ползает по камере и ищет телефон! Дверь в коридор ОТКРЫТА!"
                        } else {
                            "* Вы заперты в мягкой одиночной камере №6. Подойдите к двери внизу, чтобы действовать."
                        }
                    )
                }
            }
            return
        }

        currentDialogueLine = dialogueQueue.removeAt(0)
        val line = currentDialogueLine ?: return

        _uiState.update {
            it.copy(
                currentSpeaker = line.name,
                currentPortrait = line.portrait,
                displayedDialogueText = "",
                isTyping = true
            )
        }

        val fullText = line.text
        val speedMs = if (line.slow) 70L else 24L

        typingJob = viewModelScope.launch {
            var index = 0
            while (index < fullText.length && isActive) {
                index++
                val sub = fullText.substring(0, index)
                _uiState.update { it.copy(displayedDialogueText = sub) }
                if (index % 2 == 0 && fullText[index - 1] != ' ') {
                    soundEngine.playVoiceBlip(line.voice)
                }
                delay(speedMs)
            }
            _uiState.update { it.copy(isTyping = false) }
        }
    }

    fun showChoices(prompt: String, portrait: String?, choices: List<GameChoice>) {
        typingJob?.cancel()
        _uiState.update {
            it.copy(
                mode = GameMode.CHOICE,
                promptText = prompt,
                currentSpeaker = "",
                currentPortrait = portrait,
                displayedDialogueText = prompt,
                isTyping = false,
                choices = choices,
                selectedChoiceIndex = 0,
                canAdvanceDialogue = false
            )
        }
    }

    fun moveChoiceCursor(delta: Int) {
        val count = _uiState.value.choices.size
        if (count == 0) return
        soundEngine.sfxMenuMove()
        _uiState.update {
            val next = (it.selectedChoiceIndex + delta + count) % count
            it.copy(selectedChoiceIndex = next)
        }
    }

    fun selectChoiceByIndex(index: Int) {
        _uiState.update { it.copy(selectedChoiceIndex = index.coerceIn(0, it.choices.size - 1)) }
        handleConfirm()
    }

    /* ==========================================
       ACTION / CONFIRM / CANCEL BUTTONS
       ========================================== */
    fun handleConfirm() {
        val state = _uiState.value

        if (state.mode == GameMode.BATTLE && state.battle.phase == BattlePhase.ATTACK_BAR) {
            confirmAttackBarHit()
            return
        }

        if (state.isTyping && currentDialogueLine != null) {
            // Instant text display
            typingJob?.cancel()
            _uiState.update {
                it.copy(
                    displayedDialogueText = currentDialogueLine?.text.orEmpty(),
                    isTyping = false
                )
            }
            return
        }

        if (state.mode == GameMode.CHOICE && state.choices.isNotEmpty()) {
            soundEngine.sfxMenuSelect()
            val chosen = state.choices[state.selectedChoiceIndex]
            onChoiceExecuted(chosen.id)
            return
        }

        if (state.mode == GameMode.CUTSCENE_DRAG || state.mode == GameMode.DIALOGUE || dialogueQueue.isNotEmpty()) {
            nextDialogueLine()
            return
        }

        if (state.mode == GameMode.EXPLORE || state.mode == GameMode.ORDERLY_SEARCH) {
            tryInteractInRoom()
        }
    }

    fun handleCancel() {
        if (_uiState.value.isTyping && currentDialogueLine != null) {
            typingJob?.cancel()
            _uiState.update {
                it.copy(
                    displayedDialogueText = currentDialogueLine?.text.orEmpty(),
                    isTyping = false
                )
            }
        }
    }

    private fun onChoiceExecuted(choiceId: String) {
        when (choiceId) {
            "DOOR_KNOCK" -> handleDoorAction(DoorAction.KNOCK)
            "DOOR_SCREAM" -> handleDoorAction(DoorAction.SCREAM)
            "DOOR_BANG" -> handleDoorAction(DoorAction.BANG)
            "DOOR_LEAVE" -> {
                _uiState.update {
                    it.copy(
                        mode = GameMode.EXPLORE,
                        choices = emptyList(),
                        displayedDialogueText = "* Вы отошли от двери."
                    )
                }
            }
            "ORDERLY_PHONE" -> {
                startDialogue(
                    listOf(
                        DialogueLine(
                            name = "ПАЦИЕНТ",
                            portrait = "player_smirk",
                            voice = "player",
                            text = "* Командир, когда вы меня сюда кидали, у вас из кармана халата выпал телефон! Лежит вон в углу."
                        ),
                        DialogueLine(
                            name = "САНИТАР МИХАЛЫЧ",
                            portrait = "orderly_panic",
                            voice = "orderly",
                            text = "* ЧТО?! Моя мобила?! Блин, там же незакрытый экспресс на футбол и жена звонить должна! Отойди к стенке, ща зайду заберу!"
                        )
                    )
                ) {
                    startScriptedOrderlyEntrance()
                }
            }
            "ORDERLY_HEALTHY" -> {
                _uiState.update { it.copy(orderlyAtWindow = false, orderly = it.orderly.copy(state = "outside")) }
                startDialogue(
                    listOf(
                        DialogueLine(
                            name = "САНИТАР МИХАЛЫЧ",
                            portrait = "orderly_smirk",
                            voice = "orderly",
                            text = "* Ага, расскажешь главврачу на обходе в понедельник. Спи давай! (Санитар уходит от окошка)."
                        )
                    )
                )
            }
            "ORDERLY_WALLS" -> {
                _uiState.update { it.copy(orderlyAtWindow = false, orderly = it.orderly.copy(state = "outside")) }
                startDialogue(
                    listOf(
                        DialogueLine(
                            name = "САНИТАР МИХАЛЫЧ",
                            portrait = "orderly_normal",
                            voice = "orderly",
                            text = "* Это твои тараканы в голове аплодируют. Не отвлекай персонал! (Санитар закрывает заслонку)."
                        )
                    )
                )
            }
            "BATTLE_FIGHT" -> startAttackBarPhase()
            "BATTLE_ACT" -> openBattleActMenu()
            "BATTLE_ITEM" -> useBattleItem()
            "BATTLE_SPARE" -> trySpareOrderly()
            "ACT_CHECK" -> {
                _uiState.update { it.copy(battle = it.battle.copy(phase = BattlePhase.ENEMY_DIALOGUE)) }
                startDialogue(
                    listOf(
                        DialogueLine(
                            name = "ОЦЕНКА",
                            portrait = "orderly_normal",
                            text = "* САНИТАР МИХАЛЫЧ — АТК 4, ЗАЩ 2. Носит модный белый халат. Кто первый наденет этот халат — тот и станет врачом!"
                        )
                    )
                ) {
                    startEnemyDodgeWave()
                }
            }
            "ACT_SHOELACE" -> {
                _uiState.update {
                    it.copy(
                        battle = it.battle.copy(
                            phase = BattlePhase.ENEMY_DIALOGUE,
                            enemyDefDown = true,
                            bossEmotion = "shock"
                        )
                    )
                }
                startDialogue(
                    listOf(
                        DialogueLine(
                            name = "ПАЦИЕНТ",
                            portrait = "player_smirk",
                            voice = "player",
                            text = "* Эй, Михалыч, у тебя на тапках шнурки развязались!"
                        ),
                        DialogueLine(
                            name = "САНИТАР МИХАЛЫЧ",
                            portrait = "orderly_panic",
                            voice = "orderly",
                            text = "* Где?! А ведь у меня тапки на липучках!.. Тьфу ты, сбил с толку! (ЗАЩИТА Михалыча снижена! Следующий удар сильнее!)"
                        )
                    )
                ) {
                    startEnemyDodgeWave()
                }
            }
            "ACT_BACK" -> openBattleMainMenu("* Вы вернулись к выбору действия.")
            "RESTART_BATTLE" -> {
                _uiState.update { it.copy(playerHP = 20) }
                startOrderlyBattle()
            }
            "RESTART_CELL" -> skipToCell()
            "RESTART_INTRO" -> restartFullGame()
        }
    }

    /* ==========================================
       ROOM INTERACTION & COMBOS
       ========================================== */
    private fun tryInteractInRoom() {
        val state = _uiState.value
        val px = state.player.x + state.player.w / 2
        val py = state.player.y + state.player.h / 2

        if (state.doorOpen && abs(px - 320f) < 55f && py > 275f) {
            triggerEscapeEnding()
            return
        }

        if (abs(px - 320f) < 55f && py > 275f) {
            openDoorMenu()
            return
        }

        if (state.orderlyInside) {
            val ox = state.orderly.x + state.orderly.w / 2
            val oy = state.orderly.y + state.orderly.h / 2
            if (kotlin.math.hypot(px - ox, py - oy) < 55f) {
                startDialogue(
                    listOf(
                        DialogueLine(
                            name = "САНИТАР МИХАЛЫЧ",
                            portrait = "orderly_panic",
                            voice = "orderly",
                            text = "* Да где ж этот проклятый смартфон?! Под матрас закатился, что ли?! Не стой над душой!"
                        )
                    )
                ) {
                    _uiState.update { it.copy(mode = GameMode.ORDERLY_SEARCH) }
                }
                return
            }
        }

        // Pillow in corner
        if (px < 195f && py < 145f) {
            startDialogue(
                listOf(
                    DialogueLine(
                        name = "",
                        portrait = null,
                        text = "* Это казённая мягкая подушка. Пахнет валерьянкой и безысходностью."
                    )
                )
            )
            return
        }

        // Requirement 2: Scratched Cipher Wall Hint on top-right wall
        if (px > 400f && py < 140f) {
            startDialogue(
                listOf(
                    DialogueLine(
                        name = "ШИФР ПАЦИЕНТА #0",
                        portrait = null,
                        text = "* На обшивке стены кто-то нацарапал ногтями странный шифр пациента №0: \"РЕЦЕПТ ВЕЧНОГО СПОКОЙСТВИЯ: Ударь. Закричи. Дважды ударь. И снова закричи у самой двери...\""
                    )
                )
            )
            return
        }
    }

    private fun openDoorMenu() {
        showChoices(
            prompt = "* Перед вами массивная стальная дверь с маленьким бронеокошком. Что будете делать?",
            portrait = "player_normal",
            choices = listOf(
                GameChoice("DOOR_KNOCK", "Постучать"),
                GameChoice("DOOR_SCREAM", "Орать, что ты не псих"),
                GameChoice("DOOR_BANG", "Тарабанить (буянить и бить)"),
                GameChoice("DOOR_LEAVE", "Отойти от двери")
            )
        )
    }

    private fun handleDoorAction(action: DoorAction) {
        if (action == DoorAction.KNOCK) {
            soundEngine.sfxKnock()
            _uiState.update {
                it.copy(
                    doorActionHistory = emptyList(),
                    orderlyAtWindow = true,
                    orderly = it.orderly.copy(state = "window")
                )
            }
            startDialogue(
                listOf(
                    DialogueLine(
                        name = "",
                        portrait = null,
                        text = "* Тук-тук-тук. Вы вежливо и культурно постучали в железную дверь."
                    ),
                    DialogueLine(
                        name = "САНИТАР МИХАЛЫЧ",
                        portrait = "orderly_normal",
                        voice = "orderly",
                        text = "* (В окошке появляется заспанное лицо санитара) Чего тебе, болезный? Тихий час вообще-то!"
                    )
                )
            ) {
                openOrderlyPersuasionMenu()
            }
            return
        }

        val updatedHistory = (_uiState.value.doorActionHistory + action).takeLast(5)
        _uiState.update { it.copy(doorActionHistory = updatedHistory) }

        if (updatedHistory.size == 5 && updatedHistory == secretCombo) {
            triggerVegetableSecretEnding()
            return
        }

        var matchedSteps = 0
        for (i in 1..updatedHistory.size) {
            val slice = updatedHistory.takeLast(i)
            val target = secretCombo.take(i)
            if (slice == target) {
                matchedSteps = i
            }
        }

        if (action == DoorAction.BANG) {
            soundEngine.sfxBangDoor()
            triggerScreenShake(duration = 16, intensity = 14f)
            spawnDoorDustParticles()

            var text = "* БАМ! БАМ! Вы яростно буяните и колотите ногами по стальной двери!"
            if (matchedSteps == 1) text += " За дверью послышалось недовольное ворчание санитаров (Комбо 1/5)."
            if (matchedSteps == 3) text += " Из коридора кричат: «Эй, щас кто-то допрыгается!» (Комбо 3/5)."
            if (matchedSteps == 4) text += " Слышен звон ампул в коридоре! Ещё один крик — и они ворвутся! (Комбо 4/5)."

            startDialogue(
                listOf(
                    DialogueLine(
                        name = "ПАЦИЕНТ",
                        portrait = "player_angry",
                        voice = "player",
                        text = text
                    )
                )
            ) {
                openDoorMenu()
            }
        } else if (action == DoorAction.SCREAM) {
            soundEngine.sfxScream()
            triggerScreenShake(duration = 14, intensity = 12f)

            var text = "* «Я НЕ ПСИХ!! ВЫПУСТИТЕ МЕНЯ НЕМЕДЛЕННО, Я АБСОЛЮТНО НОРМАЛЬНЫЙ!!»"
            if (matchedSteps == 2) text += " Санитар за дверью: «Слышь, Петрович, он опять буянит...» (Комбо 2/5)."

            startDialogue(
                listOf(
                    DialogueLine(
                        name = "ПАЦИЕНТ",
                        portrait = "player_shock",
                        voice = "player",
                        text = text
                    )
                )
            ) {
                openDoorMenu()
            }
        }
    }

    private fun openOrderlyPersuasionMenu() {
        showChoices(
            prompt = "* Санитар Михалыч смотрит на вас через решётчатое окошко двери. Как убедить его открыть?",
            portrait = "orderly_normal",
            choices = listOf(
                GameChoice("ORDERLY_PHONE", "«Вы выронили телефон у меня в камере!»"),
                GameChoice("ORDERLY_HEALTHY", "«Откройте, я совершенно здоров!»"),
                GameChoice("ORDERLY_WALLS", "«Тут в стенах кто-то шуршит!»")
            )
        )
    }

    /* ==========================================
       REQUIREMENT 4: SMOOTH SCRIPTED ORDERLY ENTRY
       ========================================== */
    private fun startScriptedOrderlyEntrance() {
        soundEngine.sfxDoorOpen()
        triggerScreenShake(duration = 10, intensity = 8f)

        orderlyEnterStep = 1
        orderlyEnterTimer = 0

        _uiState.update {
            it.copy(
                mode = GameMode.CUTSCENE_ORDERLY_ENTER,
                doorOpen = true,
                orderlyAtWindow = false,
                orderlyInside = true,
                orderly = it.orderly.copy(
                    x = 304f,
                    y = 330f,
                    dir = "up",
                    state = "entering",
                    hasCoat = true,
                    bubbleSymbol = null
                )
            )
        }
    }

    private fun updateScriptedOrderlyEntry() {
        orderlyEnterTimer++
        val current = _uiState.value

        when (orderlyEnterStep) {
            1 -> {
                // Player backs up towards upper center of room
                var py = current.player.y
                val targetPy = 150f
                if (py > targetPy) {
                    py = (py - 1.6f).coerceAtLeast(targetPy)
                    val frame = ((orderlyEnterTimer / 8) % 4)
                    _uiState.update {
                        it.copy(player = it.player.copy(y = py, dir = "up", animFrame = frame, moving = true))
                    }
                } else {
                    _uiState.update {
                        it.copy(player = it.player.copy(moving = false, dir = "down"))
                    }
                    orderlyEnterStep = 2
                    orderlyEnterTimer = 0
                }
            }
            2 -> {
                // Mihalych steps through the doorway from y = 330 down to y = 220
                var oy = current.orderly.y
                val targetOy = 220f
                if (oy > targetOy) {
                    oy = (oy - 1.8f).coerceAtLeast(targetOy)
                    val frame = ((orderlyEnterTimer / 7) % 4)
                    _uiState.update {
                        it.copy(orderly = it.orderly.copy(y = oy, dir = "up", animFrame = frame))
                    }
                } else {
                    orderlyEnterStep = 3
                    orderlyEnterTimer = 0
                }
            }
            3 -> {
                // Mihalych looks around, "?" bubble appears above his head
                val lookPhase = orderlyEnterTimer / 25
                val dir = if (lookPhase % 2 == 0) "left" else "right"
                _uiState.update {
                    it.copy(orderly = it.orderly.copy(dir = dir, bubbleSymbol = "?"))
                }
                if (orderlyEnterTimer > 60) {
                    orderlyEnterStep = 4
                    orderlyEnterTimer = 0
                }
            }
            4 -> {
                // Mihalych pats pocket, "!" bubble appears
                _uiState.update {
                    it.copy(orderly = it.orderly.copy(dir = "up", bubbleSymbol = "!"))
                }
                if (orderlyEnterTimer > 40) {
                    orderlyEnterStep = 5
                    _uiState.update {
                        it.copy(orderly = it.orderly.copy(bubbleSymbol = null))
                    }
                    // Trigger angry dialogue
                    startDialogue(
                        listOf(
                            DialogueLine(
                                name = "САНИТАР МИХАЛЫЧ",
                                portrait = "orderly_angry",
                                voice = "orderly",
                                text = "* Погоди-ка... Вот же мой телефон, во внутреннем кармане лежит! Ах ты ж хитрый симулянт, обмануть меня вздумал?!"
                            ),
                            DialogueLine(
                                name = "САНИТАР МИХАЛЫЧ",
                                portrait = "orderly_angry",
                                voice = "orderly",
                                text = "* Ну всё, щас ты у меня получишь лечебных люлей!"
                            )
                        )
                    ) {
                        startOrderlyBattle()
                    }
                }
            }
        }
    }

    /* ==========================================
       UNDERTALE BATTLE SYSTEM
       ========================================== */
    private fun startOrderlyBattle() {
        soundEngine.sfxBattleStart()
        _uiState.update {
            it.copy(
                mode = GameMode.BATTLE,
                battle = BattleState(
                    phase = BattlePhase.INTRO_FLASH,
                    enemyHP = 100,
                    pillsLeft = 2,
                    bossEmotion = "normal"
                )
            )
        }
        viewModelScope.launch {
            delay(650)
            openBattleMainMenu("* Санитар Михалыч преграждает путь к выходу!")
        }
    }

    private fun openBattleMainMenu(flavorText: String) {
        val pills = _uiState.value.battle.pillsLeft
        _uiState.update {
            it.copy(
                battle = it.battle.copy(
                    phase = BattlePhase.MENU,
                    bossEmotion = if (it.battle.enemyDefDown) "shock" else "normal"
                )
            )
        }
        showChoices(
            prompt = flavorText,
            portrait = "orderly_angry",
            choices = listOf(
                GameChoice("BATTLE_FIGHT", "⚔ БИТВА"),
                GameChoice("BATTLE_ACT", "🔊 ДЕЙСТВИЕ"),
                GameChoice("BATTLE_ITEM", "💊 ВЕЩИ (x$pills)"),
                GameChoice("BATTLE_SPARE", "✖ ПОЩАДА")
            )
        )
    }

    private fun openBattleActMenu() {
        showChoices(
            prompt = "* Выберите действие:",
            portrait = "player_smirk",
            choices = listOf(
                GameChoice("ACT_CHECK", "Оценить противника"),
                GameChoice("ACT_SHOELACE", "«У вас шнурки развязались!»"),
                GameChoice("ACT_BACK", "Назад в меню")
            )
        )
    }

    private fun useBattleItem() {
        val state = _uiState.value
        if (state.battle.pillsLeft <= 0) {
            openBattleMainMenu("* У вас больше не осталось припрятанных таблеток валерьянки!")
            return
        }
        soundEngine.sfxHeal()
        _uiState.update {
            it.copy(
                playerHP = (it.playerHP + 12).coerceAtMost(it.maxHP),
                battle = it.battle.copy(
                    phase = BattlePhase.ENEMY_DIALOGUE,
                    pillsLeft = it.battle.pillsLeft - 1
                )
            )
        }
        startDialogue(
            listOf(
                DialogueLine(
                    name = "",
                    portrait = "player_normal",
                    text = "* Вы проглотили припрятанную таблетку сладкой валерьянки. Восстановлено +12 ОЗ! (Осталось: ${_uiState.value.battle.pillsLeft})"
                )
            )
        ) {
            startEnemyDodgeWave()
        }
    }

    private fun trySpareOrderly() {
        _uiState.update { it.copy(battle = it.battle.copy(phase = BattlePhase.ENEMY_DIALOGUE)) }
        startDialogue(
            listOf(
                DialogueLine(
                    name = "САНИТАР МИХАЛЫЧ",
                    portrait = "orderly_angry",
                    voice = "orderly",
                    text = "* Никаких переговоров! Мне ещё этого... халата лишиться не хватало! Защищайся!"
                )
            )
        ) {
            startEnemyDodgeWave()
        }
    }

    private fun startAttackBarPhase() {
        _uiState.update {
            it.copy(
                battle = it.battle.copy(
                    phase = BattlePhase.ATTACK_BAR,
                    barCursor = 0f,
                    barDir = 1f
                ),
                choices = emptyList(),
                currentSpeaker = "ПРИЦЕЛ АТАКИ",
                currentPortrait = null,
                displayedDialogueText = "* Нажмите [Z / ENTER / КНОПКУ Z], когда белая линия будет в центре!",
                canAdvanceDialogue = false
            )
        }
    }

    private fun confirmAttackBarHit() {
        val b = _uiState.value.battle
        if (b.phase != BattlePhase.ATTACK_BAR) return

        soundEngine.sfxSlash()
        triggerScreenShake(duration = 18, intensity = 15f)

        val dist = abs(b.barCursor - 0.5f)
        var dmg = ((1f - dist * 1.4f) * 38f).toInt()
        if (dist < 0.09f) dmg = 45
        if (b.enemyDefDown) {
            dmg = (dmg * 1.4f).toInt()
        }
        dmg = dmg.coerceAtLeast(18)

        val newEnemyHP = (b.enemyHP - dmg).coerceAtLeast(0)
        _uiState.update {
            it.copy(
                battle = b.copy(
                    phase = BattlePhase.SLASH_ANIM,
                    enemyHP = newEnemyHP,
                    lastDamage = dmg,
                    enemyShake = 22,
                    slashTimer = 48,
                    slashProgress = 0f,
                    bossEmotion = "hurt",
                    damageText = "-$dmg",
                    damageTextY = 0f,
                    damageTextVy = -7.5f,
                    damageVisible = true,
                    enemyDefDown = false
                ),
                displayedDialogueText = "* ВЫ НАНОСИТЕ УДАР! -$dmg ОЗ у санитара!"
            )
        }
    }

    private fun startEnemyDodgeWave() {
        val b = _uiState.value.battle
        if (b.enemyHP <= 0) {
            triggerBattleVictory()
            return
        }

        val turn = b.turnCount + 1
        val taunts = listOf(
            "* Щас я тебе пропишу внеплановую витаминку!",
            "* Лови больничный инвентарь, беглец!",
            "* Никто ещё не уходил из шестой палаты без процедуры!"
        )
        val taunt = taunts[(turn - 1) % taunts.size]

        _uiState.update {
            it.copy(battle = b.copy(phase = BattlePhase.ENEMY_DIALOGUE, turnCount = turn, bossEmotion = "normal"))
        }

        startDialogue(
            listOf(
                DialogueLine(
                    name = "САНИТАР МИХАЛЫЧ",
                    portrait = "orderly_angry",
                    voice = "orderly",
                    text = taunt
                )
            )
        ) {
            _uiState.update {
                it.copy(
                    battle = it.battle.copy(
                        phase = BattlePhase.DODGE,
                        waveTimer = 260,
                        soulX = 312f,
                        soulY = 255f,
                        soulInvuln = 0,
                        bullets = mutableListOf()
                    ),
                    currentSpeaker = "АТАКА САНИТАРА!",
                    currentPortrait = null,
                    displayedDialogueText = "* Уворачивайтесь красным сердечком [СТРЕЛКИ / D-PAD / КАСАНИЕ] от шприцов, таблеток и ключей!",
                    canAdvanceDialogue = false
                )
            }
        }
    }

    private fun triggerBattleVictory() {
        soundEngine.sfxVictory()
        _uiState.update { it.copy(battle = it.battle.copy(phase = BattlePhase.VICTORY)) }
        startDialogue(
            listOf(
                DialogueLine(
                    name = "САНИТАР МИХАЛЫЧ",
                    portrait = "orderly_panic",
                    voice = "orderly",
                    text = "* Ой-ёй... Чё-то в глазах потемнело... Походу давление скакнуло... (Михалыч рухнул на мягкий пол!)"
                ),
                DialogueLine(
                    name = "",
                    portrait = "player_smirk",
                    voice = "narrator",
                    text = "* ВЫ ПОБЕДИЛИ! Получено 0 ОП и 1 БЕЛЫЙ МЕДИЦИНСКИЙ ХАЛАТ! Вы снимаете с Михалыча халат с шапочкой и надеваете на себя!"
                )
            )
        ) {
            triggerDoctorCoatEnding()
        }
    }

    private fun triggerDoctorCoatEnding() {
        soundEngine.sfxBangDoor()
        triggerScreenShake(duration = 20, intensity = 16f)

        _uiState.update {
            it.copy(
                mode = GameMode.POST_BATTLE_CUTSCENE,
                doorOpen = false,
                orderlyAtWindow = true,
                orderlyInside = true,
                endingsUnlocked = it.endingsUnlocked + "escape",
                player = it.player.copy(x = 282f, y = 335f, dir = "up", hasCoat = true),
                orderly = it.orderly.copy(x = 304f, y = 265f, state = "stripped_locked", hasCoat = false),
                orderly2 = it.orderly2.copy(x = 334f, y = 335f, hasCoat = true),
                orderly2Visible = true
            )
        }

        startDialogue(
            listOf(
                DialogueLine(
                    name = "ПАЦИЕНТ (В ХАЛАТЕ)",
                    portrait = "player_doctor",
                    voice = "player",
                    text = "* (Вы выскакиваете в коридор и запираете стальную дверь на засов) В психиатрии всё просто, Михалыч: кто первый надел халат — тот и врач!"
                ),
                DialogueLine(
                    name = "МИХАЛЫЧ (ИЗ КАМЕРЫ)",
                    portrait = "orderly_panic",
                    voice = "orderly",
                    text = "* (БАМ! БАМ! Тарабанит изнутри по двери) ЭЙ!! ВЫПУСТИТЕ НЕМЕДЛЕННО!! Я НЕ ПСИХ, Я САНИТАР МИХАЛЫЧ! ОН У МЕНЯ ХАЛАТ ОТЖАЛ!!"
                ),
                DialogueLine(
                    name = "НОВЕНЬКИЙ САНИТАР",
                    portrait = "orderly_normal",
                    voice = "orderly",
                    text = "* (Подходит молодой стажёр с папкой) Ого, товарищ доктор, ну и буйный у вас пациент! Уже санитаром Михалычем себя возомнил! Может, ему галоперидолу?"
                ),
                DialogueLine(
                    name = "ДОКТОР #404",
                    portrait = "player_doctor",
                    voice = "player",
                    text = "* Обязательно, коллега! Пропишите ему двойную порцию, а я пока пойду домой — у меня смена закончилась!"
                ),
                DialogueLine(
                    name = "ПОБЕДА!",
                    portrait = "player_doctor",
                    voice = "narrator",
                    text = "* КОНЦОВКА «КТО В ХАЛАТЕ — ТОТ И ВРАЧ»: Михалыч остался куковать в мягкой камере, а вы гордо вышли через главный вход!"
                )
            )
        ) {
            showChoices(
                prompt = "* Вы гениально сбежали из дурки в халате врача! Хотите сыграть ещё раз или открыть секретную концовку «Овощ»?",
                portrait = "player_doctor",
                choices = listOf(
                    GameChoice("RESTART_CELL", "Вернуться в камеру"),
                    GameChoice("RESTART_INTRO", "Начать с катсцены")
                )
            )
        }
    }

    private fun triggerVegetableSecretEnding() {
        soundEngine.sfxScream()
        triggerScreenShake(duration = 20, intensity = 18f)

        _uiState.update {
            it.copy(
                mode = GameMode.ENDING_VEGETABLE,
                doorOpen = true,
                orderlyInside = true,
                orderly2Visible = true,
                player = it.player.copy(x = 306f, y = 190f, dir = "down"),
                orderly = it.orderly.copy(x = 275f, y = 235f, state = "injecting"),
                orderly2 = it.orderly2.copy(x = 335f, y = 235f, hasCoat = true)
            )
        }

        startDialogue(
            listOf(
                DialogueLine(
                    name = "САНИТАР МИХАЛЫЧ",
                    portrait = "orderly_angry",
                    voice = "orderly",
                    text = "* НУ ВСЁ, ДОИГРАЛСЯ! Тарабанит и орёт, тарабанит и орёт! Петрович, доставай «Успокоитин-9000»!"
                ),
                DialogueLine(
                    name = "",
                    portrait = "syringe",
                    voice = "narrator",
                    text = "* Дверь слетает с катушек! Два огромных санитара зажимают вас в мягком углу с гигантским светящимся шприцем!"
                )
            )
        ) {
            soundEngine.sfxSyringe()
            _uiState.update {
                it.copy(
                    playerHP = 1,
                    isVegetable = true,
                    isDruggedMode = true,
                    endingsUnlocked = it.endingsUnlocked + "vegetable"
                )
            }

            startDialogue(
                listOf(
                    DialogueLine(
                        name = "ПАЦИЕНТ (ОВОЩ)",
                        portrait = "player_vegetable",
                        voice = "vegetable",
                        slow = true,
                        text = "* У-у-у... ы-ы-ы... пускает слюни... какие мягкие обои... бульк..."
                    ),
                    DialogueLine(
                        name = "СЕКРЕТНАЯ КОНЦОВКА ОТКРЫТА!",
                        portrait = "player_vegetable",
                        voice = "narrator",
                        text = "* КОНЦОВКА «ОВОЩ»: Вам вкололи убойный препарат. Побег не удался, зато теперь вас вообще ничего не тревожит!"
                    )
                )
            ) {
                showChoices(
                    prompt = "* ИГРА ОКОНЧЕНА (Секретная концовка «Овощ» открыта!). Что дальше?",
                    portrait = "player_vegetable",
                    choices = listOf(
                        GameChoice("RESTART_CELL", "Переиграть в камере (Попробовать побег)"),
                        GameChoice("RESTART_INTRO", "С самого начала (Катсцена)")
                    )
                )
            }
        }
    }

    private fun triggerEscapeEnding() {
        soundEngine.sfxVictory()
        _uiState.update {
            it.copy(
                mode = GameMode.ENDING_ESCAPE,
                endingsUnlocked = it.endingsUnlocked + "escape"
            )
        }

        startDialogue(
            listOf(
                DialogueLine(
                    name = "ПАЦИЕНТ",
                    portrait = "player_smirk",
                    voice = "player",
                    text = "* Пока, Михалыч! Ищи свой телефон дальше — можешь даже позвонить на него с тумбочки!"
                ),
                DialogueLine(
                    name = "САНИТАР МИХАЛЫЧ",
                    portrait = "orderly_panic",
                    voice = "orderly",
                    text = "* СТОЯТЬ!! А ну вернись! Блин, он ещё и дверь снаружи на засов закрыл! ПЕТРОВИИИЧ, ВЫПУСТИ!"
                ),
                DialogueLine(
                    name = "ПОБЕДА!",
                    portrait = "player_smirk",
                    voice = "narrator",
                    text = "* КОНЦОВКА «ПОБЕГ ИЗ ДУРДОМА»: Вы перехитрили санитара, заперли его в мягкой камере и выбежали на свободу!"
                )
            )
        ) {
            showChoices(
                prompt = "* Вы успешно сбежали из дурки! Хотите проверить секретную концовку «Овощ»?",
                portrait = "player_smirk",
                choices = listOf(
                    GameChoice("RESTART_CELL", "Вернуться в камеру"),
                    GameChoice("RESTART_INTRO", "Начать с катсцены")
                )
            )
        }
    }

    /* ==========================================
       TICK / UPDATE LOOP (~60 FPS)
       ========================================== */
    fun setSoulPosition(x: Float, y: Float) {
        val b = _uiState.value.battle
        if (b.phase == BattlePhase.DODGE) {
            val minX = 220f + 6f
            val maxX = 220f + 200f - b.soulSize - 6f
            val minY = 185f + 6f
            val maxY = 185f + 155f - b.soulSize - 6f
            _uiState.update {
                it.copy(
                    battle = b.copy(
                        soulX = x.coerceIn(minX, maxX),
                        soulY = y.coerceIn(minY, maxY)
                    )
                )
            }
        }
    }

    private fun updateTick() {
        val current = _uiState.value
        val newTick = current.tick + 1
        val newShake = if (current.screenShakeTick > 0) current.screenShakeTick - 1 else 0

        // Update dust particles
        val updatedDust = current.dustParticles.mapNotNull { p ->
            p.x += p.vx
            p.y += p.vy
            p.vy += 0.15f // gravity
            p.alpha -= 0.035f
            if (p.alpha > 0f) p else null
        }

        if (current.mode == GameMode.CUTSCENE_ORDERLY_ENTER) {
            updateScriptedOrderlyEntry()
            _uiState.update {
                it.copy(tick = newTick, screenShakeTick = newShake, dustParticles = updatedDust)
            }
            return
        }

        if (current.mode == GameMode.BATTLE) {
            updateBattleTick(current, newTick, newShake, updatedDust)
            return
        }

        // Overworld movement
        if (current.mode == GameMode.EXPLORE || current.mode == GameMode.ORDERLY_SEARCH) {
            updatePlayerMovement(current, newTick, newShake, updatedDust)
        } else {
            _uiState.update {
                it.copy(tick = newTick, screenShakeTick = newShake, dustParticles = updatedDust)
            }
        }
    }

    private fun updatePlayerMovement(
        current: UiState,
        newTick: Long,
        newShake: Int,
        dust: List<DustParticle>
    ) {
        var dx = 0f
        var dy = 0f
        var dir = current.player.dir

        if (inputUp) { dy -= current.player.speed; dir = "up" }
        if (inputDown) { dy += current.player.speed; dir = "down" }
        if (inputLeft) { dx -= current.player.speed; dir = "left" }
        if (inputRight) { dx += current.player.speed; dir = "right" }

        val moving = dx != 0f || dy != 0f
        var animFrame = current.player.animFrame
        if (moving && newTick % 7 == 0L) {
            animFrame = (animFrame + 1) % 4
        } else if (!moving) {
            animFrame = 0
        }

        var nx = current.player.x + dx
        var ny = current.player.y + dy

        // Clamp to room bounds
        nx = nx.coerceIn(130f, 510f - current.player.w)

        // Door escape check
        val inDoorX = abs((nx + current.player.w / 2f) - 320f) < 34f
        if (current.doorOpen && inDoorX && (ny + current.player.h > 315f - 4f)) {
            triggerEscapeEnding()
            return
        }

        ny = ny.coerceIn(85f, 315f - current.player.h)

        // Interact hint calculation
        val px = nx + current.player.w / 2f
        val py = ny + current.player.h / 2f
        val hint: String? = when {
            abs(px - 320f) < 55f && py > 275f -> {
                if (current.doorOpen) "[Z / ДЕЙСТВИЕ] — ВЫБЕЖАТЬ НА СВОБОДУ!" else "[Z / ДЕЙСТВИЕ] — ДВЕРЬ КАМЕРЫ"
            }
            current.orderlyInside && kotlin.math.hypot(px - (current.orderly.x + 16f), py - (current.orderly.y + 20f)) < 55f -> {
                "[Z / ДЕЙСТВИЕ] — САНИТАР МИХАЛЫЧ"
            }
            px < 195f && py < 145f -> "[Z / ДЕЙСТВИЕ] — ОСМОТРЕТЬ ПОДУШКУ"
            px > 400f && py < 140f -> "[Z / ДЕЙСТВИЕ] — ШИФР НА СТЕНЕ"
            else -> null
        }

        // Orderly searching animation if inside
        var newOrderly = current.orderly
        if (current.orderlyInside && current.orderly.state == "searching") {
            val odx = current.orderly.targetX - current.orderly.x
            val ody = current.orderly.targetY - current.orderly.y
            val dist = kotlin.math.hypot(odx, ody)
            if (dist > 4f) {
                val oFrame = ((newTick / 8) % 4).toInt()
                newOrderly = newOrderly.copy(
                    x = newOrderly.x + (odx / dist) * 1.5f,
                    y = newOrderly.y + (ody / dist) * 1.5f,
                    animFrame = oFrame
                )
            } else {
                val timer = newOrderly.searchTimer + 1
                if (timer > 60) {
                    val spots = listOf(Pair(160f, 120f), Pair(440f, 125f), Pair(175f, 245f))
                    val pick = spots[(Math.random() * spots.size).toInt()]
                    newOrderly = newOrderly.copy(
                        searchTimer = 0,
                        targetX = pick.first,
                        targetY = pick.second,
                        animFrame = 0
                    )
                } else {
                    newOrderly = newOrderly.copy(searchTimer = timer)
                }
            }
        }

        _uiState.update {
            it.copy(
                tick = newTick,
                screenShakeTick = newShake,
                dustParticles = dust,
                interactPrompt = hint,
                player = it.player.copy(x = nx, y = ny, dir = dir, moving = moving, animFrame = animFrame),
                orderly = newOrderly
            )
        }
    }

    private fun updateBattleTick(
        current: UiState,
        newTick: Long,
        newShake: Int,
        dust: List<DustParticle>
    ) {
        val b = current.battle

        // Attack timing bar update
        if (b.phase == BattlePhase.ATTACK_BAR) {
            var cursor = b.barCursor + 0.022f * b.barDir
            var dir = b.barDir
            if (cursor >= 1f) {
                cursor = 1f
                dir = -1f
            } else if (cursor <= 0f) {
                cursor = 0f
                dir = 1f
            }
            _uiState.update {
                it.copy(
                    tick = newTick,
                    screenShakeTick = newShake,
                    dustParticles = dust,
                    battle = b.copy(barCursor = cursor, barDir = dir)
                )
            }
            return
        }

        // Slash animation & damage number jump physics update
        if (b.phase == BattlePhase.SLASH_ANIM) {
            val timer = b.slashTimer - 1

            // Curved slash progress 0f to 1f
            val slashP = (48 - timer) / 48f

            // Damage text jump physics
            var dY = b.damageTextY + b.damageTextVy
            var dVy = b.damageTextVy + 0.5f // gravity
            if (dY > 20f) {
                dY = 20f
                dVy = 0f
            }

            if (timer <= 0) {
                if (b.enemyHP <= 0) {
                    triggerBattleVictory()
                } else {
                    startEnemyDodgeWave()
                }
            } else {
                _uiState.update {
                    it.copy(
                        tick = newTick,
                        screenShakeTick = newShake,
                        dustParticles = dust,
                        battle = b.copy(
                            slashTimer = timer,
                            slashProgress = slashP,
                            enemyShake = if (b.enemyShake > 0) b.enemyShake - 1 else 0,
                            damageTextY = dY,
                            damageTextVy = dVy
                        )
                    )
                }
            }
            return
        }

        // Dodge phase update
        if (b.phase == BattlePhase.DODGE) {
            var sx = b.soulX
            var sy = b.soulY
            val speed = b.soulSpeed
            if (inputUp) sy -= speed
            if (inputDown) sy += speed
            if (inputLeft) sx -= speed
            if (inputRight) sx += speed

            // Battle box: x=220, y=185, w=200, h=155
            val minX = 220f + 6f
            val maxX = 220f + 200f - b.soulSize - 6f
            val minY = 185f + 6f
            val maxY = 185f + 155f - b.soulSize - 6f
            sx = sx.coerceIn(minX, maxX)
            sy = sy.coerceIn(minY, maxY)

            val invuln = if (b.soulInvuln > 0) b.soulInvuln - 1 else 0
            val bullets = b.bullets.toMutableList()

            // Spawn bullets according to wave pattern
            val pattern = (b.turnCount - 1) % 3
            if (pattern == 0 && b.waveTimer % 28 == 0) {
                // Flying syringes
                val fromLeft = Math.random() < 0.5
                val py = 185f + 14f + (Math.random() * (155f - 28f)).toFloat()
                bullets.add(
                    Bullet(
                        x = if (fromLeft) 220f - 16f else 220f + 200f + 16f,
                        y = py,
                        vx = if (fromLeft) 3.1f else -3.1f,
                        vy = 0f,
                        w = 18f,
                        h = 8f,
                        type = "syringe"
                    )
                )
            } else if (pattern == 1 && b.waveTimer % 24 == 0) {
                // Bouncing pills from ceiling
                val px = 220f + 14f + (Math.random() * (200f - 28f)).toFloat()
                bullets.add(
                    Bullet(
                        x = px,
                        y = 185f - 12f,
                        vx = ((Math.random() - 0.5) * 2.2).toFloat(),
                        vy = 2.6f,
                        w = 10f,
                        h = 10f,
                        type = "pill"
                    )
                )
            } else if (pattern == 2 && b.waveTimer % 30 == 0) {
                // Spinning hospital keys
                val side = (Math.random() * 3).toInt()
                var kx = 220f + (Math.random() * 200f).toFloat()
                var ky = 185f - 12f
                if (side == 1) { kx = 220f - 12f; ky = 185f + (Math.random() * 155f).toFloat() }
                if (side == 2) { kx = 220f + 200f + 12f; ky = 185f + (Math.random() * 155f).toFloat() }

                val angle = atan2((sy + 8f) - ky, (sx + 8f) - kx)
                bullets.add(
                    Bullet(
                        x = kx,
                        y = ky,
                        vx = (cos(angle) * 2.9).toFloat(),
                        vy = (sin(angle) * 2.9).toFloat(),
                        w = 11f,
                        h = 11f,
                        type = "key"
                    )
                )
            }

            // Move bullets and check collision
            var playerHp = current.playerHP
            var newSoulInvuln = invuln

            val itBullets = bullets.iterator()
            while (itBullets.hasNext()) {
                val bul = itBullets.next()
                bul.x += bul.vx
                bul.y += bul.vy

                // Pill bouncing off bottom of box
                if (bul.type == "pill" && bul.y + bul.h > 185f + 155f - 4f && bul.vy > 0) {
                    bul.vy = -bul.vy * 0.85f
                }

                // Collision with soul
                if (newSoulInvuln == 0 &&
                    bul.x < sx + b.soulSize - 2f &&
                    bul.x + bul.w > sx + 2f &&
                    bul.y < sy + b.soulSize - 2f &&
                    bul.y + bul.h > sy + 2f
                ) {
                    soundEngine.sfxDamage()
                    triggerScreenShake(duration = 16, intensity = 14f)
                    newSoulInvuln = 45
                    playerHp = (playerHp - 4).coerceAtLeast(0)
                }

                // Remove out of bounds
                if (bul.x < 220f - 40f || bul.x > 220f + 200f + 40f || bul.y < 185f - 40f || bul.y > 185f + 155f + 40f) {
                    itBullets.remove()
                }
            }

            if (playerHp <= 0) {
                _uiState.update {
                    it.copy(
                        playerHP = 0,
                        battle = b.copy(phase = BattlePhase.DEFEAT)
                    )
                }
                showChoices(
                    prompt = "* У вас закончились ОЗ! Но ваша РЕШИМОСТЬ выбраться из дурки сильнее любых уколов!",
                    portrait = "player_shock",
                    choices = listOf(
                        GameChoice("RESTART_BATTLE", "Решительно переиграть бой!"),
                        GameChoice("RESTART_CELL", "Вернуться в камеру")
                    )
                )
                return
            }

            val newTimer = b.waveTimer - 1
            if (newTimer <= 0) {
                val postWaveTexts = listOf(
                    "* Санитар Михалыч тяжело дышит, поправляя колпак.",
                    "* Михалыч ищет глазами, чем бы ещё в вас кинуть.",
                    "* Халат Михалыча выглядит очень солидно. Скоро он станет вашим!"
                )
                openBattleMainMenu(postWaveTexts[b.turnCount % postWaveTexts.size])
            } else {
                _uiState.update {
                    it.copy(
                        tick = newTick,
                        screenShakeTick = newShake,
                        dustParticles = dust,
                        playerHP = playerHp,
                        battle = b.copy(
                            soulX = sx,
                            soulY = sy,
                            soulInvuln = newSoulInvuln,
                            waveTimer = newTimer,
                            bullets = bullets
                        )
                    )
                }
            }
            return
        }

        _uiState.update {
            it.copy(tick = newTick, screenShakeTick = newShake, dustParticles = dust)
        }
    }
}
