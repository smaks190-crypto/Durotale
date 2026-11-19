extends Node3D

var ward:=preload("res://ward.gd").new()
var patient:=CharacterBody3D.new()
var camera:=Camera3D.new()
var model:Node3D
var yaw:=0.
var pitch:=-.06
var sens:=.003
var joy:=Vector2.ZERO
var move_id:=-1
var look_id:=-1
var pressed:=false
var clock:=0.
var subtitle_until:=0.
var notice:=Label.new()
var prompt:=Label.new()
var bag:=Label.new()
var canvas:=Control.new()
var options:=PanelContainer.new()
var film:=ColorRect.new()
var veil:=ColorRect.new()
var touch:=Control.new()
var ending:=PanelContainer.new()
var result:=Label.new()
var standing:=false
var closed:=false
var pending:=false
var shut_yaw:=0.
var knocks:Array[float]=[]
var pattern:Array[float]=[]
var beats:Array[float]=[]
var source:=Vector3.ZERO
var listening:=false
var answered:=false
var visits:=0
var intervals:Array[float]=[]
var last_open:=-1.
var pocket:=false
var fitted:=false
var returned:=false
var finale:=false
var held:=0.
var ending_name:=""
var steps:=0.
var change_at:=0.
var finale_echo:=0.
var flash:=true
var shake:=true
var done_intro:=false
var drift:=false
var seated_time:=32.
var last_zone_yaw:=0.
var ambience:=AudioStreamPlayer.new()
var sounds:Dictionary={}
var rng:=RandomNumberGenerator.new()

func _ready():
    add_child(ward)
    ward.build()
    var collider:=CollisionShape3D.new()
    var capsule:=CapsuleShape3D.new()
    capsule.radius=.24
    capsule.height=1.72
    collider.shape=capsule
    collider.position.y=.86
    patient.add_child(collider)
    patient.collision_layer=2
    patient.position=Vector3(-.48,.82,-1.27)
    patient.floor_snap_length=.2
    add_child(patient)
    model=ward.figure(patient,false)
    yaw=-.82
    add_child(camera)
    camera.fov=58
    camera.near=.038
    camera.current=true
    build_ui()
    for s in ["knock","step","latch","air","voices"]:sounds[s]=wave(s)
    ambience.stream=sounds.air
    ambience.volume_db=-17
    add_child(ambience)
    ambience.play()
    say("Медсестра   «Когда пойдёте завтракать, прикройте за собой. Тут сквозит».",8)
    if not mobile():Input.mouse_mode=Input.MOUSE_MODE_CAPTURED
    update_camera()

func mobile()->bool:return OS.has_feature("mobile")
func wave(type:String)->AudioStreamWAV:
    var w:=AudioStreamWAV.new()
    w.mix_rate=12000
    w.format=AudioStreamWAV.FORMAT_16_BITS
    var duration:float={"air":2.,"knock":.23,"step":.42,"latch":.19,"voices":2.1}[type]
    var count:=int(duration*w.mix_rate)
    var bytes:=PackedByteArray()
    bytes.resize(count*2)
    var f:=0.
    for i in count:
        var t:=float(i)/w.mix_rate
        f=lerpf(f,rng.randf_range(-1,1),.065)
        var value:=0.
        match type:
            "air":value=.06*sin(TAU*50*t)+.025*sin(TAU*121*t)+f*.15
            "knock":value=(sin(TAU*143*t)*.47+sin(TAU*318*t)*.25)*exp(-27*t)+f*exp(-41*t)
            "step":value=(sin(TAU*(62*t+0.4*sin(17*t)))*.42+f*.7)*exp(-11*t)
            "latch":value=(sin(TAU*940*t)*.45+f)*exp(-76*t)+.22*sin(TAU*246*t)*exp(-23*abs(t-.07))
            "voices":value=(sin(TAU*113*t)+.4*sin(TAU*221*t))*.12*pow(abs(sin(9*t)*sin(4*t)),.65)+f*.09
        bytes.encode_s16(i*2,int(clampf(value,-1,1)*30000))
    w.data=bytes
    if type=="air":
        w.loop_mode=AudioStreamWAV.LOOP_FORWARD
        w.loop_end=count
    return w
func play(which:String,at:Vector3):
    var a:=AudioStreamPlayer3D.new()
    a.stream=sounds[which]
    a.position=at
    a.unit_size=3.4
    add_child(a)
    a.finished.connect(a.queue_free)
    a.play()
func say(line:String,time:=3.4):
    notice.text=line
    subtitle_until=clock+time
