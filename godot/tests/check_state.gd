extends SceneTree

func _initialize() -> void:
    call_deferred("run_checks")

func run_checks() -> void:
    var S = load("res://scripts/run_state.gd")
    # Arbitrary rhythm, including early knocks before closing the window.
    var early = S.new()
    assert(not early.record_knock(0.0))
    assert(early.record_knock(0.82))
    assert(early.echo_established)
    assert(absf(early.echo_pattern[0] - 0.82) < 0.001)
    early.close_window()
    assert(early.echo_established)
    # Slow door opening must succeed on the third attempt.
    assert(not early.open_far_door(10.0))
    assert(not early.open_far_door(15.0))
    assert(early.open_far_door(20.0))
    assert(early.handle_owned)
    early.enter_reality(S.Reality.LIVING)
    early.enter_reality(S.Reality.ABANDONED)
    early.enter_reality(S.Reality.DISTORTED)
    assert(early.handle_owned)
    assert(early.install_handle())
    assert(early.handle_installed)
    early.start_finale()
    assert(early.finish("hold"))
    assert(early.ending == "hold")
    # A fast player may resolve it earlier, but never needs to.
    var fast = S.new()
    assert(not fast.open_far_door(1.0))
    assert(fast.open_far_door(1.4))
    assert(fast.install_handle())
    fast.start_finale()
    assert(fast.finish("leave"))
    assert(fast.ending == "leave")
    # Still possible to ignore knocks initially.
    var late = S.new()
    late.close_window()
    assert(late.record_knock(50.0) == false)
    assert(late.record_knock(50.3))
    print("STATE_TESTS_OK: early rhythm, slow/fast door, persistence, two endings, late rhythm")
    quit(0)
