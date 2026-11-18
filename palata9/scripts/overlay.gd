extends Control
class_name GameOverlay
## Один CanvasItem HUD. Данные поступают из PalataGame без прямой связи с миром.

const WHITE = Color("f2f5f4")
const DIM = Color("8a97a0")
const TEAL = Color("63d1c3")
const RED = Color("ff2b45")
const YELLOW = Color("ffd83d")
const ORANGE = Color("ff9d2e")
const STEEL = Color("9fb4c8")
const GREEN = Color("45d97e")
const DARK = Color("05070a")
const SURFACE = Color("0d1418")
const BORDER = Color("2e4a44")

var state: Dictionary = {}

func set_state(value: Dictionary) -> void:
    state = value
    queue_redraw()

func _font() -> Font:
    return ThemeDB.fallback_font

func _text(msg: String, x: float, y: float, size: int = 18, color: Color = WHITE, align: HorizontalAlignment = HORIZONTAL_ALIGNMENT_LEFT) -> void:
    var length: float = -1.0
    if align == HORIZONTAL_ALIGNMENT_CENTER:
        length = 640.0
        x = 0.0
    draw_string(_font(), Vector2(x, y), msg, align, length, size, color)

func _box(rect: Rect2, border: Color = WHITE, fill: Color = DARK) -> void:
    draw_rect(rect, fill)
    draw_rect(rect, border, false, 3.0)

func _wrap(msg: String, width: float, size: int) -> Array[String]:
    var rows: Array[String] = []
    var line := ""
    for word in msg.split(" "):
        var trial: String = word if line.is_empty() else line + " " + word
        if _font().get_string_size(trial, HORIZONTAL_ALIGNMENT_LEFT, -1, size).x > width and not line.is_empty():
            rows.append(line)
            line = word
        else:
            line = trial
    if not line.is_empty():
        rows.append(line)
    return rows

func _heart(p: Vector2, scale: float = 1.0, color: Color = RED) -> void:
    var pattern: Array[String] = [".XX..XX.", "XXXXXXXX", "XXXXXXXX", "XXXXXXXX", ".XXXXXX.", "..XXXX..", "...XX..."]
    var size: float = 2.0 * scale
    for y in range(pattern.size()):
        for x in range(pattern[y].length()):
            if pattern[y].substr(x, 1) == "X":
                draw_rect(Rect2(p.x + x * size - 8.0 * scale, p.y + y * size - 7.0 * scale, size, size), color)

func _draw() -> void:
    if state.is_empty():
        return
    var phase: String = str(state.get("phase", "title"))
    if phase == "title":
        _title()
    elif phase == "gallery":
        _gallery()
    elif phase in ["room", "door_menu", "bang"] or (phase == "dialogue" and state.get("dialogue_origin", "room") == "room"):
        _room_hud()
        if phase == "door_menu": _door_menu()
        if phase == "bang": _bang()
    elif phase.begins_with("battle") or (phase == "dialogue" and state.get("dialogue_origin", "room") == "battle"):
        _battle()
    elif phase == "ending":
        _ending()
    elif phase == "death":
        _death()
    if phase == "dialogue":
        _dialogue()

func _title() -> void:
    draw_rect(Rect2(0, 0, 640, 480), DARK)
    draw_rect(Rect2(262, 150, 116, 196), Color("101820"))
    draw_rect(Rect2(270, 157, 100, 180), Color("18242e"))
    draw_rect(Rect2(301, 197, 39, 27), Color("b3273c"))
    _text("9", 312, 218, 24, WHITE)
    _text("ПАЛАТА №9", 0, 92, 40, WHITE, HORIZONTAL_ALIGNMENT_CENTER)
    _text("тихая история о громкой двери", 0, 125, 18, DIM, HORIZONTAL_ALIGNMENT_CENTER)
    var diff := "ТИХАЯ" if state.get("difficulty") == "quiet" else "ОБЫЧНАЯ"
    var count: int = int(state.get("endings_count", 0))
    var items: Array[String] = ["НАЧАТЬ ИГРУ", "СЛОЖНОСТЬ: " + diff, "КОНЦОВКИ: %d/4" % count]
    for i in range(items.size()):
        var y: float = 360.0 + i * 34.0
        if i == int(state.get("title_index", 0)):
            _heart(Vector2(156, y - 5), 1.25)
        _text(items[i], 182, y, 20, YELLOW if i == int(state.get("title_index", 0)) else WHITE)
    _text("СТРЕЛКИ / WASD  ·  Z / ENTER — ВЫБОР", 0, 467, 13, TEAL, HORIZONTAL_ALIGNMENT_CENTER)

