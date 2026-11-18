extends Node2D
class_name PalataGame
## ПАЛАТА №9. Нативный перенос базовых игровых систем из авторского HTML-пролога.
## Игровая логика отделена от рендера мира, интерфейса и сенсорного ввода.

@onready var world: WardWorld = $World
@onready var player: WardPlayer = $Player
@onready var overlay: GameOverlay = $HUD/Overlay
@onready var touch_controls: MobileControls = $HUD/TouchControls

const INTERACT_DISTANCE: float = 56.0
const OBJECTS: Dictionary = {
    "door": Vector2(320, 207),
    "window": Vector2(486, 207),
    "poster": Vector2(151, 203),
    "scratches": Vector2(131, 268),
    "bed": Vector2(233, 379),
    "paper": Vector2(236, 413),
}
const END_ORDER: Array[String] = ["good", "flee", "hard", "secret"]
const END_TITLES: Dictionary = {
    "good": "ЧЕЛОВЕЧНОСТЬ", "flee": "ЛОВКИЙ ПОБЕГ",
    "hard": "ТИХИЙ КОРИДОР", "secret": "РОМАШКОВЫЙ РЕЖИМ",
}
const DOOR_OPTIONS: Array[String] = [
    "Кричать о помощи", "Лечь и ждать", "Сказать: «Мне плохо…»", "КОЛОТИТЬ В ДВЕРЬ!",
]
const BATTLE_OPTIONS: Array[String] = ["АТАКА", "ДЕЙСТВИЕ", "ПРЕДМЕТ", "ПОЩАДА"]
const ACT_OPTIONS: Array[String] = ["Поговорить", "Извиниться", "Попросить воды", "Осмотреть"]
const MERCY_OPTIONS: Array[String] = ["Пощадить", "Сбежать"]

var phase: String = "title"
var difficulty: String = "normal"
var title_index: int = 0
var menu_index: int = 0
var sub_index: int = 0
var sub_type: String = ""
var dialogue_pages: Array[String] = []
var dialogue_page: int = 0
var dialogue_origin: String = "room"
var dialogue_done: Callable = Callable()
var seen_objects: Dictionary = {}
var endings: Dictionary = {}
var door_strikes: int = 0
var door_strike_timer: float = 0.0
var game_time: float = 0.0

var hp: int = 20
var enemy_hp: int = 60
var mercy: int = 0
var turns: int = 0
var hits: int = 0
var items: Array[int] = [1, 1]
var act_used: Array[bool] = [false, false, false, false]
var timing_pos: float = 0.0
var defend_time: float = 0.0
var defend_spawn_time: float = 0.0
var invulnerability: float = 0.0
var heart: Vector2 = Vector2(320, 308)
var bullets: Array[Dictionary] = []
var battle_flavor: String = "* Боря преграждает путь."
var final_id: String = ""

func _ready() -> void:
    _setup_inputs()
    _load_progress()
    _update_ui()

func _setup_inputs() -> void:
    for action in ["palata_action", "palata_cancel", "mobile_action", "mobile_restart"]:
        if not InputMap.has_action(action):
            InputMap.add_action(action)
    var keys: Dictionary = {
        "palata_action": [KEY_Z], "palata_cancel": [KEY_X],
        "ui_up": [KEY_W], "ui_down": [KEY_S],
        "ui_left": [KEY_A], "ui_right": [KEY_D],
    }
    for action in keys.keys():
        for key_code in keys[action]:
            var event := InputEventKey.new()
            event.physical_keycode = key_code
            if not InputMap.action_has_event(action, event):
                InputMap.action_add_event(action, event)

func _load_progress() -> void:
    var save := ConfigFile.new()
    if save.load("user://palata9_progress.cfg") == OK:
        difficulty = str(save.get_value("game", "difficulty", "normal"))
        for id in END_ORDER:
            endings[id] = bool(save.get_value("endings", id, false))

