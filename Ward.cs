using Godot;
using System.Collections.Generic;
using static WardSurfaces;

public partial class Ward : Node3D
{
    private const float Height = 3.28f;
    private readonly WardSurfaces surfaces = new();
    private readonly List<(MeshInstance3D Mesh, Finish Finish, bool Home)> pieces = new();
    private readonly List<(OmniLight3D Light, float Power, bool Home)> lamps = new();
    private readonly List<(Node3D Node, int State, bool Home)> variations = new();
    private readonly List<MeshInstance3D> dust = new();
    private readonly BoxMesh cube = new() { Size = Vector3.One };
    private StaticBody3D structure;
    private Node3D bracket, sash, trolley;
    private Godot.Environment atmosphere;
    private float tremor = -1;
    private int currentState, homeState;
    private bool home, closed;

    public void Build()
    {
        structure = new StaticBody3D { Name = "Общая_геометрия" };
        AddChild(structure);
        atmosphere = new Godot.Environment {
            BackgroundMode = Godot.Environment.BGMode.Color, BackgroundColor = new Color(.075f,.081f,.083f),
            AmbientLightSource = Godot.Environment.AmbientSource.Color, AmbientLightColor = new Color(.65f,.68f,.70f),
            AmbientLightEnergy = .34f, ReflectedLightSource = Godot.Environment.ReflectionSource.Disabled,
            TonemapMode = Godot.Environment.ToneMapper.Filmic
        };
        AddChild(new WorldEnvironment { Environment = atmosphere });
        Hall();
        home = true;
        Room(true);
        home = false;
        Room(false);
        State(0);
    }

    private MeshInstance3D Box(float x,float y,float z,float w,float h,float d, Finish f, bool solid=false, Node3D owner=null)
    {
        var piece = new MeshInstance3D {
            Mesh = cube, Position = new Vector3(x,y,z), Scale = new Vector3(w,h,d),
            MaterialOverride = surfaces.Get(f,0)
        };
        (owner ?? this).AddChild(piece);
        pieces.Add((piece,f,home));
        if (solid) Collider(x,y,z,w,h,d);
        return piece;
    }

    private void Collider(float x,float y,float z,float w,float h,float d)
    {
        structure.AddChild(new CollisionShape3D { Position = new Vector3(x,y,z), Shape = new BoxShape3D { Size = new Vector3(w,h,d) } });
    }

    private Node3D Variant(int state)
    {
        var node = new Node3D();
        AddChild(node);
        variations.Add((node,state,home));
        return node;
    }

    private void Wall(float x,float z,float w,float d)
    {
        Box(x,Height*.5f,z,w,Height,d,Finish.Lime,true);
        Box(x,.69f,z,w+.012f,1.38f,d+.012f,Finish.Lime);
        Box(x,1.375f,z,w+.026f,.036f,d+.026f,Finish.Steel);
        Box(x,.082f,z,w+.036f,.16f,d+.036f,Finish.Rubber);
    }

    private void Label(string value, Vector3 pos, float yaw, float size, bool ink=false, Node3D owner=null, float roll=0)
    {
        var label = new Label3D {
            Text = value, PixelSize = size, FontSize = 48, OutlineSize = 0, Position = pos,
            Rotation = new Vector3(0,yaw,roll), Modulate = ink ? new Color(.13f,.14f,.13f) : new Color(.86f,.84f,.78f),
            Shaded = false, DoubleSided = false
        };
        (owner ?? this).AddChild(label);
    }

    private void Arch(float z, string number)
    {
        foreach (float sz in new[]{z-.97f,z+.97f})
            Box(2,1.18f,sz,.31f,2.36f,.14f,Finish.Wood,true);
        Box(2,2.77f,z,.19f,1.02f,1.94f,Finish.Lime,true);
        Box(2,2.28f,z,.32f,.14f,2.0f,Finish.Wood);
        Box(1.873f,1.9f,z+1.43f,.056f,.38f,.64f,Finish.Rubber);
        Label(number,new Vector3(1.838f,1.92f,z+1.43f),-Mathf.Pi/2,.0050f);
        Label("ПАЛАТА",new Vector3(1.837f,1.77f,z+1.43f),-Mathf.Pi/2,.00093f);
    }