func _gallery() -> void:
    draw_rect(Rect2(0, 0, 640, 480), DARK)
    _text("ОТКРЫТЫЕ КОНЦОВКИ", 0, 45, 26, WHITE, HORIZONTAL_ALIGNMENT_CENTER)
    _text("%d из 4" % int(state.get("endings_count", 0)), 0, 73, 16, DIM, HORIZONTAL_ALIGNMENT_CENTER)
    var orders: Array = state.get("end_order", [])
    var titles: Dictionary = state.get("ending_titles", {})
    var unlocked: Dictionary = state.get("endings", {})
    for i in range(orders.size()):
        var id: String = str(orders[i])
        var owned: bool = bool(unlocked.get(id, false))
        var y: float = 90.0 + i * 84.0
        _box(Rect2(60, y, 520, 72), TEAL if owned else BORDER, SURFACE)
        _text(str(titles.get(id, "???")) if owned else "???", 90, y + 29, 24, YELLOW if owned else DIM)
        _text("НАЙДЕНА" if owned else ("СЕКРЕТ" if id == "secret" else "НЕ ОТКРЫТА"), 90, y + 55, 15, DIM)
    _text("X / ENTER — НАЗАД", 0, 455, 16, TEAL, HORIZONTAL_ALIGNMENT_CENTER)

func _room_hud() -> void:
    _box(Rect2(14, 10, 244, 43), BORDER, Color("071015"))
    _text("ПАЛАТА №9", 27, 38, 24, TEAL)
    var phase: String = str(state.get("phase"))
    if phase == "room":
        var near: String = str(state.get("near", ""))
        var label: String = "WASD / СТРЕЛКИ · ДВИЖЕНИЕ" if near.is_empty() else "Z / ● · " + ({"door": "ДВЕРЬ", "paper": "ЗАПИСКА", "bed": "КРОВАТЬ", "poster": "РАСПОРЯДОК", "window": "ОКНО", "scratches": "ЦАРАПИНЫ"}.get(near, "ОСМОТРЕТЬ"))
        draw_rect(Rect2(178, 448, 291, 29), Color("080d10", 0.92))
        _text(label, 183, 468, 15, TEAL)

func _door_menu() -> void:
    draw_rect(Rect2(0, 0, 640, 480), Color(0.0, 0.0, 0.0, 0.68))
    _box(Rect2(80, 87, 480, 315))
    _text("* Дверь заперта. За ней — шаги.", 101, 120, 20)
    _text("Что делать?", 101, 147, 17, DIM)
    var opts: Array = state.get("door_options", [])
    for i in range(opts.size()):
        var y: float = 186.0 + i * 48.0
        var chosen: bool = i == int(state.get("menu_index", 0))
        if chosen: _heart(Vector2(114, y - 7), 1.0)
        _text(str(opts[i]), 146, y + 2, 19, (RED if i == 3 else YELLOW) if chosen else WHITE)
    _text("↑↓ — ВЫБОР   Z — ДЕЙСТВИЕ   X — НАЗАД", 101, 385, 14, DIM)

func _bang() -> void:
    _box(Rect2(89, 283, 462, 138), RED)
    _text("КОЛОТИ В ДВЕРЬ!", 0, 312, 24, RED, HORIZONTAL_ALIGNMENT_CENTER)
    var n: int = int(state.get("door_strikes", 0))
    for i in range(4):
        _box(Rect2(226 + i * 52, 328, 36, 35), RED, RED if i < n else DARK)
    _text("УДАРЫ: %d / 4" % n, 0, 387, 19, WHITE, HORIZONTAL_ALIGNMENT_CENTER)
    _text("Z / ● · 4 УДАРА БЕЗ ПАУЗ", 0, 411, 14, DIM, HORIZONTAL_ALIGNMENT_CENTER)