func _save_progress() -> void:
    var save := ConfigFile.new()
    save.set_value("game", "difficulty", difficulty)
    for id in END_ORDER:
        save.set_value("endings", id, endings.get(id, false))
    var err := save.save("user://palata9_progress.cfg")
    if err != OK:
        push_warning("Unable to save progress: %d" % err)

func _process(delta: float) -> void:
    game_time += delta
    player.can_move = phase == "room"
    world.visible = phase in ["room", "door_menu", "bang"] or (phase == "dialogue" and dialogue_origin == "room")
    player.visible = world.visible
    touch_controls.update_phase(phase)
    if phase == "bang" and door_strikes > 0:
        door_strike_timer -= delta
        if door_strike_timer <= 0.0:
            door_strikes = 0
    if phase == "battle_attack":
        timing_pos = minf(timing_pos + delta / 1.15, 1.0)
        if timing_pos >= 1.0:
            _strike(false)
    elif phase == "battle_defend":
        _defend(delta)
    var accept: bool = Input.is_action_just_pressed("palata_action") or Input.is_action_just_pressed("ui_accept") or Input.is_action_just_pressed("mobile_action")
    var cancel: bool = Input.is_action_just_pressed("palata_cancel") or Input.is_action_just_pressed("ui_cancel")
    var up: bool = Input.is_action_just_pressed("ui_up")
    var down: bool = Input.is_action_just_pressed("ui_down")
    var left: bool = Input.is_action_just_pressed("ui_left")
    var right: bool = Input.is_action_just_pressed("ui_right")
    _handle_ui(accept, cancel, up, down, left, right)
    if Input.is_action_just_pressed("mobile_restart") and phase in ["ending", "death"]:
        _return_to_title()
    _update_ui()

func _handle_ui(accept: bool, cancel: bool, up: bool, down: bool, left: bool, right: bool) -> void:
    match phase:
        "title":
            if up: title_index = posmod(title_index - 1, 3)
            if down: title_index = posmod(title_index + 1, 3)
            if accept:
                if title_index == 0: _start_room()
                elif title_index == 1:
                    difficulty = "quiet" if difficulty == "normal" else "normal"
                    _save_progress()
                else: phase = "gallery"
        "gallery":
            if accept or cancel: phase = "title"
        "room":
            if accept: _interact()
        "dialogue":
            if accept or cancel: _advance_dialogue()
        "door_menu":
            if up: menu_index = posmod(menu_index - 1, DOOR_OPTIONS.size())
            if down: menu_index = posmod(menu_index + 1, DOOR_OPTIONS.size())
            if cancel: phase = "room"
            elif accept: _select_door(menu_index)
        "bang":
            if cancel: phase = "room"
            elif accept: _bang()
        "battle_menu":
            if left: menu_index = posmod(menu_index - 1, 4)
            if right: menu_index = posmod(menu_index + 1, 4)
            if accept: _battle_choose()
        "battle_sub":
            var count: int = 4 if sub_type == "act" else 2
            if up or left: sub_index = posmod(sub_index - 1, count)
            if down or right: sub_index = posmod(sub_index + 1, count)
            if cancel: phase = "battle_menu"
            elif accept: _battle_sub_choose()
        "battle_attack":
            if accept: _strike(true)
        "ending", "death":
            if accept:
                if phase == "ending": _start_room()
                else: _start_battle()
            elif cancel: _return_to_title()

func _start_room() -> void:
    phase = "room"
    dialogue_origin = "room"
    seen_objects.clear()
    door_strikes = 0
    world.door_open = false
    player.position = Vector2(320, 310)
    player.velocity = Vector2.ZERO
    _say(["* ПАЛАТА №9. Третья ночь.", "* Пахнет хлоркой. За дверью слышны шаги.", "* У кровати лежит записка. Решение за тобой."])

func _return_to_title() -> void:
    phase = "title"
    title_index = 0