    private void Fixture(float x,float z,bool shadow,float power)
    {
        Box(x,3.17f,z,.43f,.14f,1.16f,Finish.Steel);
        Box(x,3.09f,z,.35f,.025f,1.08f,Finish.Rubber);
        foreach(float dx in new[]{-.095f,.095f})
            Box(x+dx,3.055f,z,.056f,.055f,.94f,Finish.Lamp);
        var light = new OmniLight3D {
            Position = new Vector3(x,2.78f,z), OmniRange = 6.65f, OmniAttenuation=1.57f,
            LightEnergy=power*.72f, LightColor=new Color(.95f,.94f,.88f), ShadowEnabled=shadow,
            ShadowBias=.07f
        };
        AddChild(light);
        lamps.Add((light,power,home));
    }

    private void Hall()
    {
        Box(0,-.13f,-3,4.18f,.26f,32,Finish.Floor,true);
        Box(0,Height+.10f,-3,4.20f,.20f,32,Finish.Ceiling,true);
        Wall(-2.08f,-3,.16f,32);
        Wall(0,13.08f,4.30f,.16f);
        Wall(0,-19.08f,4.30f,.16f);
        foreach(var (a,b) in new[]{(-19f,-4.9f),(-3.1f,4.1f),(5.9f,13f)})
            Wall(2.08f,(a+b)*.5f,.16f,b-a);
        Arch(5,"06");
        Arch(-4,"07");
        foreach (float z in new[]{-16.1f,-8.1f,-.1f,7.9f})
        {
            Fixture(-.17f,z,z==-.1f,1.50f);
            Box(-1.94f,1.02f,z,.105f,.105f,6.75f,Finish.Wood);
            foreach (float dz in new[]{-2.80f,-.78f,1.30f})
                Box(-1.97f,.97f,z+dz,.08f,.15f,.058f,Finish.Steel);
        }
        // Painted-over doors repeat beyond the loop. Their frames are embedded in the wall.
        foreach(var (z,n) in new[]{(-15f,"03"),(-8.6f,"04"),(-1.6f,"05"),(6.6f,"08")})
        {
            Box(-1.973f,1.11f,z,.09f,2.22f,1.35f,Finish.Wood);
            Box(-1.918f,1.51f,z,.032f,.49f,.38f,Finish.Rubber);
            Box(-1.896f,1.51f,z,.018f,.39f,.29f,Finish.Window);
            Box(-1.914f,.92f,z+.47f,.10f,.045f,.18f,Finish.Steel);
            Label(n,new Vector3(-1.90f,1.93f,z),Mathf.Pi/2,.0020f);
        }
        Label("ПОСТ",new Vector3(0,2.40f,12.968f),Mathf.Pi,.0075f);
        Label("ДЕЖУРНАЯ СЕСТРА",new Vector3(0,2.08f,12.967f),Mathf.Pi,.00165f);
        Box(-1.24f,.49f,10.92f,1.31f,.93f,1.92f,Finish.Wood,true);
        Box(-1.24f,.99f,10.92f,1.43f,.08f,2.04f,Finish.Enamel);
        // Open shift book, pen, angled telephone receiver and table lamp.
        Box(-1.22f,1.045f,10.43f,.58f,.035f,.39f,Finish.Paper);
        Box(-1.19f,1.068f,10.43f,.012f,.008f,.35f,Finish.Steel);
        for(int i=0;i<6;i++) Box(-1.34f,1.07f,10.30f+i*.045f,.18f,.006f,.006f,Finish.Rubber);
        Box(-.89f,1.052f,10.57f,.020f,.025f,.26f,Finish.Steel).RotationDegrees = new Vector3(0,18,0);
        Box(-1.26f,1.08f,11.45f,.32f,.13f,.31f,Finish.Rubber);
        Box(-1.26f,1.18f,11.48f,.44f,.08f,.084f,Finish.Ink);
        foreach (float dx in new[]{-.17f,.17f}) Box(-1.26f+dx,1.145f,11.48f,.09f,.12f,.14f,Finish.Rubber);
        Board();
        Fixture(0,11.65f,false,1.27f);
        Clock(0, 2.36f, -18.96f);

        var twisted = Variant(2);
        // A second corridor hangs upside-down high inside the original, seen in fragments.
        for(int i=0;i<4;i++)
        {
            float z = -9.2f+i*4;
            Box(-1.48f,2.68f,z,.56f,.065f,1.48f,Finish.Linen,false,twisted);
            Box(-1.80f,2.91f,z,.08f,.52f,1.52f,Finish.Steel,false,twisted);
            Box(1.87f,2.83f,z,.14f,.12f,.66f,Finish.Ember,false,twisted);
        }
        // The registration window folds across the ceiling. Its sill and panes
        // match the ones in the ward, but the trees now stand above the player.
        Box(0,3.232f,-4.65f,2.18f,.065f,1.72f,Finish.Wood,false,twisted);
        Box(0,3.185f,-4.65f,2.03f,.024f,1.59f,Finish.Window,false,twisted);
        foreach(float x in new[]{-.98f,-.33f,.33f,.98f})
            Box(x,3.155f,-4.65f,.045f,.058f,1.62f,Finish.Steel,false,twisted);
        foreach(float z in new[]{-5.43f,-4.66f,-3.87f})
            Box(0,3.155f,z,2.01f,.058f,.048f,Finish.Steel,false,twisted);
        Box(0,3.105f,-3.80f,2.34f,.18f,.13f,Finish.Enamel,false,twisted);
        trolley = new Node3D { Visible=false };
        AddChild(trolley);
        Box(0,.012f,0,1.86f,.008f,.48f,Finish.Ink,false,trolley);
        foreach(float dx in new[]{-.66f,.66f})
            Box(dx,.015f,.32f,.11f,.009f,.41f,Finish.Ink,false,trolley);
    }