func label(l:Label,y:float,height:float,size:int):
    l.set_anchors_and_offsets_preset(Control.PRESET_TOP_WIDE)
    l.offset_left=80
    l.offset_right=-80
    l.offset_top=y
    l.offset_bottom=y+height
    l.horizontal_alignment=HORIZONTAL_ALIGNMENT_CENTER
    l.vertical_alignment=VERTICAL_ALIGNMENT_CENTER
    l.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
    l.add_theme_font_size_override("font_size",size)
    l.add_theme_constant_override("outline_size",4)
    l.mouse_filter=Control.MOUSE_FILTER_IGNORE
    canvas.add_child(l)
func button(parent:Control,title:String,fn:Callable)->Button:
    var b:=Button.new()
    b.text=title
    parent.add_child(b)
    b.pressed.connect(fn)
    return b
func build_ui():
    var layer:=CanvasLayer.new()
    add_child(layer)
    layer.add_child(canvas)
    canvas.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
    canvas.mouse_filter=Control.MOUSE_FILTER_IGNORE
    canvas.add_child(film)
    film.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
    film.mouse_filter=Control.MOUSE_FILTER_IGNORE
    var shader:=Shader.new()
    shader.code="shader_type canvas_item; void fragment(){float f=fract(sin(dot(floor(FRAGCOORD.xy)+floor(TIME*10.),vec2(12.96,43.57)))*43758.5);COLOR=vec4(vec3(.08),.025+.04*f+.025*step(.5,fract(FRAGCOORD.y*.5)));}"
    var sm:=ShaderMaterial.new()
    sm.shader=shader
    film.material=sm
    label(notice,405,60,17)
    label(prompt,359,38,15)
    label(bag,16,34,14)
    bag.horizontal_alignment=HORIZONTAL_ALIGNMENT_LEFT
    canvas.add_child(touch)
    touch.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
    touch.mouse_filter=Control.MOUSE_FILTER_IGNORE
    touch.visible=mobile()
    var key:=Button.new()
    key.text="Рука"
    key.position=Vector2(824,444)
    key.size=Vector2(110,68)
    touch.add_child(key)
    key.button_down.connect(func():pressed=true; act())
    key.button_up.connect(func():pressed=false)
    var pause_key:=button(touch,"Ⅱ",pause_menu)
    pause_key.position=Vector2(887,16)
    pause_key.size=Vector2(49,42)
    canvas.add_child(options)
    options.position=Vector2(284,60)
    options.custom_minimum_size=Vector2(392,330)
    options.process_mode=Node.PROCESS_MODE_ALWAYS
    var col:=VBoxContainer.new()
    options.add_child(col)
    var title:=Label.new()
    title.text="ПАУЗА     /     Чувствительность"
    col.add_child(title)
    var sl:=HSlider.new()
    sl.min_value=.001
    sl.max_value=.008
    sl.step=.00025
    sl.value=sens
    sl.value_changed.connect(func(v):sens=v)
    col.add_child(sl)
    for name_ in ["Тряска","Мигание","Плёнка"]:
        var b:=CheckBox.new()
        b.text=name_
        b.button_pressed=true
        col.add_child(b)
        b.toggled.connect(func(v):
            match b.text:
                "Тряска":shake=v
                "Мигание":flash=v
                "Плёнка":film.visible=v
        )
    button(col,"Продолжить",pause_menu)
    options.hide()
    canvas.add_child(veil)
    veil.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
    veil.color=Color(0,0,0,0)
    veil.mouse_filter=Control.MOUSE_FILTER_IGNORE
    canvas.add_child(ending)
    ending.position=Vector2(291,201)
    ending.custom_minimum_size=Vector2(380,132)
    var e:=VBoxContainer.new()
    ending.add_child(e)
    e.add_child(result)
    result.horizontal_alignment=HORIZONTAL_ALIGNMENT_CENTER
    button(e,"Пройти снова",func():get_tree().reload_current_scene())
    ending.hide()
func pause_menu():
    if ending_name!="":return
    get_tree().paused=not get_tree().paused
    options.visible=get_tree().paused
    pressed=false
    move_id=-1
    look_id=-1
    joy=Vector2.ZERO
    touch.visible=mobile() and not options.visible
    if not mobile():Input.mouse_mode=Input.MOUSE_MODE_VISIBLE if options.visible else Input.MOUSE_MODE_CAPTURED
func _input(event):
    if event is InputEventKey and not event.echo:
        if event.keycode==KEY_ESCAPE and event.pressed:pause_menu()
        if event.physical_keycode==KEY_E:
            pressed=event.pressed
            if pressed:act()
    if event is InputEventMouseMotion and Input.mouse_mode==Input.MOUSE_MODE_CAPTURED:
        look(event.relative)
