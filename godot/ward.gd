extends "res://form.gd"

var plaster:=pigment("b5ada2",1)
var tiles:=pigment("797670",2)
var metal:=pigment("69655c",3)
var dark:=pigment("393735",3)
var enamel:=pigment("ada89b",3)
var cloth:=pigment("d0cab9")
var wet:=pigment("65594c",3)
var main_door:Node3D
var loop_door:Node3D
var next_door:Node3D
var deadbolt:StaticBody3D
var lever:Node3D
var spindle_handle:Node3D
var sash:Node3D
var nurse:Node3D
var ribs:Array[Node3D]=[]
var bracket:Node3D
var sheet:Node3D
var pillow:Node3D
var warp:=Node3D.new()
var gurney:=Node3D.new()
var hand:Node3D
var ward_lamp:OmniLight3D
var version:=0
var dust:Array[Node3D]=[]
var stations={"pipe":Vector3(-3.16,1,1.0),"sash":Vector3(-.30,1.4,-3.62),"ward":Vector3(2.45,1,-3.82),"loop":Vector3(2.45,1,-20.25),"socket":Vector3(.92,1,-13.3),"desk":Vector3(3.64,1,-8.1)}
func wall(x:float,z:float,w:float,d:float):
    box(self,Vector3(x,1.62,z),Vector3(w,3.24,d),plaster,true)
    box(self,Vector3(x,.085,z),Vector3(w+.02,.17,d+.02),dark)
func lamp(at:Vector3,power:float,r:float,col:Color)->OmniLight3D:
    var l:=OmniLight3D.new()
    l.position=at
    l.light_color=col
    l.light_energy=power
    l.omni_range=r
    l.omni_attenuation=1.25
    add_child(l)
    return l
func glow(c:Color)->StandardMaterial3D:
    var m:=StandardMaterial3D.new()
    m.albedo_color=c
    m.emission_enabled=true
    m.emission=c
    m.emission_energy_multiplier=1.6
    return m
func fixture(x:float,z:float):
    box(self,Vector3(x,3.13,z),Vector3(.40,.12,1.2),dark)
    for xx in [-.11,.11]:
        bar(self,Vector3(x+xx,3.04,z-.5),Vector3(x+xx,3.04,z+.5),.027,glow(Color(.86,.83,.75)))
func plaque(at:Vector3,txt:String,angle:float):
    var l:=Label3D.new()
    l.text=txt
    l.font_size=48
    l.pixel_size=.0025
    l.position=at
    l.rotation.y=angle
    l.outline_size=3
    add_child(l)
func door(at:Vector3,angle:float)->Node3D:
    var hinge:=Node3D.new()
    hinge.position=at
    hinge.rotation.y=angle
    add_child(hinge)
    box(hinge,Vector3(.57,1.08,0),Vector3(1.14,2.16,.095),dark)
    box(hinge,Vector3(.57,1.55,.054),Vector3(.49,.48,.02),metal)
    for y in [.31,.95]:
        box(hinge,Vector3(.56,y,.057),Vector3(.94,.48,.018),pigment("4f4c46"))
    return hinge
func bed(x:float,z:float):
    box(self,Vector3(x,.56,z),Vector3(1.26,.115,2.25),dark,true)
    var mattress:=box(self,Vector3(x,.68,z),Vector3(1.20,.18,2.16),wet)
    mattress.rotation.z=.014
    sheet=box(self,Vector3(x,.78,z+.24),Vector3(1.22,.09,1.71),cloth)
    for i in 10:
        var fold:=box(sheet,Vector3(0,.047,-.74+i*.154),Vector3(1.19,.015,.033),pigment("bab4a6"))
        fold.rotation.y=sin(i*.8)*.06
    pillow=rings(self,Vector3(x,.79,z-.77),[Vector4(0,.52,.29,0),Vector4(.09,.5,.28,0),Vector4(.15,.36,.18,0),Vector4(.16,.001,.001,0)],cloth,8)
    for end in [-1.13,1.13]:
        var ht:float=1.24 if end<0 else 1.01
        for side in [-.63,.63]: bar(self,Vector3(x+side,.05,z+end),Vector3(x+side,ht,z+end),.035,metal)
        bar(self,Vector3(x-.63,ht,z+end),Vector3(x+.63,ht,z+end),.038,metal)
        for i in 6: bar(self,Vector3(x-.49+i*.195,.61,z+end),Vector3(x-.49+i*.195,ht,z+end),.018,metal)