    private void Board()
    {
        Box(-1.96f,1.94f,9.0f,.074f,1.0f,1.52f,Finish.Wood);
        foreach(var (z,y,tilt) in new[]{(8.58f,2.10f,3f),(9.02f,1.89f,-2f),(9.43f,2.08f,5f),(8.55f,1.68f,-6f)})
        {
            var paper=Box(-1.911f,y,z,.012f,.37f,.27f,Finish.Paper);
            paper.RotationDegrees=new Vector3(tilt,0,0);
            for(int i=0;i<4;i++) Box(-1.898f,y+.075f-i*.048f,z,.012f,.007f,.16f-i*.012f,Finish.Enamel);
            Box(-1.89f,y+.15f,z,.018f,.017f,.016f,Finish.Rubber);
        }
    }

    private void Clock(float x,float y,float z)
    {
        Box(x,y,z,.71f,.72f,.075f,Finish.Rubber);
        Box(x,y,z+.044f,.65f,.66f,.025f,Finish.Paper);
        for(int i=0;i<12;i++)
        {
            float a=i*Mathf.Tau/12;
            Box(x+.27f*Mathf.Sin(a),y+.27f*Mathf.Cos(a),z+.063f,.025f,.05f,.010f,Finish.Ink).RotationDegrees=new Vector3(0,0,-30*i);
        }
        Box(x+.071f,y+.081f,z+.08f,.025f,.235f,.012f,Finish.Ink).RotationDegrees=new Vector3(0,0,-42);
        Box(x-.081f,y-.020f,z+.083f,.18f,.024f,.012f,Finish.Ink).RotationDegrees=new Vector3(0,0,12);
    }

    private void Room(bool main)
    {
        float origin=main ? 1 : -9;
        Box(6,-.13f,origin+5,8.04f,.26f,10.04f,Finish.Floor,true);
        Box(6,Height+.1f,origin+5,8.04f,.20f,10.04f,Finish.Ceiling,true);
        Wall(10.08f,origin+5,.16f,10.0f);
        Wall(6,origin,8.0f,.16f);
        if(main) Wall(6,11.08f,8.0f,.16f);
        // The long interior partition crosses the z=1 shared wall, whose finish is built by the home room.
        Bed(7.35f,origin+7.20f);
        Table(8.52f,origin+6.78f);
        Chair(5.20f,origin+7.85f);
        Window(origin+2.45f,main);
        Fixture(5.20f,origin+4.65f,main,1.54f);
        Label(main ? "6" : "7",new Vector3(9.97f,2.57f,origin+7.35f),-Mathf.Pi/2,.0080f);
        Label("ТЕРАПЕВТИЧЕСКОЕ ОТД.",new Vector3(9.97f,2.29f,origin+7.35f),-Mathf.Pi/2,.00125f);
        if(main) { Radiator(); PersonalEffects(); }
        else
        {
            // Old hook scars on the other side occupy exactly the same place as the bracket.
            Box(7,1.06f,.898f,.28f,.18f,.04f,Finish.Steel);
            Label("II",new Vector3(7,1.26f,.889f),Mathf.Pi,.0022f);
            var t=Variant(2);
            Box(7,2.45f,-5.65f,1.30f,.15f,2.13f,Finish.Linen,false,t).RotationDegrees = new Vector3(0,0,19);
            Box(7.52f,2.75f,-6.61f,.06f,.83f,.06f,Finish.Steel,false,t);
        }
    }