func _nearest_object() -> String:
    var nearest := ""
    var distance: float = INTERACT_DISTANCE
    for id in OBJECTS.keys():
        var d: float = player.position.distance_to(OBJECTS[id])
        if d < distance:
            distance = d
            nearest = id
    return nearest

func _interact() -> void:
    var id := _nearest_object()
    match id:
        "door":
            menu_index = 0
            phase = "door_menu"
        "bed":
            _object_dialogue(id, ["* Кровать помнит форму чужой усталости. Пружина поёт ноту «ля».", "* Нет. Спать — это сдаться."])
        "window":
            _object_dialogue(id, ["* Решётка. Дождь. Фонарь за окном — как одинокая звезда.", "* До свободы три этажа. Прыгать — плохой план."])
        "poster":
            _object_dialogue(id, ["* РЕЖИМ ДНЯ: 07:00 подъём, 08:00 каша, 22:00 отбой.", "* Пункт «побег» зачёркнут. Дважды."])
        "scratches":
            _object_dialogue(id, ["* На стене нацарапано: «ЧЕТЫРЕ УДАРА. БЕЗ ПАУЗ».", "* Ниже: «Это не побег. Это лучше. Или хуже»."])
        "paper":
            _object_dialogue(id, ["* Записка: «Они жалеют слабых. Сыграй слабого».", "* В углу крошечное красное сердечко."])
        _:
            _say(["* Старая лампа гудит. Дверь молчит."])

func _object_dialogue(id: String, texts: Array[String]) -> void:
    var count: int = int(seen_objects.get(id, 0))
    seen_objects[id] = count + 1
    _say([texts[mini(count, texts.size() - 1)]])

func _say(texts: Array[String], done: Callable = Callable()) -> void:
    dialogue_origin = "battle" if phase.begins_with("battle") or (phase == "dialogue" and dialogue_origin == "battle") else "room"
    dialogue_pages = texts.duplicate()
    dialogue_page = 0
    dialogue_done = done
    phase = "dialogue"

func _advance_dialogue() -> void:
    dialogue_page += 1
    if dialogue_page < dialogue_pages.size():
        return
    var done := dialogue_done
    dialogue_done = Callable()
    phase = "battle_menu" if dialogue_origin == "battle" else "room"
    if done.is_valid():
        done.call()

func _select_door(index: int) -> void:
    phase = "room"
    match index:
        0: _say(["* Ты закричал. Эхо вежливо вернуло крик обратно.", "* Никто не пришёл."])
        1: _say(["* Ты лёг на кровать. Пружина скрипнула.", "* За дверью скучают. Это ничего не меняет."])
        2: _say(["* Тук… тук… Ты стучишь тихо, словно извиняешься.", "БОРЯ: Девятый? Что тебе?", "ТЫ: Мне плохо. Кажется, я сейчас упаду…", "БОРЯ: Только без фокусов, слышишь?", "* Щёлкнул замок. Боря вошёл.", "БОРЯ: Стоп. Ты же стоишь!"], Callable(self, "_start_battle"))
        3:
            door_strikes = 0
            door_strike_timer = 0.0
            phase = "bang"

func _bang() -> void:
    if door_strikes == 0:
        door_strike_timer = 1.4
    door_strikes += 1
    if door_strikes >= 4:
        world.door_open = true
        _finish("secret")

func _start_battle() -> void:
    phase = "battle_menu"
    dialogue_origin = "battle"
    hp = 20
    enemy_hp = 60
    mercy = 0
    turns = 0
    hits = 0
    menu_index = 0
    items = [1, 1]
    act_used = [false, false, false, false]
    bullets.clear()
    battle_flavor = "* Боря преграждает путь."
    _say(["* САНИТАР БОРЯ преграждает путь!", "* Не обязательно причинять ему вред."], Callable(self, "_return_to_battle_menu"))

func _return_to_battle_menu() -> void:
    phase = "battle_menu"
    battle_flavor = "* Боря переступает с ноги на ногу."