func build():
    var e:=Environment.new()
    e.background_mode=Environment.BG_COLOR
    e.background_color=Color(.058,.06,.062)
    e.ambient_light_source=Environment.AMBIENT_SOURCE_COLOR
    e.ambient_light_color=Color(.62,.63,.65)
    e.ambient_light_energy=.29
    e.tonemap_mode=Environment.TONE_MAPPER_FILMIC
    e.tonemap_exposure=.83
    e.fog_enabled=true
    e.fog_light_color=Color(.13,.13,.14)
    e.fog_density=.018
    var env:=WorldEnvironment.new()
    env.environment=e
    add_child(env)
    for region in [[Vector3(0,0,0),Vector3(7.4,.18,8)],[Vector3(2.45,0,-12.4),Vector3(3.3,.18,16.8)],[Vector3(-2.05,0,-13.3),Vector3(5.7,.18,5.8)]]:
        box(self,region[0]-Vector3.UP*.09,region[1],tiles,true)
        box(self,region[0]+Vector3.UP*3.29,region[1],pigment("413d39",1),true)
    wall(-3.7,0,.14,8)
    wall(3.7,0,.14,8)
    wall(0,4,7.4,.14)
    wall(-.96,-4,5.48,.16)
    wall(3.48,-4,.44,.16)
    wall(4.1,-12.4,.14,16.8)
    wall(.8,-8.38,.14,8.76)
    wall(.8,-17.40,.14,6.80)
    wall(2.45,-20.8,3.3,.16)
    wall(-4.9,-13.3,.14,5.8)
    wall(-2.05,-10.4,5.7,.14)
    wall(-2.05,-16.2,5.7,.14)
    # Recessed window, deep timber frame, transoms and individual opaque panes.
    box(self,Vector3(-.28,1.90,-3.87),Vector3(2.27,1.92,.18),dark)
    var pane:=glow(Color(.33,.33,.32))
    pane.albedo_texture=preload("res://materials.gd").get_map("glass")
    pane.emission_texture=pane.albedo_texture
    pane.emission_energy_multiplier=.78
    for i in 4:
        for j in 3:
            box(self,Vector3(-1.075+i*.53,1.31+j*.58,-3.76),Vector3(.475,.52,.037),pane)
    box(self,Vector3(-.28,.98,-3.67),Vector3(2.38,.095,.46),enamel)
    sash=box(self,Vector3(.58,1.58,-3.68),Vector3(.037,.23,.06),metal)
    sash.rotation.z=-.7
    bed(-.49,-2.05)
    box(self,Vector3(-1.55,.44,-2.91),Vector3(.65,.88,.58),metal,true)
    for y in [.3,.68]:
        box(self,Vector3(-1.55,y,-2.60),Vector3(.54,.26,.035),enamel)
        box(self,Vector3(-1.55,y+.035,-2.565),Vector3(.18,.025,.05),dark)
    bar(self,Vector3(-1.55,.89,-2.92),Vector3(-1.55,1.03,-2.92),.076,enamel)
    bar(self,Vector3(-1.55,1.031,-2.92),Vector3(-1.55,1.033,-2.92),.064,dark)
    box(self,Vector3(-1.46,.98,-2.92),Vector3(.07,.052,.028),enamel)
    for dx in [-.23,.23]:
        for dz in [-.20,.20]:bar(self,Vector3(.74+dx,.03,-2.94+dz),Vector3(.74+dx,.51,-2.94+dz),.025,dark)
    rings(self,Vector3(.74,.48,-2.94),[Vector4(-.06,.29,.25,0),Vector4(.025,.31,.27,0),Vector4(0,.18,.15,0),Vector4(-.013,.001,.001,0)],wet)
    box(self,Vector3(.74,.76,-3.17),Vector3(.50,.45,.055),metal)
    for j in 2:
        var zz:=.27+j*1.48
        bar(self,Vector3(-3.49,.06,zz),Vector3(-3.49,3.19,zz),.057,metal)
        for h in [.37,1.36,2.47]:bar(self,Vector3(-3.49,h,zz),Vector3(-3.49,h+.065,zz),.078,dark)
    for i in 13:
        var zz:=.20+i*.125
        var rib:=rings(self,Vector3(-3.28,.18,zz),[Vector4(0,.082,.045,0),Vector4(.06,.139,.051,0),Vector4(.88,.139,.051,0),Vector4(.98,.095,.046,0),Vector4(1,.001,.001,0)],metal,8)
        ribs.append(rib)
    bracket=box(self,Vector3(-3.34,1.23,1),Vector3(.51,.085,.16),enamel)
    for i in 7:
        var speck:=box(self,bracket.position,Vector3(.025,.027,.023),cloth)
        speck.hide()
        dust.append(speck)
    main_door=door(Vector3(3.18,0,-4),PI/2)
    loop_door=door(Vector3(1.89,0,-20.66),0)
    lever=box(loop_door,Vector3(.94,1.03,.11),Vector3(.22,.045,.095),enamel)
    next_door=door(Vector3(.81,0,-12.80),PI/2)
    spindle_handle=box(next_door,Vector3(.91,1.05,.091),Vector3(.06,.06,.12),enamel)
    var barrier:=box(self,Vector3(.8,1.1,-13.38),Vector3(.13,2.2,1.44),dark,true)
    barrier.visible=false
    deadbolt=barrier.get_child(0)
    for z in [-4.,-20.6]:
        for x in [1.77,3.18]:box(self,Vector3(x,1.1,z+.11),Vector3(.10,2.2,.25),metal)
        box(self,Vector3(2.47,2.23,z+.11),Vector3(1.5,.15,.25),metal)
    box(self,Vector3(3.65,.54,-8.15),Vector3(.68,1.06,1.7),dark,true)
    box(self,Vector3(3.61,1.09,-8.15),Vector3(.77,.07,1.78),metal)
    box(self,Vector3(3.53,1.14,-8.1),Vector3(.27,.024,.34),cloth)
    plaque(Vector3(3.99,1.62,-8.1),"ПОСТ   /   1",-PI/2)
    plaque(Vector3(.92,1.66,-14.18),"07",PI/2)
    plaque(Vector3(2.47,2.46,-20.67),"ВЫХОД",0)
    for z in [-5.5,-10.,-15.,-19.5]:
        fixture(2.48,z)
        lamp(Vector3(2.45,2.66,z),.69,5.5,Color(.82,.84,.83))
        box(self,Vector3(4.0,.91,z),Vector3(.08,.07,4.3),metal)
    fixture(.6,.1)
    ward_lamp=lamp(Vector3(.5,2.62,-.4),1.55,7.,Color(.91,.88,.79))
    ward_lamp.shadow_enabled=true
    lamp(Vector3(-.4,1.9,-3.20),1.45,5.,Color(.84,.85,.86))
    lamp(Vector3(-2.7,2.5,-13.3),.72,6.,Color(.74,.76,.76))
    nurse=figure(self,true)
    nurse.position=Vector3(2.45,.01,-3.99)
    nurse.rotation.y=PI-.2
    add_child(warp)
    for i in 6:
        var f:=box(warp,Vector3(2.48,2.7-i*.04,-11.0-i*1.20),Vector3(3.1,.10,.15),metal)
        f.rotation.z=i*.11
    for i in 13:box(warp,Vector3(-3.1,3.16,.2+i*.125),Vector3(1.10,.019,.069),dark)
    add_child(gurney)
    box(gurney,Vector3(0,.72,0),Vector3(.55,.10,1.4),metal)
    for x in [-.23,.23]:
        for z in [-.58,.58]:
            bar(gurney,Vector3(x,.15,z),Vector3(x,.70,z),.025,metal)
            box(gurney,Vector3(x,.093,z),Vector3(.068,.17,.16),dark)
    gurney.position=Vector3(-.27,0,-13.9)
    hand=bar(self,Vector3(.65,.95,-13.65),Vector3(1.03,1.04,-13.65),.064,pigment("afa394"),.045)
    hand.hide()
    state(0)
func state(which:int):
    version=which
    var wear:=0.10 if which==0 else .95
    for m in [plaster,metal,dark,tiles,enamel]:m.set_shader_parameter("wear",wear)
    sheet.visible=which==0
    pillow.visible=which==0
    for i in ribs.size():ribs[i].visible=which==0 or i%4!=1
    warp.visible=which==2
    gurney.visible=which!=1
    ward_lamp.light_energy=[1.55,.48,.64][which]
    ward_lamp.light_color=Color(.69,.40,.39) if which==2 else Color(.90,.88,.83)
func pulse():
    bracket.position.y=1.27
    create_tween().tween_property(bracket,"position:y",1.23,.15)
    for i in dust.size():
        var d:=dust[i]
        d.position=Vector3(-3.18+randf()*.13,1.2, .91+i*.032)
        d.show()
        var t:=create_tween()
        t.tween_property(d,"position:y",.13,.42+i*.032).set_trans(Tween.TRANS_QUAD).set_ease(Tween.EASE_IN)
        t.tween_callback(d.hide)