    private void PersonalEffects()
    {
        // Folded nightshirt on the seat. The two folded edges catch the light.
        var shirt=Box(5.18f,.52f,8.88f,.34f,.073f,.31f,Finish.Linen);
        shirt.RotationDegrees=new Vector3(0,9,-4);
        Box(5.19f,.56f,8.88f,.30f,.049f,.25f,Finish.Linen).RotationDegrees=new Vector3(0,6,-2);
        // Pair of backless slippers pushed only partly beneath the bed.
        foreach(float x in new[]{6.53f,6.78f})
        {
            var sole=Box(x,.041f,8.42f,.13f,.055f,.29f,Finish.Rubber);
            sole.RotationDegrees=new Vector3(0,x<6.6f?-11:6,0);
            Box(x,.085f,8.36f,.14f,.076f,.14f,Finish.Wood).RotationDegrees=sole.RotationDegrees;
        }
        // A discarded newspaper is tucked under the bedside cup.
        Box(8.49f,.809f,7.73f,.44f,.013f,.28f,Finish.Paper);
        for(int i=0;i<5;i++)
            Box(8.38f,.820f,7.65f+i*.032f,.096f,.005f,.005f,Finish.Steel);
        Box(8.63f,.82f,7.73f,.105f,.004f,.107f,Finish.Enamel);
        Clock(5.28f,2.37f,1.124f);
        // Pleated privacy curtain stacked against the window rail.
        Box(9.54f,2.99f,3.45f,.042f,.043f,2.88f,Finish.Steel);
        foreach(float z in new[]{2.05f,4.85f}) Box(9.74f,2.99f,z,.40f,.037f,.037f,Finish.Steel);
        for(int i=0;i<8;i++)
        {
            float z=4.42f+.052f*i;
            float height=1.61f+.027f*Mathf.Sin(i*2.2f);
            Box(9.53f+Mathf.Sin(i*1.8f)*.055f,2.087f,z,.062f,height,.082f,Finish.Linen)
                .RotationDegrees=new Vector3(0,(i%2==0?1:-1)*23,0);
            Box(9.53f,2.934f,z,.016f,.103f,.015f,Finish.Steel);
        }
        // Opaque lost-property cabinet on the far wall, with pressed-metal louvres.
        Box(3.04f,.93f,10.58f,.94f,1.84f,.61f,Finish.Enamel,true);
        foreach(float dx in new[]{-.225f,.225f})
        {
            Box(3.04f+dx,.96f,10.25f,.43f,1.72f,.043f,Finish.Enamel);
            for(int i=0;i<4;i++) Box(3.04f+dx,1.51f+i*.044f,10.221f,.22f,.012f,.018f,Finish.Rubber);
            Box(3.04f+dx+.13f,.96f,10.21f,.031f,.102f,.04f,Finish.Steel);
        }
    }

    private void Bed(float x,float z)
    {
        Collider(x,.36f,z,1.22f,.72f,2.24f);
        foreach(float end in new[]{-1.10f,1.10f})
        {
            float top = end>0 ? .91f : 1.19f;
            foreach(float side in new[]{-.57f,.57f})
            {
                Box(x+side,top*.5f,z+end,.054f,top,.054f,Finish.Steel);
                Box(x+side,.065f,z+end,.10f,.13f,.12f,Finish.Rubber);
            }
            Box(x,top,z+end,1.18f,.06f,.063f,Finish.Steel);
            for(int i=0;i<5;i++) Box(x-.38f+i*.19f,(top+.38f)/2,z+end,.027f,top-.38f,.028f,Finish.Steel);
        }
        Box(x,.425f,z,1.19f,.13f,2.18f,Finish.Steel);
        Box(x,.555f,z,1.13f,.20f,2.13f,Finish.Linen);
        Box(x,.715f,z-.74f,.82f,.115f,.43f,Finish.Linen).RotationDegrees = new Vector3(-6,2,0);
        Box(x,.683f,z+.37f,1.17f,.072f,1.19f,Finish.Linen);
        Box(x,.62f,z+.94f,1.14f,.16f,.042f,Finish.Linen);
        Box(x-.565f,.59f,z+.34f,.042f,.20f,1.15f,Finish.Linen);
        for(int i=0;i<4;i++)
            Box(x-.27f+i*.16f,.722f,z+.29f,.023f,.014f,.48f+i*.065f,Finish.Linen)
                .RotationDegrees = new Vector3(0,-13+i*9,0);
    }

