extends SceneTree

const MainScene = preload("res://scenes/main.tscn")

func _initialize() -> void:
    call_deferred("_run")

func _run() -> void:
    var game = MainScene.instantiate()
    root.add_child(game)
    await process_frame
    if game.player.camera.position.x < 0.9:
        printerr("VISUAL_LAYOUT_FAIL: camera must use over-shoulder framing")
        quit(5)
        return
    var decor = game.world.get_node_or_null("CinematicSetDressing")
    if decor == null or decor.get_child_count() < 30:
        printerr("VISUAL_LAYOUT_FAIL: cinematic set dressing missing or incomplete")
        quit(6)
        return
    if game.world.tiles_clean.uv1_scale.x < 2.0:
        printerr("VISUAL_LAYOUT_FAIL: floor tile scale is too coarse")
        quit(7)
        return
    for state in [0, 1, 2]:
        game.apply_reality(state)
        await process_frame
        if game.world.reality != state:
            printerr("VISUAL_LAYOUT_FAIL: reality state not propagated")
            quit(8)
            return
    print("VISUAL_LAYOUT_OK: over-shoulder camera, architectural dressing, scaled wet tiles, all states")
    quit(0)
