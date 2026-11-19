extends Node3D

const StateScript = preload("res://scripts/run_state.gd")
const PlayerScript = preload("res://scripts/player.gd")
const WorldScript = preload("res://scripts/world.gd")
const HUDScript = preload("res://scripts/hud.gd")

var run: DurkaRunState
var player: DurkaPlayer
var world: DurkaWorld
var hud: DurkaHUD
var hum := AudioStreamPlayer.new()
var sound: AudioStreamPlayer3D
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
var echo_due := -1.0
var returned_at := -1.0
var seated := true
var next_steps := 0.0
var previous_interact := false
var current_target := ""
var last_footstep := 0.0
var intro_nurse_gone := false
var end_started := false

func _ready() -> void:
    process_mode = Node.PROCESS_MODE_ALWAYS
    run = StateScript.new()
    world = WorldScript.new()
    world.process_mode = Node.PROCESS_MODE_PAUSABLE
    world.name = "SharedHospitalGeometry"
    add_child(world)
    player = PlayerScript.new()
    player.game = self
    player.position = Vector3(2.05,0.80,-1.6)
    player.yaw = PI
    player.paused = true
    add_child(player)
    hud = HUDScript.new()
    add_child(hud)
    hud.sensitivity_changed.connect(_on_sensitivity)
    hud.accessibility_changed.connect(_on_accessibility)
    hud.resume_requested.connect(_resume)
    hud.pause_requested.connect(_toggle_pause)
    hud.restart_requested.connect(_restart)
    sound = AudioStreamPlayer3D.new()
    sound.unit_size = 7.0
    sound.process_mode = Node.PROCESS_MODE_PAUSABLE
    sound.volume_db = -6
    add_child(sound)
    var air := AudioStreamWAV.new()
    air.format = AudioStreamWAV.FORMAT_16_BITS
    air.mix_rate = 11025
    air.loop_mode = AudioStreamWAV.LOOP_FORWARD
    air.loop_end = 22050
    var samples := PackedByteArray()
    samples.resize(44100)
    for i in 22050:
        var t := float(i)/11025.0
        samples.encode_s16(i*2, int(800*sin(TAU*50*t) + 230*sin(TAU*101*t) + randf_range(-95,95)))
    air.data = samples
    hum.stream = air
    hum.process_mode = Node.PROCESS_MODE_PAUSABLE
    add_child(hum)
    hum.play()
    touch_screen = OS.has_feature("mobile")
    last_yaw = player.yaw
    title_text(("Слева — шаг, справа — обзор. E — действие, Ⅱ — пауза." if touch_screen else "Утро. Больничная палата.\nWASD — ходить, мышь — осмотреться, E — взаимодействовать, Esc — пауза."), 7.0)
    # The nurse does not wait for a quest trigger; the room remains safe and calm.
    get_tree().create_timer(3.2, false).timeout.connect(_nurse_hello)
    get_tree().create_timer(12.0, false).timeout.connect(_nurse_leaves)

func _nurse_hello() -> void:
    if run.chapter == DurkaRunState.Chapter.FINISHED:
        return
    title_text("Медсестра: «Когда пойдёте завтракать, прикройте за собой. Тут сквозит». ", 6.2)

func _nurse_leaves() -> void:
    intro_nurse_gone = true
    var departure := create_tween()
    departure.tween_property(world.nurse, "position:z", 9.2, 2.2)
    departure.tween_callback(world.nurse.hide)
    # Early environmental hint: a door opens on its own, with no forced player input.
    world.room_leaf.visible = true
    var slow_door := create_tween()
    slow_door.tween_property(world.room_leaf, "rotation:y", 0.85, 2.8)
    if not run.echo_established:
        await get_tree().create_timer(17.0, false).timeout
    if not run.echo_established:
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
    get_tree().paused = paused
    player.paused = paused or seated
    if not touch_screen:
        Input.mouse_mode = Input.MOUSE_MODE_VISIBLE if paused else Input.MOUSE_MODE_CAPTURED