func _unhandled_input(event):
    if event is InputEventScreenTouch:
        if event.pressed:
            if event.position.x<420 and move_id<0:
                move_id=event.index
                joy=Vector2.ZERO
            elif look_id<0:look_id=event.index
        else:
            if event.index==move_id:
                move_id=-1
                joy=Vector2.ZERO
            if event.index==look_id:look_id=-1
    if event is InputEventScreenDrag:
        if event.index==move_id:joy=(joy+event.relative/48.).limit_length(1.)
        if event.index==look_id:look(event.relative*1.38)
func look(v:Vector2):
    yaw-=v.x*sens
    pitch=clampf(pitch-v.y*sens,-.76,.61)
func update_camera():
    model.rotation.y=yaw
    var basis_y:=Basis(Vector3.UP,yaw)
    var focus:=patient.position+Vector3.UP*1.56
    var desired:=focus+basis_y*Vector3(.46,.18,1.05)
    var ray:=PhysicsRayQueryParameters3D.create(focus,desired,1)
    var hit:=get_world_3d().direct_space_state.intersect_ray(ray)
    camera.position=desired if hit.is_empty() else Vector3(hit.position).move_toward(focus,.14)
    camera.rotation=Vector3(pitch,yaw,0)
    if shake and finale:camera.rotation.z=sin(clock*6.)*.0045
func nearby()->String:
    if not standing:return "rise"
    var best:=""
    var dist:=1.72
    for id in ward.stations:
        var coord:Vector3=ward.stations[id]
        var diff:=coord-patient.position
        diff.y=0
        if diff.length()<dist and (-camera.basis.z).dot(diff.normalized())>-.30:
            best=id
            dist=diff.length()
    return best
func act():
    if options.visible or ending_name!="":return
    match nearby():
        "rise":
            standing=true
            patient.position=Vector3(-1.45,.02,-.78)
            yaw=-.43
            say("Слева — движение, справа — обзор. Кнопка «Рука» — действие." if mobile() else "WASD — идти     Мышь — обзор     E — действие     Esc — пауза",5)
        "desk":
            if finale:finish("leave")
            else:say("В журнале дежурств под сегодняшней датой написано карандашом: «Седьмая снова стучит».",5)
        "pipe":
            if not listening:knocks.clear()
            listening=true
            beats.clear()
            knocks.append(clock)
            ward.pulse()
            play("knock",ward.bracket.position)
        "sash":
            closed=true
            ward.sash.rotation.z=0
            pending=ward.version==0 and not fitted
            shut_yaw=yaw
            play("latch",ward.sash.position)
            say("Сквозняк стих.")
        "ward":
            var target:=PI if abs(ward.main_door.rotation.y-PI)>0.5 else PI/2
            create_tween().tween_property(ward.main_door,"rotation:y",target,.8)
            play("latch",ward.main_door.position)
        "loop":
            if pocket or fitted:say("В дереве осталось квадратное отверстие.")
            elif not answered or not closed:say("Дверь притворена. В тишине слышно, как в палате дребезжит крепление трубы.")
            else:
                visits+=1
                intervals.append(clock-last_open)
                last_open=clock
                var quick:bool=visits>1 and intervals.back()<3.0
                var t:=create_tween()
                t.tween_property(ward.loop_door,"rotation:y",-1.42,.16 if quick else .46)
                play("latch",ward.loop_door.position)
                if visits>=3 or quick:
                    pocket=true
                    ward.lever.hide()
                    bag.text="В кармане   /   дверная ручка"
                    ward.state(2)
                    change_at=clock+4
                    say("С той стороны потянули сильнее. В ладони осталась ручка.")
                else:
                    t.tween_callback(func():
                        patient.position.z=-15.8 if visits==1 else -18.6
                        ward.loop_door.rotation.y=0
                        ward.state(2)
                        say("Та же царапина на косяке.",2.5))
        "socket":
            if finale:return
            if pocket:
                pocket=false
                fitted=true
                bag.text=""
                ward.spindle_handle.scale.x=4.
                ward.deadbolt.collision_layer=0
                create_tween().tween_property(ward.next_door,"rotation:y",2.94,.75)
                ward.state(0)
                steps=clock
                say("Ручка села на стержень. В твоей палате снова горит свет.")
            elif not fitted:say("Под большим пальцем — квадратный стержень замка.")
