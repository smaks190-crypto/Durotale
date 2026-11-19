extends Node3D

const StateScript = preload("res://scripts/run_state.gd")
const PlayerScript = preload("res://scripts/player.gd")
const WorldScript = preload("res://scripts/world.gd")
const HUDScript = preload("res://scripts/hud.gd")

var run: DurkaRunState
var player: DurkaPlayer
var world: DurkaWorld
var hud: DurkaHUD
var sound: AudioStreamPlayer
var game_clock := 0.0
var subtitle_end := 0.0
var paused := false
var shake_enabled := true
var flashes_enabled := true
var vhs_enabled := true
var touch_screen := false
var window_transition_pending := false
var yaw_at_window_close := 0.0
var last_yaw := 0.0
var last_reality_shift := 0.0
var finale_begin := 0.0
var handle_recovered_at := -1.0
var hold_since := -1.0
var previous_interact := false
var current_target := ""
var last_footstep := 0.0
var intro_nurse_gone := false
var end_started := false

func _ready() -> void:
    run = StateScript.new()
    world = WorldScript.new()
    world.name = "SharedHospitalGeometry"
    add_child(world)
    player = PlayerScript.new()
    player.game = self
    player.position = Vector3(0.7,0.12,0.7)
    add_child(player)
    hud = HUDScript.new()
    add_child(hud)
    hud.sensitivity_changed.connect(_on_sensitivity)
    hud.accessibility_changed.connect(_on_accessibility)
    hud.resume_requested.connect(_resume)
    hud.pause_requested.connect(_toggle_pause)
    hud.restart_requested.connect(_restart)
    sound = AudioStreamPlayer.new()
    sound.volume_db = -6
    add_child(sound)
    touch_screen = OS.has_feature("mobile")
    last_yaw = player.yaw
    title_text("Утро. Больничная палата.\nWASD — ходить, мышь — осмотреться, E — взаимодействовать, Esc — пауза.", 7.0)
    # The nurse does not wait for a quest trigger; the room remains safe and calm.
    get_tree().create_timer(3.2).timeout.connect(_nurse_hello)
    get_tree().create_timer(10.0).timeout.connect(_nurse_leaves)

func _nurse_hello() -> void:
    if run.chapter == DurkaRunState.Chapter.FINISHED:
        return
    title_text("Медсестра: «Когда пойдёте завтракать, прикройте за собой. Тут сквозит». ", 6.2)

func _nurse_leaves() -> void:
    intro_nurse_gone = true
    world.nurse.visible = false
    # Early environmental hint: a door opens on its own, with no forced player input.
    world.room_leaf.visible = true
    var slow_door := create_tween()
    slow_door.tween_property(world.room_leaf, "rotation:y", 0.85, 2.8)
    if run.chapter == DurkaRunState.Chapter.WAKE:
        title_text("За дверью затихают шаги. Где-то у батареи — три глухих удара.", 4.8)
        knock_sound(3)

func _on_sensitivity(value: float) -> void:
    player.sensitivity = 0.0026 * value

func _on_accessibility(feature: String, enabled: bool) -> void:
    match feature:
        "shake": shake_enabled = enabled
        "flashes": flashes_enabled = enabled
        "vhs":
            vhs_enabled = enabled
            hud.set_vhs(enabled)

func _input(event: InputEvent) -> void:
    if event.is_action_pressed("pause_game") and not event.is_echo():
        _toggle_pause()
        get_viewport().set_input_as_handled()

func _toggle_pause() -> void:
    if end_started:
        return
    paused = not paused
    hud.show_pause(paused)
    player.paused = paused
    if not touch_screen:
        Input.mouse_mode = Input.MOUSE_MODE_VISIBLE if paused else Input.MOUSE_MODE_CAPTURED

func _resume() -> void:
    paused = false
    player.paused = false
    hud.show_pause(false)
    if not touch_screen:
        Input.mouse_mode = Input.MOUSE_MODE_CAPTURED

func _restart() -> void:
    Input.action_release("interact")
    get_tree().reload_current_scene()

func title_text(text: String, duration: float = 4.0) -> void:
    hud.set_subtitle(text)
    subtitle_end = game_clock + duration