    private void Table(float x,float z)
    {
        Box(x,.38f,z,.58f,.72f,.49f,Finish.Enamel,true);
        Box(x,.77f,z,.65f,.06f,.55f,Finish.Linen);
        Box(x,.61f,z-.252f,.54f,.20f,.022f,Finish.Wood);
        Box(x,.62f,z-.282f,.13f,.028f,.040f,Finish.Steel);
        var cup = new CylinderMesh { TopRadius=.085f, BottomRadius=.068f, Height=.168f, RadialSegments=10, Rings=0 };
        var mesh = new MeshInstance3D { Mesh=cup, Position=new Vector3(x,.884f,z) };
        AddChild(mesh); pieces.Add((mesh,Finish.Ceramic,home));
        var tea = new CylinderMesh { TopRadius=.066f, BottomRadius=.066f,Height=.004f,RadialSegments=10,Rings=0 };
        var liquid = new MeshInstance3D { Mesh=tea,Position=new Vector3(x,.969f,z) };
        AddChild(liquid);pieces.Add((liquid,Finish.Rubber,home));
        Box(x+.101f,.925f,z,.069f,.018f,.022f,Finish.Ceramic);
        Box(x+.13f,.888f,z,.018f,.09f,.022f,Finish.Ceramic);
        Box(x+.100f,.848f,z,.068f,.018f,.022f,Finish.Ceramic);
    }

    private void Chair(float x,float z)
    {
        Collider(x,.44f,z,.49f,.88f,.50f);
        foreach(float dx in new[]{-.207f,.207f})
        foreach(float dz in new[]{-.205f,.205f})
            Box(x+dx,dz>0 ? .46f : .217f,z+dz,.048f,dz>0 ? .92f : .434f,.048f,Finish.Steel);
        Box(x,.435f,z,.52f,.050f,.49f,Finish.Wood);
        foreach(float dx in new[]{-.177f,.177f})
            Box(x+dx,.476f,z,.16f,.049f,.46f,Finish.Rubber).RotationDegrees=new Vector3(0,0,dx*31);
        Box(x,.455f,z,.24f,.05f,.47f,Finish.Rubber);
        Box(x,.787f,z+.218f,.50f,.22f,.062f,Finish.Wood);
    }

    private void Window(float z,bool main)
    {
        Box(9.955f,1.98f,z,.08f,1.89f,2.15f,Finish.Wood);
        Box(9.906f,1.99f,z,.032f,1.74f,2.01f,Finish.Window);
        Box(9.80f,1.015f,z,.42f,.11f,2.37f,Finish.Enamel);
        foreach(float offset in new[]{-.99f,-.33f,.33f,.99f})
            Box(9.88f,1.96f,z+offset,.075f,1.82f,.045f,Finish.Steel);
        foreach(float y in new[]{1.14f,1.86f,2.36f,2.82f})
            Box(9.878f,y,z,.078f,.044f,2.00f,Finish.Steel);
        if(main)
        {
            sash=new Node3D {Position=new Vector3(9.848f,2.80f,z-.67f),RotationDegrees=new Vector3(0,0,-18)};
            AddChild(sash);
            Box(0,-.22f,0,.034f,.41f,.60f,Finish.Window,false,sash);
            Box(-.017f,-.43f,0,.055f,.04f,.65f,Finish.Wood,false,sash);
            foreach(float dz in new[]{-.307f,.307f}) Box(-.017f,-.22f,dz,.055f,.45f,.038f,Finish.Wood,false,sash);
            Box(-.045f,-.385f,0,.055f,.034f,.10f,Finish.Steel,false,sash);
        }
        var daylight=new OmniLight3D { Position=new Vector3(9.20f,2.04f,z), OmniRange=5.9f, OmniAttenuation=1.45f,
            LightColor=new Color(.83f,.88f,.91f), LightEnergy=1.54f };
        AddChild(daylight); lamps.Add((daylight,1.54f,home));
    }

