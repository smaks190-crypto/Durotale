package com.example.model

enum class GameMode {
    CUTSCENE_DRAG,
    EXPLORE,
    ORDERLY_SEARCH,
    DIALOGUE,
    CHOICE,
    BATTLE,
    POST_BATTLE_CUTSCENE,
    ENDING_VEGETABLE,
    ENDING_ESCAPE
}

enum class BattlePhase {
    INTRO_FLASH,
    MENU,
    ATTACK_BAR,
    SLASH_ANIM,
    ENEMY_DIALOGUE,
    DODGE,
    VICTORY,
    DEFEAT
}

enum class DoorAction {
    KNOCK,
    BANG,
    SCREAM
}

data class DialogueLine(
    val name: String = "",
    val portrait: String? = null,
    val voice: String = "narrator",
    val text: String = "",
    val slow: Boolean = false
)

data class GameChoice(
    val id: String,
    val label: String
)

data class Bullet(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val w: Float,
    val h: Float,
    val type: String // "syringe", "pill", "key"
)

data class PlayerState(
    var x: Float = 300f,
    var y: Float = 200f,
    val w: Float = 28f,
    val h: Float = 36f,
    val speed: Float = 3.2f,
    var dir: String = "down",
    var moving: Boolean = false,
    var animFrame: Int = 0,
    var hasCoat: Boolean = false
)

data class OrderlyState(
    var x: Float = 304f,
    var y: Float = 330f,
    val w: Float = 32f,
    val h: Float = 40f,
    var dir: String = "up",
    var state: String = "outside", // "outside", "window", "entering", "searching", "stripped_locked", "injecting", "dragging"
    var searchTimer: Int = 0,
    var targetX: Float = 220f,
    var targetY: Float = 165f,
    var hasCoat: Boolean = true
)

data class BattleState(
    var phase: BattlePhase = BattlePhase.MENU,
    var turnCount: Int = 0,
    var enemyHP: Int = 100,
    val enemyMaxHP: Int = 100,
    var enemyDefDown: Boolean = false,
    var enemyShake: Int = 0,
    var lastDamage: Int = 0,
    var slashTimer: Int = 0,
    var barCursor: Float = 0f,
    var barDir: Float = 1f,
    var pillsLeft: Int = 2,
    var soulX: Float = 312f,
    var soulY: Float = 255f,
    val soulSize: Float = 16f,
    val soulSpeed: Float = 3.4f,
    var soulInvuln: Int = 0,
    var waveTimer: Int = 270,
    val bullets: MutableList<Bullet> = mutableListOf()
)