func _process(delta: float) -> void:
    if end_started or paused:
        return
    game_clock += delta
    if subtitle_end < game_clock and hud.subtitle.text != "":
        hud.set_subtitle("")
    var action := nearest_interaction()
    current_target = action
    var hint := ""
    if action != "":
        hint = interaction_label(action)
    hud.set_hint(hint)
    hud.set_inventory(run.handle_owned, run.handle_installed)
    var pressed := Input.is_action_pressed("interact")
    if pressed and not previous_interact:
        if run.chapter == DurkaRunState.Chapter.FINALE and action == "neighbor_door":
            hold_since = game_clock
            title_text("Дверь тянет на себя кто-то невидимый. Держите E, чтобы придержать.", 2.2)
        else:
            do_interaction(action)
    if not pressed:
        hold_since = -1.0
    if hold_since >= 0.0:
        if action != "neighbor_door":
            hold_since = -1.0
        elif game_clock - hold_since >= 2.0:
            end_episode("hold")
    previous_interact = pressed
    if window_transition_pending:
        if absf(angle_difference(player.yaw, yaw_at_window_close)) > 0.45:
            window_transition_pending = false
            apply_reality(1)
            title_text("Металл кровати почернел от ржавчины. Но кружка стояла здесь же.", 4.8)
    if run.chapter == DurkaRunState.Chapter.HANDLE and handle_recovered_at >= 0 and game_clock - handle_recovered_at > 3.4:
        handle_recovered_at = -1.0
        apply_reality(0)
        title_text("Тот же коридор. В ладони — тяжёлая дверная ручка.", 4.3)
    if run.chapter == DurkaRunState.Chapter.RETURN and game_clock - finale_begin >= 4.2:
        run.start_finale()
        apply_reality(2, true)
        title_text("Из соседней палаты отвечают тем же ритмом. Дверь поддаётся под рукой.", 5.2)
        play_beat_echo()
    if run.chapter == DurkaRunState.Chapter.FINALE:
        # The world accelerates around a stable, recognizable geometry.
        if game_clock - last_reality_shift > 3.2 or absf(angle_difference(player.yaw,last_yaw)) > 1.5:
            last_reality_shift = game_clock
            var next_version := (world.reality + 1) % 3
            world.set_reality(next_version,true)
            if flashes_enabled:
                world.hall_light.light_energy = 0.36 if world.hall_light.light_energy > 0.6 else 1.30
            if game_clock - last_footstep > 2.6:
                last_footstep = game_clock
                tone(100, 0.23, 0.35)
    last_yaw = player.yaw
    if shake_enabled and run.chapter >= DurkaRunState.Chapter.RETURN and run.chapter < DurkaRunState.Chapter.FINISHED:
        player.camera.rotation.z = sin(game_clock * 9.2) * (0.004 if run.chapter == DurkaRunState.Chapter.RETURN else 0.011)
    else:
        player.camera.rotation.z = 0.0

func nearest_interaction() -> String:
    if run.chapter == DurkaRunState.Chapter.FINISHED:
        return ""
    var result := ""
    var distance_min := 2.35
    for name in world.markers.keys():
        var key: String = str(name)
        if key == "station_exit" and run.chapter != DurkaRunState.Chapter.FINALE:
            continue
        if key == "neighbor_inside":
            continue
        if key == "window" and run.window_closed:
            continue
        var pos: Vector3 = world.markers[key]["position"]
        var delta_to := pos - player.global_position
        var flat := Vector3(delta_to.x,0,delta_to.z)
        var dist := flat.length()
        if dist > distance_min:
            continue
        if flat.length() > 0.2 and flat.normalized().dot(player.facing_direction()) < -0.42:
            continue
        distance_min = dist
        result = key
    return result

func interaction_label(key: String) -> String:
    match key:
        "radiator": return "E — стукнуть по стене у батареи"
        "window": return "E — закрыть форточку"
        "ward_door": return "E — прикрыть дверь"
        "far_door": return "E — открыть дальнюю дверь"
        "neighbor_door":
            if run.chapter == DurkaRunState.Chapter.FINALE:
                return "Удерживать E — придержать дверь. Или уйти через пост."
            elif run.handle_installed:
                return "Дверь открыта. За ней — чужие шаги."
            elif run.handle_owned:
                return "E — вставить найденную ручку в квадратный стержень"
            return "E — осмотреть сломанный замок"
        "station_exit": return "E — уйти через пост"
    return ""