func _resume() -> void:
    paused = false
    get_tree().paused = false
    player.paused = seated
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
    if world.mixed:
        world.get_node("LivingHospitalShadow").position.z = 20.0 + sin(game_clock * 1.2) * 1.7
    if subtitle_end < game_clock and hud.subtitle.text != "":
        hud.set_subtitle("")
    if echo_due >= 0 and game_clock >= echo_due:
        echo_due = -1
        run.knock_times.clear()
        play_beat_echo()
    var action := "wake" if seated else nearest_interaction()
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
    if run.chapter == DurkaRunState.Chapter.RETURN:
        if game_clock >= next_steps:
            tone(72, 0.18, 0.42)
            next_steps = game_clock + maxf(0.43, 1.65 - (game_clock-finale_begin)*0.04)
        if player.position.z < 5.4 and returned_at < 0:
            returned_at = game_clock
            title_text("В палате горит свет. Шаги остановились у порога.", 4)
    if returned_at >= 0 and run.chapter == DurkaRunState.Chapter.RETURN and (player.facing_direction().z > 0.55 or game_clock-returned_at > 6):
        run.start_finale()
        apply_reality(2, true)
        title_text("Из соседней палаты отвечают тем же ритмом. Дверь поддаётся под рукой.", 5.2)
        play_beat_echo()
    if run.chapter == DurkaRunState.Chapter.FINALE:
        # The world accelerates around a stable, recognizable geometry.
        if game_clock - last_reality_shift > 3.2 or absf(angle_difference(player.yaw,last_yaw)) > 1.5:
            last_reality_shift = game_clock
            var next_version := (world.reality + 1) % 3
            apply_reality(next_version,true)
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
        "wake": return "E — сесть и встать с койки"
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
        "bed": return "E — осмотреть край койки"
        "cup": return "E — поднять кружку"
        "medical_record": return "E — прочитать карточку на посту"
        "archive_record": return "E — осмотреть историю болезни"
        "trolley": return "E — осмотреть каталку"
        "treatment_notes": return "E — осмотреть медицинский поднос"
    return ""