func _battle_choose() -> void:
    if menu_index == 0:
        phase = "battle_attack"
        timing_pos = 0.0
        return
    sub_index = 0
    sub_type = ["", "act", "item", "mercy"][menu_index]
    phase = "battle_sub"

func _battle_sub_choose() -> void:
    match sub_type:
        "act":
            var gains: Array[int] = [20, 35, 15, 10]
            var repeat_gains: Array[int] = [10, 10, 5, 0]
            var first: bool = not act_used[sub_index]
            var m: int = gains[sub_index] if first else repeat_gains[sub_index]
            act_used[sub_index] = true
            mercy = mini(100, mercy + m)
            var texts: Array[String] = []
            match sub_index:
                0: texts = ["ТЫ: Тяжёлая смена?", "БОРЯ: Двенадцать часов. Ноги гудят."] if first else ["* Вы поговорили о дожде. Дождь одобряет."]
                1: texts = ["ТЫ: Прости. Обман был нечестным.", "БОРЯ: …Ладно. Бывает."] if first else ["* Ещё одно извинение. Боря кивает."]
                2:
                    hp = mini(20, hp + (4 if first else 2))
                    texts = ["* Ты попросил воды. Боря протянул фляжку.", "* ОЗ восстановлены."]
                3: texts = ["* БОРЯ — АТК 6, ЗЩТ 2.", "* Добрый под слоем халата."] if first else ["* Всё тот же Боря. Халат глажен."]
            texts.append("* ПОЩАДА +%d%% (итого %d%%)." % [m, mercy])
            if mercy == 100: texts.append("* Имя Бори стало ЖЁЛТЫМ. Его можно пощадить.")
            _say(texts, Callable(self, "_enemy_turn"))
        "item":
            if items[sub_index] <= 0:
                _say(["* Пусто. Осталась только решимость."], Callable(self, "_return_to_battle_menu"))
                return
            items[sub_index] -= 1
            var heal: int = 10 if sub_index == 0 else 8
            hp = mini(20, hp + heal)
            _say(["* Ты использовал %s. +%d ОЗ." % ["батончик" if sub_index == 0 else "ромашковый чай", heal]], Callable(self, "_enemy_turn"))
        "mercy":
            if sub_index == 0:
                if mercy >= 100: _finish("good")
                else: _say(["* Боря ещё настороже. Он не готов тебя отпустить."], Callable(self, "_enemy_turn"))
            else:
                if mercy >= 60: _finish("flee")
                else: _say(["БОРЯ: Не в этот раз.", "* Боря перекрыл дверь плечом."], Callable(self, "_enemy_turn"))

func _strike(on_time: bool) -> void:
    if phase != "battle_attack":
        return
    var accuracy: float = 1.0 - absf(timing_pos - 0.5) * 2.0 if on_time else 0.0
    var damage: int = roundi(6.0 + 14.0 * pow(accuracy, 1.5)) if on_time else 0
    enemy_hp = maxi(0, enemy_hp - damage)
    if enemy_hp <= 0:
        _finish("hard")
    else:
        _say(["* Ты нанёс %d урона." % damage], Callable(self, "_enemy_turn"))

func _enemy_turn() -> void:
    turns += 1
    defend_time = 6.5
    defend_spawn_time = 0.10
    invulnerability = 0.0
    heart = Vector2(320, 308)
    bullets.clear()
    phase = "battle_defend"