func do_interaction(key: String) -> void:
    match key:
        "radiator":
            tone(140,0.10,0.45)
            if run.record_knock(game_clock):
                title_text("После паузы за стеной повторяют твой ритм.", 3.2)
                play_beat_echo()
            else:
                title_text("Металл отзывается одиночным глухим ударом. Можно постучать снова.", 2.6)
        "window":
            run.close_window()
            window_transition_pending = true
            yaw_at_window_close = player.yaw
            world.window_leaf.rotation.z = -0.4
            title_text("Форточка закрыта. Что-то тихо скрипнуло за спиной.", 3.2)
            # Never require the player to rotate: fallback after four seconds.
            get_tree().create_timer(4.0).timeout.connect(func() -> void:
                if window_transition_pending and not end_started:
                    window_transition_pending = false
                    apply_reality(1)
            )
        "ward_door":
            world.room_leaf.visible = not world.room_leaf.visible
            if world.room_leaf.visible:
                title_text("Защёлка щёлкнула почти беззвучно.",2.2)
            else:
                title_text("Дверь медленно открывается сама.",2.2)
        "far_door":
            if run.handle_owned:
                title_text("На месте ручки остался только квадратный стержень.",2.4)
                return
            var acquired := run.open_far_door(game_clock)
            if acquired:
                apply_reality(2)
                handle_recovered_at = game_clock
                player.global_position = Vector3(0,0.1,13.0)
                tone(87,0.62,0.65)
                title_text("Дверь вырвали из руки. Что-то тяжёлое осталось в ладони.", 5.0)
            else:
                # Loop a short, consistent route: do not penalize slow players.
                player.global_position = Vector3(0,0.1,9.0)
                world.room_leaf.visible = false
                title_text("За дверью — тот же самый коридор. Ты ошибся поворотом?", 3.5)
                tone(110,0.15,0.2)
        "neighbor_door":
            if run.chapter == DurkaRunState.Chapter.FINALE:
                return
            if run.handle_installed:
                title_text("Шаги теперь по ту сторону. Дверь сопротивляется.",2.8)
            elif run.install_handle():
                world.open_neighbor_door()
                finale_begin = game_clock
                apply_reality(0)
                title_text("Ручка подошла. Снаружи кто-то идёт всё быстрее.",4.4)
                tone(75,0.20,0.45)
            else:
                title_text("В замке виден квадратный стержень. Ручки нет.",3.2)
        "station_exit":
            if run.chapter == DurkaRunState.Chapter.FINALE:
                end_episode("leave")

func apply_reality(version: int, mixing: bool = false) -> void:
    run.enter_reality(version)
    world.set_reality(version, mixing)

func play_beat_echo() -> void:
    var sequence: Array[float] = run.echo_pattern.duplicate()
    if sequence.is_empty():
        sequence = [0.32,0.32]
    # Small pause, then replay exactly the player's inter-knock intervals.
    await get_tree().create_timer(0.75).timeout
    if not is_inside_tree() or end_started:
        return
    knock_visual()
    tone(97,0.12,0.48)
    for interval in sequence:
        await get_tree().create_timer(clampf(interval,0.08,2.5)).timeout
        if not is_inside_tree() or end_started:
            return
        knock_visual()
        tone(97,0.12,0.48)

func knock_visual() -> void:
    # Deliberately readable when muted: bracket twitches and dust visibly falls.
    var bracket := world.get_node_or_null("RadiatorBracket") as MeshInstance3D
    if bracket != null:
        var old_pos := bracket.position
        bracket.position = old_pos + Vector3(0,0.11,0)
        get_tree().create_timer(0.13).timeout.connect(func() -> void:
            if is_instance_valid(bracket):
                bracket.position = old_pos
        )
    var particle := MeshInstance3D.new()
    var mesh := SphereMesh.new()
    mesh.radius = 0.085
    mesh.height = 0.15
    mesh.radial_segments = 5
    mesh.rings = 3
    particle.mesh = mesh
    particle.position = Vector3(-5.2,1.2,-1.1)
    var m := StandardMaterial3D.new()
    m.albedo_color = Color(0.62,0.58,0.54)
    particle.material_override = m
    add_child(particle)
    var tween := create_tween()
    tween.tween_property(particle,"position:y",0.06,0.75)
    tween.tween_callback(particle.queue_free)

func knock_sound(count: int = 3) -> void:
    for i in count:
        await get_tree().create_timer(0.28 if i != 0 else 0.1).timeout
        if not is_inside_tree():
            return
        tone(125,0.13,0.35)
        knock_visual()

func tone(hz: float, duration: float, volume: float) -> void:
    # Short generated sound: no external samples and no copyright dependencies.
    var rate := 22050
    var samples := int(rate * duration)
    var audio := PackedByteArray()
    audio.resize(samples * 2)
    for i in samples:
        var envelope := exp(-5.0 * float(i) / maxf(1.0,float(samples)))
        var wave := sin(TAU * hz * float(i) / float(rate))
        var sample := int(clampf(wave * envelope * volume,-0.95,0.95) * 32000.0)
        if sample < 0:
            sample += 65536
        audio[i*2] = sample & 255
        audio[i*2+1] = (sample >> 8) & 255
    var wav := AudioStreamWAV.new()
    wav.format = AudioStreamWAV.FORMAT_16_BITS
    wav.mix_rate = rate
    wav.stereo = false
    wav.data = audio
    sound.stream = wav
    sound.play()

func end_episode(choice: String) -> void:
    if not run.finish(choice):
        return
    end_started = true
    player.paused = true
    hud.set_hint("")
    hud.set_subtitle("")
    world.shadow.visible = choice == "hold"
    if choice == "leave":
        tone(380,0.18,0.25)
    else:
        tone(65,0.3,0.25)
    # Silence and fade, then a restart action. No branching menu beforehand.
    await get_tree().create_timer(2.5).timeout
    if not is_inside_tree():
        return
    hud.show_ending(choice)
    if not touch_screen:
        Input.mouse_mode = Input.MOUSE_MODE_VISIBLE
