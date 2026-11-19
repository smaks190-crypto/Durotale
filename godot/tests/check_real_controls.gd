extends SceneTree

const GameScene = preload("res://scenes/main.tscn")

func _initialize() -> void:
    call_deferred("_run")

func tap() -> void:
    Input.action_press("interact")
    await process_frame
    await process_frame
    Input.action_release("interact")
    await process_frame
    await process_frame

func place_at(game: Node, key: String) -> void:
    game.player.global_position = game.world.markers[key]["position"]
    game.player.velocity = Vector3.ZERO

func _run() -> void:
    var game = GameScene.instantiate()
    root.add_child(game)
    await process_frame
    game.player.paused = true
    # Two early knocks using the real E input
    place_at(game, "radiator")
    await tap()
    await tap()
    assert(game.run.echo_established, "Two early knocks must create an echo")
    # Closing a window must later trigger the abandoned state.
    place_at(game, "window")
    await tap()
    assert(game.run.window_closed)
    game.player.yaw += 1.1
    game.player.apply_orientation()
    await process_frame
    assert(game.run.reality == 1, "Window turn triggers abandoned room")
    # Slow door opening; never require rapid clicks.
    for i in 3:
        place_at(game,"far_door")
        await tap()
        if i < 2:
            await create_timer(1.9).timeout
    assert(game.run.handle_owned, "Slow opening should give handle")
    assert(game.run.far_door_attempts == 3)
    # One physical handle regardless of reality changes.
    for state in [0,1,2]:
        game.apply_reality(state)
        assert(game.run.handle_owned)
    place_at(game, "neighbor_door")
    await tap()
    assert(game.run.handle_installed, "Handle installs through E")
    # The changed game requires physically returning to the starting ward.
    place_at(game, "bed")
    await process_frame
    assert(game.run.room_revisited, "Returning to the ward must be detected")
    game.player.yaw += 1.2
    game.player.apply_orientation()
    await create_timer(1.9).timeout
    assert(game.run.chapter == game.run.Chapter.FINALE, "Finale is reachable")
    # Hold E to finish, no dialog choices.
    place_at(game, "neighbor_door")
    Input.action_press("interact")
    await create_timer(2.25).timeout
    Input.action_release("interact")
    await process_frame
    assert(game.run.ending == "hold")
    game.queue_free()
    await process_frame

    # Second run: leave via post through the usual E action.
    var leave_game = GameScene.instantiate()
    root.add_child(leave_game)
    await process_frame
    leave_game.player.paused = true
    place_at(leave_game,"window")
    await tap()
    for i in 3:
        place_at(leave_game,"far_door")
        await tap()
    assert(leave_game.run.handle_owned)
    place_at(leave_game,"neighbor_door")
    await tap()
    place_at(leave_game, "bed")
    await process_frame
    leave_game.player.yaw += 1.2
    leave_game.player.apply_orientation()
    await create_timer(1.9).timeout
    assert(leave_game.run.chapter == leave_game.run.Chapter.FINALE)
    place_at(leave_game,"station_exit")
    await tap()
    assert(leave_game.run.ending == "leave", "Exit action must yield leave ending")
    leave_game.queue_free()
    print("INTERACTION_TESTS_OK: early knock, window turn, slow door, handle persistence, hold ending, leave ending")
    quit(0)