    private void Radiator()
    {
        bracket=new Node3D { Position=new Vector3(7,1.04f,1.17f) };AddChild(bracket);
        Box(0,0,0,.26f,.17f,.12f,Finish.Steel,false,bracket);
        Box(0,.03f,.086f,.34f,.057f,.12f,Finish.Steel,false,bracket);
        foreach(float dx in new[]{-.093f,.093f}) Box(dx,0,.063f,.025f,.026f,.023f,Finish.Ink,false,bracket);
        Collider(7,.46f,1.23f,1.75f,.92f,.34f);
        for(int s=0;s<3;s++)
        {
            var set=Variant(s);
            for(int i=0;i<15;i++)
            {
                if(s>0 && (i==2 || i==3 || i>=11)) continue;
                float x=6.26f+i*.108f;
                Box(x,.53f,1.24f,.086f,.73f,.23f,Finish.Steel,false,set);
                Box(x,.54f,1.355f,.048f,.60f,.046f,Finish.Enamel,false,set);
            }
            Box(7,.27f,1.20f,1.65f,.075f,.074f,Finish.Steel,false,set);
            if(s==0)
            {
                Box(8.05f,1.61f,1.19f,.084f,3.21f,.084f,Finish.Steel,false,set);
                Box(7.92f,.81f,1.19f,.30f,.073f,.073f,Finish.Steel,false,set);
                Box(8.05f,1.76f,1.19f,.125f,.064f,.12f,Finish.Steel,false,set);
            }
            if(s==2)
                for(int i=0;i<15;i++)
                {
                    float x=6.25f+i*.105f;
                    Box(x,2.72f,1.095f,.037f,1.02f,.020f,Finish.Ink,false,set);
                    Box(x,3.245f,1.69f,.045f,.024f,1.25f+i*.023f,Finish.Ink,false,set);
                }
        }
        for(int i=0;i<15;i++)
        {
            var chip=Box(7,1,1.31f,.02f+(i%3)*.008f,.021f,.019f,Finish.Paper);
            chip.Visible=false; dust.Add(chip);
        }
    }

    public void State(int state)
    {
        currentState=homeState=Mathf.Clamp(state,0,2); Refresh();
    }

    public void RoomState(int state)
    {
        homeState=Mathf.Clamp(state,0,2); Refresh();
    }

    private void Refresh()
    {
        atmosphere.AmbientLightEnergy=currentState == 0 ? .32f : .19f;
        foreach(var p in pieces) p.Mesh.MaterialOverride=surfaces.Get(p.Finish,p.Home ? homeState : currentState);
        foreach(var v in variations) v.Node.Visible=v.State==(v.Home ? homeState : currentState);
        foreach(var lamp in lamps)
        {
            int s=lamp.Home ? homeState : currentState;
            bool failed = lamp.Light.Position.Z < 0 || (!lamp.Home && lamp.Light.Position.Z > 6 && lamp.Light.Position.Z < 10);
            lamp.Light.LightEnergy=lamp.Power*(s==0 ? .72f : failed ? .17f : .57f);
            if (s>0 && lamp.Light.Position.X>9) lamp.Light.LightEnergy=lamp.Power*.77f;
        }
    }

    public void Rattle() { tremor=0; }

    public void WindowClosed()
    {
        if(closed || sash==null) return;
        closed=true;
        CreateTween().TweenProperty(sash,"rotation:z",0.0f,.39f);
    }

    public void Finale(float time)
    {
        if(trolley==null) return;
        trolley.Visible=true;
        float sweep=Mathf.PosMod(time*.8f,7f);
        trolley.Position=new Vector3(2.02f,.004f,-7.5f+sweep);
    }

    public override void _Process(double delta)
    {
        if(tremor<0 || bracket==null) return;
        tremor+=(float)delta;
        bracket.Position=new Vector3(7,1.04f+Mathf.Sin(tremor*95)*.026f*Mathf.Exp(-tremor*12),1.17f);
        for(int i=0;i<dust.Count;i++)
        {
            var p=dust[i]; float t=tremor-i*.010f;
            p.Visible=t>=0 && t<.58f;
            p.Position=new Vector3(7+(i%6-2.7f)*.035f+Mathf.Sin(i*3.1f)*t*.34f,
                1.38f+i%3*.034f-3.9f*t*t,1.32f+t*(.13f+i%4*.07f));
        }
        if(tremor>.78f) { tremor=-1; bracket.Position=new Vector3(7,1.04f,1.17f); }
    }
}