func _dialogue() -> void:
    _box(Rect2(24, 255, 592, 143))
    var pages: Array = state.get("dialogue_pages", [])
    var index: int = int(state.get("dialogue_page", 0))
    if index < pages.size():
        var rows := _wrap(str(pages[index]), 546, 20)
        for i in range(mini(3, rows.size())):
            _text(rows[i], 42, 292 + i * 30, 20)
    _text("Z / ENTER / ● — ДАЛЬШЕ   ▼", 339, 386, 14, TEAL)

func _battle() -> void:
    draw_rect(Rect2(0, 0, 640, 480), DARK)
    # Враг в стиле пиксельного рисунка (не ассет Undertale).
    draw_rect(Rect2(273, 58, 94, 111), Color("e9edf0"))
    draw_rect(Rect2(290, 42, 60, 43), Color("e6bd93"))
    draw_rect(Rect2(288, 36, 64, 17), Color("4a3628"))
    draw_rect(Rect2(297, 56, 8, 6), Color("20242c"))
    draw_rect(Rect2(334, 56, 8, 6), Color("20242c"))
    draw_rect(Rect2(314, 70, 14, 4), Color("c9906e"))
    draw_rect(Rect2(314, 93, 12, 16), TEAL)
    draw_rect(Rect2(284, 143, 24, 34), Color("c2ccd4"))
    draw_rect(Rect2(332, 143, 24, 34), Color("c2ccd4"))
    _text("САНИТАР БОРЯ", 20, 28, 22, YELLOW if int(state.get("mercy", 0)) >= 100 else WHITE)
    _text("ПОЩАДА: %d%%" % int(state.get("mercy", 0)), 20, 52, 16, YELLOW)
    _text("ХОД %d" % maxi(1, int(state.get("turns", 0))), 549, 29, 16, DIM)
    _box(Rect2(20, 220, 600, 166))
    var phase: String = str(state.get("phase", ""))
    if phase == "battle_menu":
        var rows := _wrap(str(state.get("battle_flavor", "* Боря ждёт.")), 530, 20)
        for i in range(mini(rows.size(), 3)):
            _text(rows[i], 42, 260 + i * 28, 20)
    elif phase == "battle_sub":
        _sub_menu()
    elif phase == "battle_attack":
        _attack_bar()
    elif phase == "battle_defend":
        _defend_box()
    draw_rect(Rect2(267, 395, 150, 14), Color("7f1020"))
    draw_rect(Rect2(267, 395, 150 * float(state.get("hp", 20)) / 20.0, 14), YELLOW)
    _text("ТЫ    LV 1", 20, 407, 19)
    _text("ОЗ %d/20" % int(state.get("hp", 20)), 424, 407, 18, WHITE)
    var opts: Array = state.get("battle_options", [])
    for i in range(opts.size()):
        var x: float = 10.0 + i * 158.0
        var chosen: bool = i == int(state.get("menu_index", 0)) and phase == "battle_menu"
        _box(Rect2(x, 421, 145, 41), ORANGE, ORANGE if chosen else DARK)
        _text(str(opts[i]), x + 9, 448, 16, DARK if chosen else ORANGE)
    if phase == "battle_defend":
        _text("УКЛОНЯЙСЯ! %.1f" % maxf(0.0, float(state.get("defend_time", 0.0))), 0, 206, 17, TEAL, HORIZONTAL_ALIGNMENT_CENTER)

func _sub_menu() -> void:
    var kind: String = str(state.get("sub_type", "act"))
    var labels: Array[String] = []
    if kind == "act":
        for name in state.get("act_options", []): labels.append(str(name))
    elif kind == "item":
        var its: Array = state.get("items", [1, 1])
        labels = ["Батончик ×%d" % int(its[0]), "Ромашковый чай ×%d" % int(its[1])]
    else:
        for name in state.get("mercy_options", []): labels.append(str(name))
    for i in range(labels.size()):
        var y: float = 252.0 + i * 31.0
        var selected: bool = i == int(state.get("sub_index", 0))
        if selected: _heart(Vector2(45, y - 7), 0.8)
        _text(labels[i], 73, y + 2, 19, YELLOW if selected else WHITE)
    _text("X — НАЗАД", 460, 369, 14, DIM)

