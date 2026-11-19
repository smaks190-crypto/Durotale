extends SceneTree
var g
var failures:=0
func _initialize():call_deferred("exercise")
func check(ok:bool,text:String):
    if not ok:
        failures+=1
        push_error(text)
func key(down:bool):
    var e:=InputEventKey.new()
    e.physical_keycode=KEY_E
    e.pressed=down
    Input.parse_input_event(e)
    Input.flush_buffered_events()
func tap():
    key(true)
    await physics_frame
    key(false)
    await physics_frame
func move_to(x:float,z:float):
    var goal:=Vector2(x,z)
    for i in 750:
        var d:=goal-Vector2(g.patient.position.x,g.patient.position.z)
        if d.length()<.085:break
        g.look(Vector2(-angle_difference(g.yaw,atan2(-d.x,-d.y))/g.sens,0))
        g.joy=Vector2.UP
        await physics_frame
    g.joy=Vector2.ZERO
    await physics_frame
    check(goal.distance_to(Vector2(g.patient.position.x,g.patient.position.z))<.20,"Путь %s → %s"%[g.patient.position,goal])
func exercise():
    for pick in ["hold","leave"]:
        g=load("res://chapter.tscn").instantiate()
        root.add_child(g)
        await physics_frame
        await tap()
        await move_to(-2.85,.95)
        await tap()
        await create_timer(.39).timeout
        await tap()
        await create_timer(2.6).timeout
        check(g.answered and abs(g.pattern[0]-.4)<.07,"Ранний ритм")
        await move_to(.54,.65)
        await move_to(.54,-3.39)
        await move_to(.36,-3.52)
        await tap()
        await move_to(.96,-3.55)
        await move_to(2.46,-3.52)
        await move_to(2.46,-8.)
        check(g.closed and g.ward.version==1,"Форточка")
        for j in 3:
            await move_to(2.44,-19.83)
            await create_timer(3.2).timeout
            await tap()
            await create_timer(.55).timeout
        check(g.visits==3 and g.pocket,"Три медленных открытия")
        await create_timer(4.4).timeout
        check(g.pocket and g.ward.version==0,"Ручка после перехода")
        await move_to(2.3,-13.3)
        await move_to(1.38,-13.3)
        await tap()
        check(g.fitted,"Установка")
        await move_to(2.45,-12.6)
        await move_to(2.45,-3.31)
        await move_to(2.45,-8.1)
        check(g.finale,"Возвращение")
        if pick=="hold":
            await move_to(2.42,-13.3)
            await move_to(1.38,-13.3)
            key(true)
            await create_timer(2.1).timeout
            key(false)
        else:
            await move_to(3.13,-8.1)
            await tap()
        await create_timer(4.4).timeout
        check(g.ending_name==pick and g.ending.visible,"Конец "+pick)
        g.queue_free()
        await process_frame
    print("INTERACTION_TESTS_OK" if failures==0 else "ROUTES_FAILED")
    quit(int(failures>0))
