extends SceneTree
## Запуск: godot --headless --path . --script res://tests/smoke_test.gd

func _initialize() -> void:
    call_deferred("_run")

func _assert_ok(value: bool, message: String) -> bool:
    if value:
        print("PASS: " + message)
        return true
    push_error("FAIL: " + message)
    return false

func _finish_dialogue(game: PalataGame) -> void:
    var guard := 0
    while game.phase == "dialogue" and guard < 20:
        game._advance_dialogue()
        guard += 1

func _run() -> void:
    var scene: PackedScene = load("res://scenes/main.tscn")
    if scene == null:
        push_error("Cannot load main scene")
        quit(1)
        return
    var game: PalataGame = scene.instantiate() as PalataGame
    root.add_child(game)
    await process_frame
    var okay := _assert_ok(game.phase == "title", "Title screen appears")
    okay = _assert_ok(game.get_node_or_null("World") != null, "Room exists") and okay
    okay = _assert_ok(game.get_node_or_null("HUD/TouchControls") != null, "Mobile controls exist") and okay
    game._start_room()
    _finish_dialogue(game)
    okay = _assert_ok(game.phase == "room", "Opening dialogue closes") and okay
    game.player.position = Vector2(236, 413)
    game._interact()
    okay = _assert_ok(game.phase == "dialogue", "Note interaction works") and okay
    _finish_dialogue(game)
    game.player.position = Vector2(320, 207)
    game._interact()
    okay = _assert_ok(game.phase == "door_menu", "Door offers choices") and okay
    okay = _assert_ok(game.DOOR_OPTIONS.size() == 4, "Four door choices") and okay
    game._select_door(3)
    for i in range(4): game._bang()
    okay = _assert_ok(game.phase == "ending" and game.final_id == "secret", "Four hits unlock secret tea ending") and okay
    game._start_battle()
    _finish_dialogue(game)
    okay = _assert_ok(game.phase == "battle_menu", "Battle starts") and okay
    for index in [0, 1, 2, 3, 0, 1]:
        game.menu_index = 1
        game._battle_choose()
        game.sub_index = index
        game._battle_sub_choose()
        _finish_dialogue(game)
        game.phase = "battle_menu" # Quickly end dodge phase in this targeted story test.
    okay = _assert_ok(game.mercy == 100, "ACT actions reach 100 mercy with repeat modifiers") and okay
    game.menu_index = 3
    game._battle_choose()
    game.sub_index = 0
    game._battle_sub_choose()
    okay = _assert_ok(game.phase == "ending" and game.final_id == "good", "Nonviolent ending works") and okay
    game._start_battle()
    _finish_dialogue(game)
    game.mercy = 65
    game.menu_index = 3
    game._battle_choose()
    game.sub_index = 1
    game._battle_sub_choose()
    okay = _assert_ok(game.phase == "ending" and game.final_id == "flee", "Escape ending works") and okay
    game._start_battle()
    _finish_dialogue(game)
    game.enemy_hp = 4
    game.phase = "battle_attack"
    game.timing_pos = 0.5
    game._strike(true)
    okay = _assert_ok(game.phase == "ending" and game.final_id == "hard", "Combat ending works") and okay
    game._start_battle()
    _finish_dialogue(game)
    game.hp = 3
    game._enemy_turn()
    game.bullets.append({"kind": "syr", "pos": game.heart, "vel": Vector2.ZERO})
    game._defend(0.01)
    okay = _assert_ok(game.phase == "death", "Damage causes loss at 0 HP") and okay
    if okay:
        print("PALATA9_SMOKE_TEST_PASSED")
        quit(0)
    else:
        quit(1)
