extends Node

# Visual QA of real game rendering. Start with Godot in graphical mode:
# godot --path PROJECT res://tests/visual_capture.tscn
func _ready() -> void:
    call_deferred("_capture")

func _capture() -> void:
    var game = load("res://scenes/main.tscn").instantiate()
    add_child(game)
    for state in [0, 1, 2]:
        if state != 0:
            game.apply_reality(state)
        await get_tree().create_timer(1.3).timeout
        await RenderingServer.frame_post_draw
        var img = get_viewport().get_texture().get_image()
        var path = "/tmp/durka-real-game-%d.png" % state
        var err = img.save_png(path)
        print("VISUAL_CAPTURE state=", state, " result=", err,
              " pixels=", img.get_width(), "x", img.get_height(), " path=", path)
    get_tree().quit()
