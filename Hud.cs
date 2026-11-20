using Godot;
using System;

public partial class Hud : CanvasLayer
{
    public bool Paused => GetTree().Paused;
    public float Sensitivity { get; private set; } = 1f;
    public bool Shake { get; private set; } = true;

    private bool flicker = true, tape = true, ended;
    private float subTime, tearTime, endingTime, introTime;
    private string endingText;
    private Action restart;
    private Control root, pause, endCard, intro;
    private Label sub, prompt, result;
    private ColorRect blackout;
    private ShaderMaterial film;
    private readonly ConfigFile prefs = new();
    private static readonly Color Ink = new("#ece7db"), Grey = new("#babbb1"), Rule = new("#73736a");

    public void Build(Action restartAction)
    {
        restart = restartAction;
        ProcessMode = ProcessModeEnum.Always;
        prefs.Load("user://perception.cfg");
        Sensitivity = Mathf.Clamp(prefs.GetValue("controls","sensitivity",1f).AsSingle(), .25f, 2.75f);
        Shake = prefs.GetValue("comfort","shake",true).AsBool();
        tape = prefs.GetValue("comfort","tape",true).AsBool();
        flicker = prefs.GetValue("comfort","flicker",true).AsBool();
        root = new Control { MouseFilter = Control.MouseFilterEnum.Ignore };
        AddChild(root);
        Fill(root);
        var overlay = new ColorRect { MouseFilter = Control.MouseFilterEnum.Ignore };
        root.AddChild(overlay);
        Fill(overlay);
        film = new ShaderMaterial { Shader = new Shader { Code = @"
shader_type canvas_item;
uniform float tape=1.0;
uniform float damage=0.0;
void fragment() {
    vec2 cell=floor(FRAGCOORD.xy*0.48);
    float grain=fract(sin(dot(cell + floor(TIME*11.0),vec2(12.9898,78.233)))*43758.5453);
    float line=step(0.76,fract(FRAGCOORD.y/3.));
    float rim=smoothstep(0.30,0.72,length(UV-0.5));
    float slip=1.-smoothstep(0.002,0.016,abs(UV.y-fract(TIME*0.73)));
    COLOR=vec4(mix(vec3(0.06),vec3(0.28,0.06,0.05),slip*damage),tape*(0.13*rim+0.046*grain+0.031*line)+damage*slip*0.20);
}" } };
        overlay.Material = film;
        film.SetShaderParameter("tape",tape ? 1f : 0f);
        intro = new VBoxContainer { MouseFilter = Control.MouseFilterEnum.Ignore };
        root.AddChild(intro);
        intro.Position = new Vector2(45,35);
        Tag(intro,"ДУРКА    /    ПРОБНАЯ ГЛАВА",16);
        var title = Tag(intro,"Сквозняк",38);
        title.AddThemeFontOverride("font", new SystemFont { FontNames = new[]{"Georgia","DejaVu Serif","serif"} });
        Tag(intro,"WASD   шаг      Мышь   взгляд      E   действие      Esc   пауза",17);
        foreach(var element in intro.GetChildren())
        {
            if(element is Label label)
            {
                label.AddThemeColorOverride("font_outline_color",new Color(.035f,.038f,.033f,.86f));
                label.AddThemeConstantOverride("outline_size",4);
            }
        }
        introTime = 10;
        sub = BottomLabel(-142,-76,24);
        prompt = BottomLabel(-72,-26,19);
        pause = Shade(root, new Color(.055f,.06f,.055f,.96f));
        var box = CenterBox(pause);
        Tag(box,"ПАЛАТА № 6     /     ДЕЖУРНЫЙ ЛИСТ",16);
        Tag(box,"Пауза",43);
        box.AddChild(new HSeparator());
        var sensitivity = Tag(box,"",20);
        void Display() => sensitivity.Text = $"Чувствительность мыши     {Sensitivity:0.00}";
        Display();
        var slider = new HSlider { MinValue=.25, MaxValue=2.75, Step=.05, Value=Sensitivity, CustomMinimumSize=new Vector2(390,28) };
        box.AddChild(slider);
        slider.ValueChanged += v => { Sensitivity=(float)v; Display(); Save(); };
        Toggle(box,"Тряска камеры",Shake,v=>Shake=v);
        Toggle(box,"Мигание при искажении",flicker,v=>flicker=v);
        Toggle(box,"Помехи VHS",tape,v=>{ tape=v; film.SetShaderParameter("tape",v ? 1f : 0f); });
        Tag(box,"WASD   шаг      Мышь   взгляд      E   действие",16);
        Button(box,"Продолжить",()=>SetPaused(false));
        Button(box,"Выйти из игры",()=>GetTree().Quit());
        pause.Hide();
        blackout = (ColorRect)Shade(root, Colors.Black);
        blackout.Hide();
        endCard = new CenterContainer();
        blackout.AddChild(endCard);
        Fill(endCard);
        var end = new VBoxContainer();
        end.AddThemeConstantOverride("separation",18);
        endCard.AddChild(end);
        Tag(end,"ДУРКА     /     СКВОЗНЯК",17);
        result = Tag(end,"",34);
        Button(end,"Пройти снова",()=>restart());
        Button(end,"Закрыть игру",()=>GetTree().Quit());
        Input.MouseMode=Input.MouseModeEnum.Captured;
    }

    private static void Fill(Control node) => node.SetAnchorsAndOffsetsPreset(Control.LayoutPreset.FullRect);
    private static Label Tag(Control parent,string text,int size)
    {
        var label = new Label { Text=text, MouseFilter=Control.MouseFilterEnum.Ignore };
        parent.AddChild(label);
        label.AddThemeFontSizeOverride("font_size",size);
        label.AddThemeColorOverride("font_color",size<20 ? Grey : Ink);
        return label;
    }

    private Label BottomLabel(int top,int bottom,int size)
    {
        var label=Tag(root,"",size);
        label.SetAnchorsAndOffsetsPreset(Control.LayoutPreset.BottomWide);
        label.OffsetLeft=75; label.OffsetRight=-75; label.OffsetTop=top; label.OffsetBottom=bottom;
        label.HorizontalAlignment=HorizontalAlignment.Center;
        label.VerticalAlignment=VerticalAlignment.Center;
        label.AutowrapMode=TextServer.AutowrapMode.WordSmart;
        label.AddThemeColorOverride("font_outline_color",new Color(.025f,.025f,.025f,.94f));
        label.AddThemeConstantOverride("outline_size",6);
        return label;
    }
    private static Control Shade(Control parent,Color color)
    {
        var rect=new ColorRect { Color=color }; parent.AddChild(rect); Fill(rect); return rect;
    }
    private static VBoxContainer CenterBox(Control parent)
    {
        var center=new CenterContainer(); parent.AddChild(center); Fill(center);
        var box=new VBoxContainer { CustomMinimumSize=new Vector2(440,0) };
        center.AddChild(box); box.AddThemeConstantOverride("separation",11); return box;
    }
    private static void Button(Control parent,string text,Action click)
    {
        var b=new Button { Text=text, CustomMinimumSize=new Vector2(0,42) };
        b.AddThemeFontSizeOverride("font_size",19);
        var normal=new StyleBoxFlat { BgColor=new Color("#30342f"), BorderColor=Rule, BorderWidthLeft=2 };
        var hover=(StyleBoxFlat)normal.Duplicate(); hover.BgColor=new Color("#44483f");
        b.AddThemeStyleboxOverride("normal",normal); b.AddThemeStyleboxOverride("hover",hover);
        parent.AddChild(b); b.Pressed+=click;
    }
    private void Toggle(Control parent,string name,bool current,Action<bool> change)
    {
        var cb=new CheckButton { Text=name, ButtonPressed=current };
        cb.AddThemeFontSizeOverride("font_size",20);
        parent.AddChild(cb); cb.Toggled+=v=>{change(v);Save();};
    }
    private void Save()
    {
        prefs.SetValue("comfort","shake",Shake); prefs.SetValue("comfort","tape",tape);
        prefs.SetValue("comfort","flicker",flicker); prefs.SetValue("controls","sensitivity",Sensitivity);
        prefs.Save("user://perception.cfg");
    }
    public void Caption(string text,float seconds=4) { if(!ended){sub.Text=text;subTime=seconds;} }
    public void Prompt(string text) { if(!ended) prompt.Text=text; }
    public void Tear(float strength) { if(flicker) tearTime=Mathf.Clamp(strength,0,1)*.45f; }

    public void End(string text)
    {
        if(ended) return;
        ended=true; endingText=text; sub.Hide(); prompt.Hide(); intro.Hide(); endCard.Hide(); blackout.Show();
        blackout.Modulate=new Color(1,1,1,0); 
    }
    private void SetPaused(bool value)
    {
        GetTree().Paused=value; pause.Visible=value;
        Input.MouseMode=value ? Input.MouseModeEnum.Visible : Input.MouseModeEnum.Captured;
    }
    public override void _UnhandledInput(InputEvent e)
    {
        if(!ended && e is InputEventKey {Pressed:true,Echo:false,Keycode:Key.Escape})
        { SetPaused(!Paused); GetViewport().SetInputAsHandled(); }
    }
    public override void _Process(double delta)
    {
        if(root==null || Paused) return;
        float dt=(float)delta;
        if(ended)
        {
            endingTime+=dt;
            blackout.Modulate=new Color(1,1,1,Mathf.SmoothStep(0,1,Mathf.Clamp((endingTime-1.4f)/2.1f,0,1)));
            if(endingTime>4.7f && !endCard.Visible)
            { result.Text=endingText; endCard.Show(); Input.MouseMode=Input.MouseModeEnum.Visible; }
            return;
        }
        introTime-=dt; intro.Modulate=new Color(1,1,1,Mathf.Clamp(introTime,0,1));
        subTime-=dt; sub.Modulate=new Color(1,1,1,Mathf.Clamp(subTime*3,0,1));
        tearTime=Mathf.Max(0,tearTime-dt); film.SetShaderParameter("damage",flicker ? tearTime*2f : 0f);
    }
}