func do_interaction(key: String) -> void:
    match key:
        "wake":
            seated = false
            player.position = Vector3(0.7,0.12,0.7)
            player.paused = false
        "radiator":
            tone(140,0.10,0.45)
            if run.record_knock(game_clock):
                title_text("За стеной прислушиваются.", 3.2)
                knock_visual()
                echo_due = game_clock + 1.2
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
            world.room_leaf.visible = true
            var closing := absf(world.room_leaf.rotation.y) > 0.24
            create_tween().tween_property(world.room_leaf, "rotation:y", 0.0 if closing else 0.85, 1.15)
            title_text("Дверь снова движется слишком легко. «Тут сквозит»...", 3.1)
        "far_door":
            if not run.echo_established or not run.window_closed:
                title_text("Створка дрожит от сквозняка." if not run.window_closed else "За стеной у койки стучат.", 2.5)
                return
            if run.handle_owned:
                title_text("На месте ручки остался только квадратный стержень.",2.4)
                return
            var acquired := run.open_far_door(game_clock)
            if acquired:
                world.get_node("LoopDoorHandle").hide()
                apply_reality(2)
                handle_recovered_at = game_clock
                player.global_position = Vector3(0,0.1,13.0)
                tone(87,0.62,0.65)
                title_text("Дверь вырвали из руки. Что-то тяжёлое осталось в ладони.", 5.0)
            else:
                # Loop a short, consistent route: do not penalize slow players.
                player.global_position = Vector3(0,0.1,24.2 if run.far_door_attempts > 1 else 23.4)
                world.far_leaf.rotation.y = 1.15
                create_tween().tween_property(world.far_leaf, "rotation:y", 0.0, 0.65)
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
                title_text("Ручка подошла. В моей палате снова горит свет. Снаружи приближаются шаги.",4.4)
                tone(75,0.20,0.45)
            else:
                title_text("В замке виден квадратный стержень. Ручки нет.",3.2)
        "station_exit":
            if run.chapter == DurkaRunState.Chapter.FINALE:
                end_episode("leave")

        "bed":
            if run.reality == DurkaRunState.Reality.LIVING:
                title_text("Простыня заправлена утром. На железной раме — три глубокие царапины.", 5.2)
            else:
                title_text("Простыни давно нет. Три царапины на раме остались на прежнем месте.", 5.2)
        "cup":
            if run.reality == DurkaRunState.Reality.LIVING:
                title_text("На краю кружки след помады. Кофе всё ещё тёплый.", 4.5)
            else:
                title_text("Кружка здесь же. Пыль внутри, но скол на ободке тот же.", 4.8)
        "medical_record":
            title_text("Палата №6. Дата госпитализации неразборчива. От руки: «Слышит стук из пустой комнаты».", 6.5)
        "archive_record":
            if run.reality == DurkaRunState.Reality.LIVING:
                title_text("Архивная запись: палата №6 выведена из эксплуатации. Но сегодня вы видели в ней свет.", 6.5)
            else:
                title_text("Слова стерлись. На листе остался квадратный отпечаток дверной ручки.", 5.8)
        "trolley":
            if run.reality == DurkaRunState.Reality.LIVING:
                title_text("Каталка тёплая. На колёсах мокрые следы — к запертой палате.", 5.2)
            else:
                title_text("Ржавые колёса не крутятся. Но следы на полу совсем свежие.", 5.2)
        "treatment_notes":
            if run.reality == DurkaRunState.Reality.LIVING:
                title_text("Игла в упаковке. На подносе — снимок: пальцы обеих рук в свежих бинтах.", 5.6)
            else:
                title_text("Поднос заржавел, иглы нет. На фото всё та же ладонь, но без пальцевых отпечатков.", 5.7)

func apply_reality(version: int, mixing: bool = false) -> void:
    run.enter_reality(version)
    world.set_reality(version, mixing)

func play_beat_echo() -> void:
    var sequence: Array[float] = run.echo_pattern.duplicate()
    if sequence.is_empty():
        sequence = [0.32,0.32]
    # Small pause, then replay exactly the player's inter-knock intervals.
    await get_tree().create_timer(0.25, false).timeout
    if not is_inside_tree() or end_started:
        return
    knock_visual()
    tone(97,0.12,0.48)
    for interval in sequence:
        await get_tree().create_timer(interval, false).timeout
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
    if hz in [97.0,125.0,140.0]:
        sound.position = Vector3(2.4,1.2,20) if world.mixed else Vector3(-5.72,1.05,-1)
    elif run.chapter >= DurkaRunState.Chapter.RETURN:
        sound.position = player.position - player.facing_direction()*2.0 + Vector3.UP
    else:
        sound.position = player.position + Vector3.UP
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
    hum.stop()
    player.paused = true
    hud.set_hint("")
    hud.set_subtitle("")
    world.shadow.visible = choice == "hold"
    if choice == "hold":
        create_tween().tween_property(world.shadow,"position:x",1.66,0.55)
    if choice == "leave":
        tone(380,0.18,0.25)
    else:
        tone(65,0.3,0.25)
    # Silence and fade, then a restart action. No branching menu beforehand.
    await get_tree().create_timer(2.5).timeout
    if not is_inside_tree():
        return
    var fade := ColorRect.new()
    fade.color = Color(0,0,0,0)
    fade.mouse_filter = Control.MOUSE_FILTER_IGNORE
    hud.root.add_child(fade)
    fade.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
    var blackout := create_tween()
    blackout.tween_property(fade,"color:a",1.0,0.9)
    await blackout.finished
    hud.root.move_child(hud.ending_panel,-1)
    hud.show_ending(choice)
    if not touch_screen:
        Input.mouse_mode = Input.MOUSE_MODE_VISIBLE