func echo(at:Vector3):
    source=at
    beats.assign([clock+.35])
    for dt in pattern:beats.append(beats.back()+dt)
func finish(which:String):
    if ending_name!="":return
    ending_name=which
    touch.hide()
    prompt.text=""
    ambience.stop()
    beats.clear()
    if which=="hold":
        ward.hand.show()
        say("Ладонь с другой стороны принимает вес двери.",2.2)
    else:
        play("latch",ward.next_door.position)
        say("За спиной щёлкает защёлка.",2.2)
    await get_tree().create_timer(2.7).timeout
    notice.text=""
    var t:=create_tween()
    t.tween_property(veil,"color:a",1.,1.3)
    await t.finished
    result.text="ПАЛАТА № 0\nСквозняк стих."
    ending.show()
    Input.mouse_mode=Input.MOUSE_MODE_VISIBLE
func _physics_process(dt:float):
    clock+=dt
    if ending_name!="":return
    if standing:
        var v:=Vector2(float(Input.is_physical_key_pressed(KEY_D))-float(Input.is_physical_key_pressed(KEY_A)),float(Input.is_physical_key_pressed(KEY_S))-float(Input.is_physical_key_pressed(KEY_W)))+joy
        v=v.limit_length(1)
        var motion:=Vector3(v.x,0,v.y).rotated(Vector3.UP,yaw)*2.05
        patient.velocity=Vector3(motion.x,patient.velocity.y-12.*dt,motion.z)
        patient.move_and_slide()
        model.position.y=.013*sin(clock*9.)*v.length()
    update_camera()
    if clock>subtitle_until:notice.text=""
    if clock>8 and not done_intro:
        done_intro=true
        var t:=create_tween()
        t.tween_property(ward.nurse,"position:z",-8.3,3.2)
        t.tween_callback(ward.nurse.hide)
    if standing and not drift and (patient.position.z< -4.5 or clock>24):
        drift=true
        create_tween().tween_property(ward.main_door,"rotation:y",PI/2+.15,2.9)
    if drift and not answered and beats.is_empty() and not listening and clock>seated_time:
        source=ward.bracket.position
        beats.assign([clock+.4,clock+.86,clock+1.32])
        seated_time=clock+26
    if listening and clock-knocks.back()>1.45:
        listening=false
        if knocks.size()>=2:
            pattern.clear()
            for i in range(1,knocks.size()):pattern.append(knocks[i]-knocks[i-1])
            answered=true
            echo(ward.bracket.position)
    if not beats.is_empty() and clock>=beats.front():
        beats.pop_front()
        play("knock",source)
        if source.distance_to(ward.bracket.position)<1:ward.pulse()
        else:ward.spindle_handle.rotation.z=.16 if ward.spindle_handle.rotation.z<.1 else 0.
    if pending and abs(angle_difference(yaw,shut_yaw))>.68:
        pending=false
        ward.state(1)
    if pocket and clock>change_at and ward.version==2:ward.state(0)
    if fitted:
        if clock>steps:
            play("step",patient.position+camera.basis.z*2.6)
            steps=clock+(.56 if finale else 1.24)
        if not returned and patient.position.z> -3.7:
            returned=true
            last_zone_yaw=yaw
            say("Шаги остановились у порога.",2.8)
        if returned and not finale and (abs(angle_difference(yaw,last_zone_yaw))>.65 or patient.position.z< -4.3):
            finale=true
            change_at=clock
            echo(ward.next_door.position)
            finale_echo=clock+8
        if finale:
            if clock>change_at:
                ward.state(2 if ward.version!=2 else 1)
                ward.gurney.show()
                play("voices",patient.position+camera.basis.z*2)
                change_at=clock+4.6
            ward.gurney.position.z=-13.3+sin(clock*.7)*1.65
            if flash:ward.ward_lamp.light_energy+=.025*sin(clock*16.)
            if clock>finale_echo and beats.is_empty():
                echo(ward.next_door.position)
                finale_echo=clock+8.
    var id:=nearby()
    var names:={"rise":"Встать с койки","desk":"Уйти через пост" if finale else "Прочитать журнал","pipe":"Постучать","sash":"Форточка закрыта" if closed else "Закрыть форточку","ward":"Придержать дверь","loop":"Потянуть дверь","socket":"Придержать дверь · удерживать" if finale else ("Установить ручку" if pocket else "Коснуться замка")}
    prompt.text=(("Рука" if mobile() else "E")+"   /   "+str(names[id])) if names.has(id) else ""
    if finale and id=="socket" and pressed:
        held+=dt
        if held>1.75:finish("hold")
    else:held=0