func _defend(delta: float) -> void:
    defend_time -= delta
    invulnerability = maxf(0.0, invulnerability - delta)
    var direction := Input.get_vector("ui_left", "ui_right", "ui_up", "ui_down")
    heart += direction * 175.0 * delta
    heart.x = clampf(heart.x, 250.0, 390.0)
    heart.y = clampf(heart.y, 239.0, 377.0)
    defend_spawn_time -= delta
    if defend_spawn_time <= 0.0:
        _spawn_hazard()
        defend_spawn_time = 0.55 + 0.5 * float(mercy) / 100.0
    for i in range(bullets.size() - 1, -1, -1):
        var bullet: Dictionary = bullets[i]
        bullet["pos"] = bullet["pos"] + bullet["vel"] * delta
        var p: Vector2 = bullet["pos"]
        var kind: String = str(bullet["kind"])
        if kind == "pill":
            var v: Vector2 = bullet["vel"]
            if p.x < 251.0 or p.x > 389.0: v.x = -v.x
            if p.y < 240.0 or p.y > 376.0: v.y = -v.y
            bullet["vel"] = v
        var collided: bool = false
        if kind == "wall":
            var gap: float = float(bullet["gap"])
            collided = absf(heart.x - p.x) < 12.0 and absf(heart.y - gap) > 23.0
        else:
            collided = heart.distance_to(p) < 12.0
        if collided and invulnerability <= 0.0:
            hp = maxi(0, hp - (2 if difficulty == "quiet" else 3))
            hits += 1
            invulnerability = 0.85
            if hp <= 0:
                phase = "death"
                bullets.clear()
                return
        if p.y > 410.0 or p.y < 190.0 or p.x < 200.0 or p.x > 440.0:
            if kind != "pill":
                bullets.remove_at(i)
    if defend_time <= 0.0:
        bullets.clear()
        phase = "battle_menu"
        battle_flavor = "* Боря старается не попадать." if mercy >= 60 else "* Пахнет хлоркой и решимостью."

func _spawn_hazard() -> void:
    var pattern: int = (turns - 1) % 4
    var origin: Vector2
    var velocity: Vector2
    var kind := "syr"
    match pattern:
        0:
            kind = "pill"
            origin = Vector2(randf_range(260.0, 379.0), 245.0)
            velocity = Vector2(randf_range(-65.0, 65.0), randf_range(85.0, 115.0))
        1:
            origin = Vector2(clampf(heart.x + randf_range(-35.0, 35.0), 252.0, 388.0), 232.0)
            velocity = Vector2(0.0, 122.0)
        2:
            kind = "wall"
            origin = Vector2(238.0, 308.0)
            velocity = Vector2(95.0, 0.0)
        3:
            kind = "key"
            origin = Vector2(320, 305)
            velocity = Vector2.from_angle(randf_range(0.0, TAU)) * 110.0
    bullets.append({"kind": kind, "pos": origin, "vel": velocity, "gap": randf_range(276.0, 342.0)})

func _finish(id: String) -> void:
    final_id = id
    endings[id] = true
    _save_progress()
    phase = "ending"
    bullets.clear()
    world.door_open = true

func _update_ui() -> void:
    var count: int = 0
    for id in END_ORDER:
        if endings.get(id, false): count += 1
    overlay.set_state({
        "phase": phase, "time": game_time, "difficulty": difficulty,
        "title_index": title_index, "menu_index": menu_index, "sub_index": sub_index,
        "sub_type": sub_type, "door_options": DOOR_OPTIONS,
        "battle_options": BATTLE_OPTIONS, "act_options": ACT_OPTIONS,
        "mercy_options": MERCY_OPTIONS, "dialogue_pages": dialogue_pages,
        "dialogue_page": dialogue_page, "dialogue_origin": dialogue_origin,
        "near": _nearest_object() if phase == "room" else "",
        "door_strikes": door_strikes, "door_strike_timer": door_strike_timer,
        "hp": hp, "enemy_hp": enemy_hp, "mercy": mercy,
        "turns": turns, "hits": hits, "items": items,
        "timing_pos": timing_pos, "defend_time": defend_time,
        "heart": heart, "bullets": bullets, "invulnerability": invulnerability,
        "battle_flavor": battle_flavor, "end_id": final_id,
        "ending_titles": END_TITLES, "end_order": END_ORDER,
        "endings": endings, "endings_count": count,
    })
