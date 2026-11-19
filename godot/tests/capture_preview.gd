extends SceneTree
func _initialize():call_deferred("take")
func take():
    var g=load("res://chapter.tscn").instantiate()
    root.add_child(g)
    await process_frame
    g.standing=true
    g.patient.position=Vector3(-2.80,.01,2.18)
    g.yaw=-.39
    g.pitch=-.055
    g.canvas.hide()
    var dest:="res://build/qa"
    DirAccess.make_dir_recursive_absolute(ProjectSettings.globalize_path(dest))
    for i in 3:
        g.ward.state(i)
        g.ward.nurse.visible=i==0
        if i==2:
            g.patient.position=Vector3(2.15,0,-5.4)
            g.yaw=.01
        for frame in 5:await process_frame
        var output:=root.get_texture().get_image()
        if output.save_png(dest+"/chapter_%d.png"%i)!=OK:
            quit(1)
            return
    print("VISUAL_PREVIEW_OK")
    quit()