func _attack_bar() -> void:
    _text("ЖМИ Z / ● В ЦЕНТРЕ!", 0, 259, 21, ORANGE, HORIZONTAL_ALIGNMENT_CENTER)
    var pos: float = float(state.get("timing_pos", 0.0))
    draw_rect(Rect2(60, 290, 520, 64), Color("232c33"))
    draw_rect(Rect2(294, 290, 52, 64), YELLOW)
    draw_rect(Rect2(60 + 520.0 * pos, 283, 5, 78), WHITE)

func _defend_box() -> void:
    draw_rect(Rect2(237, 228, 166, 158), WHITE, false, 3.0)
    var heart_pos: Vector2 = state.get("heart", Vector2(320, 308))
    for bullet in state.get("bullets", []):
        var p: Vector2 = bullet.get("pos", Vector2.ZERO)
        match str(bullet.get("kind", "")):
            "wall":
                var gap: float = float(bullet.get("gap", 308.0))
                draw_rect(Rect2(p.x - 5, 230, 10, gap - 24.0 - 230.0), RED)
                draw_rect(Rect2(p.x - 5, gap + 24.0, 10, 155.0 - (gap + 24.0 - 230.0)), RED)
            "pill":
                draw_rect(Rect2(p.x - 8, p.y - 5, 8, 10), RED)
                draw_rect(Rect2(p.x, p.y - 5, 8, 10), WHITE)
            "key":
                draw_rect(Rect2(p.x - 7, p.y - 3, 14, 6), YELLOW)
            _:
                draw_rect(Rect2(p.x - 3, p.y - 8, 6, 16), TEAL)
    if float(state.get("invulnerability", 0.0)) <= 0.0 or int(float(state.get("time", 0.0)) * 10.0) % 2 == 0:
        _heart(heart_pos, 1.0)

func _ending() -> void:
    draw_rect(Rect2(0, 0, 640, 480), DARK)
    var id: String = str(state.get("end_id", "good"))
    var titles: Dictionary = state.get("ending_titles", {})
    var col: Color = YELLOW if id == "good" else TEAL if id == "flee" else RED if id == "hard" else ORANGE
    _box(Rect2(70, 78, 500, 285), col)
    _heart(Vector2(320, 119), 3.0, col)
    _text("КОНЦОВКА", 0, 185, 19, col, HORIZONTAL_ALIGNMENT_CENTER)
    _text(str(titles.get(id, "ПРОЛОГ")), 0, 230, 27, WHITE, HORIZONTAL_ALIGNMENT_CENTER)
    var lines: Array[String] = []
    match id:
        "good": lines = ["* Ты проявил человечность.", "* Боря отвернулся. Дверь осталась открытой."]
        "flee": lines = ["* Коридор. Лестница. Дождь.", "* Боря не догонял. Может, и не хотел."]
        "hard": lines = ["* Ты вышел. Коридор оказался слишком тихим.", "* Что-то осталось в палате №9."]
        "secret": lines = ["* Боря и Вова пришли с ромашковым чаем.", "* Иногда побег не нужен, чтобы тебя услышали."]
    for i in range(lines.size()):
        _text(lines[i], 85, 284 + i * 31, 17, WHITE)
    _text("ХОДОВ: %d  ·  ПОПАДАНИЙ: %d" % [int(state.get("turns", 0)), int(state.get("hits", 0))], 0, 392, 15, DIM, HORIZONTAL_ALIGNMENT_CENTER)
    _text("Z / ● — ЕЩЁ РАЗ      X — МЕНЮ", 0, 445, 16, TEAL, HORIZONTAL_ALIGNMENT_CENTER)

func _death() -> void:
    draw_rect(Rect2(0, 0, 640, 480), Color.BLACK)
    _heart(Vector2(320, 110), 5.0)
    _text("НЕ СДАВАЙСЯ!", 0, 245, 34, WHITE, HORIZONTAL_ALIGNMENT_CENTER)
    _text("Боря ждёт. Дверь тоже.", 0, 294, 20, DIM, HORIZONTAL_ALIGNMENT_CENTER)
    _text("Z / ● — ПОВТОРИТЬ     X — МЕНЮ", 0, 409, 16, TEAL, HORIZONTAL_ALIGNMENT_CENTER)
