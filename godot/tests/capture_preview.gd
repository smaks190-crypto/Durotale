extends SceneTree

func _initialize() -> void:
    call_deferred("capture")

func capture() -> void:
    var game: Node = load("res://scenes/main.tscn").instantiate()
    root.add_child(game)
    await process_frame
    game.call("apply_reality", 1)
    for i in 35:
        await process_frame
    var img := root.get_texture().get_image()
    if img == null:
        printerr("CAPTURE_FAILED: image is null")
        quit(2)
        return
    var path := "/tmp/durka_ruined_preview.png"
    var err := img.save_png(path)
    print("CAPTURE_RESULT ",err," pixels=",img.get_width(),"x",img.get_height()," output=",path)
    quit(0 if err == OK else 3)
