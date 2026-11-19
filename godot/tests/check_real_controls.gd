extends SceneTree

var game
var choice := ""

func _initialize() -> void:
    call_deferred("exercise")

func check(ok: bool, message: String) -> void:
    if not ok:
        push_error(choice + ": " + message)
        quit(1)
        await process_frame

func tap() -> void:
    Input.action_press("interact")
    await process_frame
    await process_frame
    Input.action_release("interact")
    await process_frame
    await process_frame

func walk(x: float, z: float) -> void:
    var goal := Vector2(x,z)
    for i in 1000:
        var delta := goal - Vector2(game.player.position.x, game.player.position.z)
        if delta.length() < 0.12:
            break
        var yaw := atan2(-delta.x,-delta.y)
        var motion := InputEventScreenDrag.new()
        motion.position.x = 1200
        motion.relative.x = -angle_difference(game.player.yaw,yaw)/(1.6*game.player.sensitivity)
        game.player._unhandled_input(motion)
        Input.action_press("move_forward")
        await physics_frame
    Input.action_release("move_forward")
    await process_frame
    await check(Vector2(game.player.position.x,game.player.position.z).distance_to(goal)<0.25, "Blocked on route to %s at %s" % [goal,game.player.position])

func exercise() -> void:
    for ending in ["hold","leave"]:
        choice = ending
        game = load("res://scenes/main.tscn").instantiate()
        root.add_child(game)
        await process_frame
        await tap()
        await walk(-4.6,-1)
        await tap()
        await create_timer(0.43).timeout
        await tap()
        await check(game.run.echo_established,"early knocks")
        await walk(-1,-4.9)
        await tap()
        await check(game.run.window_closed,"window")
        await walk(-2,-4)
        await walk(-2,0)
        await walk(0,0)
        await check(game.world.reality == 1,"abandoned ward")
        await walk(0,6)
        for i in 3:
            await walk(0,26.4)
            await create_timer(2.2).timeout
            await tap()
        await check(game.run.handle_owned and game.run.far_door_attempts == 3,"slow opens")
        await create_timer(4).timeout
        for version in [1,2,0]:
            game.apply_reality(version)
            await check(game.run.handle_owned,"persistent handle")
        await walk(0,19.85)
        await walk(1.15,20)
        await tap()
        await check(game.run.handle_installed,"install")
        await walk(0,4.9)
        await create_timer(6.1).timeout
        await check(game.run.chapter == game.run.Chapter.FINALE,"return")
        if ending == "hold":
            await walk(0,19.85)
            await walk(1.15,20)
            Input.action_press("interact")
            await create_timer(2.3).timeout
            Input.action_release("interact")
        else:
            await walk(0,10)
            await tap()
        await check(game.run.ending == ending,"ending")
        await create_timer(3.7).timeout
        await check(game.hud.ending_panel.visible,"replay screen")
        game.queue_free()
        await process_frame
        await process_frame
    print("INTERACTION_TESTS_OK")
    quit()
