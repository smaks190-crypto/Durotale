extends SceneTree

# A visual checkpoint, not a proof of quality or a gameplay test.
# Run under an X server (xvfb-run for CI). Headless rendering has no image.
func _initialize() -> void:
    call_deferred("capture")

func capture() -> void:
    var game: Node = load("res://scenes/main.tscn").instantiate()
    root.add_child(game)
    await process_frame
    game.player.paused = true
    var base: String = ProjectSettings.globalize_path("res://build/qa")
    var mkdir_error := DirAccess.make_dir_recursive_absolute(base)
    if mkdir_error != OK:
        printerr("CAPTURE_FAILED: cannot create directory, ", mkdir_error)
        quit(2)
        return
    var saved := 0
    for reality in [0, 1, 2]:
        game.player.global_position = Vector3(0.7, 0.12, 0.7)
        game.player.yaw = 0.0
        game.player.pitch = -0.05
        game.player.apply_orientation()
        game.apply_reality(reality)
        for i in 12:
            await process_frame
        var img := root.get_texture().get_image()
        if img == null or img.get_width() < 100:
            printerr("CAPTURE_FAILED: invalid viewport")
            quit(3)
            return
        var file: String = base + "/ward_%d.png" % reality
        var err := img.save_png(file)
        if err != OK:
            printerr("CAPTURE_FAILED: PNG write ", err)
            quit(4)
            return
        saved += 1
        print("VISUAL_PREVIEW: ", file, " ", img.get_width(), "x", img.get_height())
        if reality == 1:
            img.resize(560, 315, Image.INTERPOLATE_BILINEAR)
            print("VISUAL_PREVIEW_JPG64:", Marshalls.raw_to_base64(img.save_jpg_to_buffer(0.58)))
    game.queue_free()
    print("VISUAL_PREVIEW_OK: ", saved, " states captured")
    quit(0)
