using Godot;
using System;
using System.Collections.Generic;

public partial class Episode : Node3D
{
    private enum Beat { Morning, Abandoned, Loop, Handle, Neighbor, Footsteps, Return, Vigil, Ending }
    private enum Focus { None, Rise, Pipe, Window, Home, Neighbor, Loop, Desk, Bed, Mug, Chair, OtherPipe, OtherBed }
    private readonly CaseNotes _notes = new();
    private readonly Ward _ward = new();
    private readonly Patient _patient = new();
    private readonly Hud _hud = new();
    private readonly Soundscape _sound = new();
    private readonly EpisodeDoor _home = new(), _neighbor = new(), _loop = new();
    private static readonly Vector3 Pipe = new(7, 1.02f, 1.21f);
    private static readonly Vector3 Sash = new(9.78f, 1.65f, 3.45f);
    private static readonly Vector3 HomeDoor = new(2, 1.1f, 5);
    private static readonly Vector3 NextDoor = new(2, 1.1f, -4);
    private static readonly Vector3 LoopDoor = new(0, 1.1f, -12);
    private static readonly Vector3 Post = new(-1.12f, 1, 10.9f);
    private static readonly Vector3 OppositePipe = new(7, 1.02f, .79f);
    private readonly List<double> _taps = new(), _pattern = new();
    private Node3D _nurse;
    private MeshInstance3D _pages;
    private Node3D _pocketHandle;
    private Beat _beat;
    private int _world, _passes, _echoIndex, _initialPulses;
    private double _time, _echoStart = -1, _nextPulse, _sashTime, _loopStart = -1, _previousOpen = -1,
        _previousInterval, _scareStart = -1, _nextFootstep, _nextReminder, _phaseStart, _endTime;
    private bool _stood, _windowShut, _heard, _visitedPost, _draft, _visitedHall, _ended,
        _quiet, _braced, _metHand, _carried, _installed, _turnAway, _firstCalmKnock;
    private float _holdTime, _best;
    private Focus _focus;
    private string _endingText = "";
    public bool HasHandle => _carried;
    public bool HandleInstalled => _installed;

    public override void _Ready()
    {
        AddChild(_ward);
        _ward.Build();
        AddChild(_notes);
        _notes.Build();
        AddChild(_home);
        AddChild(_neighbor);
        AddChild(_loop);
        _home.Build(new Vector3(2,0,5.87f), Mathf.Pi/2, 1.74f, -1.52f, true);
        _neighbor.Build(new Vector3(2,0,-3.13f), Mathf.Pi/2, 1.74f, -1.52f, false);
        _loop.Build(new Vector3(-1.24f,0,-12), 0, 2.48f, 1.52f, true);
        for(int side=-1;side<=1;side+=2)
            PortalWall(new Vector3(side*1.62f,1.35f,-12),new Vector3(.76f,2.7f,.18f));
        PortalWall(new Vector3(0,2.58f,-12),new Vector3(2.5f,.44f,.18f));
        _home.Snap(.68f);

        AddChild(_patient);
        _patient.Build();
        _patient.Position = new Vector3(6.76f,.035f,8.12f);
        _patient.Face(1.05f);
        _patient.Awake = false;
        _patient.Mobile = false;
        _nurse = Patient.Figure(true);
        AddChild(_nurse);
        _nurse.Position = new Vector3(3.53f,0,4.29f);
        _nurse.Rotation = new Vector3(0,-2.1f,0);
        _pocketHandle = new Node3D { Visible=false, Position=new Vector3(.28f,.90f,-.26f) };
        _patient.AddChild(_pocketHandle);
        EpisodeDoor.Box(_pocketHandle,Vector3.Zero,new Vector3(.042f,.045f,.17f),"aaa698");
        EpisodeDoor.Box(_pocketHandle,new Vector3(0,.045f,.07f),new Vector3(.043f,.11f,.044f),"818579");
        _pages=EpisodeDoor.Box(this,new Vector3(-1.09f,1.03f,10.84f),new Vector3(.37f,.012f,.28f),"b8b7a7");
        _pages.Rotation = new Vector3(0,-.12f,0);

        AddChild(_sound);
        _sound.Build();
        AddChild(_hud);
        _hud.Build(()=>GetTree().ReloadCurrentScene());
        _hud.Caption("Медсестра   /   «Когда пойдёте завтракать, прикройте за собой. Тут сквозит».",8);
        _nextPulse=60;
        Mark("morning");
    }

    private void PortalWall(Vector3 center,Vector3 size)
    {
        var body = new StaticBody3D { Position=center };
        AddChild(body);
        EpisodeDoor.Box(body,Vector3.Zero,size,"454840");
        body.AddChild(new CollisionShape3D { Shape=new BoxShape3D { Size=size } });
    }

    private void Mark(string text) => GD.Print($"episode {_time:0.00} {text}");
    private void Phase(Beat value)
    {
        _beat=value;
        _phaseStart=_time;
        Mark(value.ToString().ToLowerInvariant());
    }

    private void World(int value)
    {
        _world=value;
        _ward.State(value);
        _sound.State(value);
        _notes.State(value);
        Mark($"world {value} carried={_carried} installed={_installed}");
    }

    public override void _PhysicsProcess(double delta)
    {
        if (_hud.Paused) { _patient.Mobile=false; return; }
        var dt=(float)delta;
        _time+=delta;
        _patient.Sensitivity=_hud.Sensitivity;
        _patient.Shake=_hud.Shake;
        _patient.Mobile=_stood && _beat!=Beat.Ending;
        _home.Tick(dt);
        _neighbor.Tick(dt);
        _loop.Tick(dt);
        if (_beat==Beat.Ending) { TickEnding(); return; }
        TickNurse(dt);
        TickRhythm();
        TickStory(dt);
        PickFocus();
        string note = _focus == Focus.None ? _notes.Prompt(_patient,_world) : "";
        if (Input.IsActionJustPressed("use"))
        {
            if (note.Length>0) _hud.Caption(_notes.Inspect(_patient,_world),7);
            else Interact(_focus);
        }
        TickGrip(dt);
        _hud.Prompt(note.Length>0 ? note : Prompt());
    }

    private void TickNurse(float dt)
    {
        if (!_nurse.Visible || _time<5.4) return;
        Vector3 target=_nurse.Position.X>1.15f ? new Vector3(1.05f,0,4.46f) : new Vector3(1.05f,0,13.4f);
        var travel=target-_nurse.Position;
        if(travel.LengthSquared()>.005f)
        {
            _nurse.Rotation = new Vector3(0,Mathf.Atan2(-travel.X,-travel.Z),0);
            _nurse.Position=_nurse.Position.MoveToward(target,dt*.88f);
        }
        if (_nurse.Position.Z>12.2f) _nurse.Visible=false;
    }

    private Vector3 Pos => _patient.GlobalPosition;
    private bool InHome => Pos.X>2.65f && Pos.Z>1.15f;
    private bool InHall => Pos.X<1.8f;
    private float FlatDistance(Vector3 point)
    {
        var v=point-Pos;
        return new Vector2(v.X,v.Z).Length();
    }

    private float Looking(Vector3 point)
    {
        var target=point-Pos;
        var flat=new Vector2(target.X,target.Z);
        var dir=new Vector2(_patient.Gaze.X,_patient.Gaze.Z);
        return dir.Normalized().Dot(flat.Normalized());
    }

    // A shared wall transmits knocks but never an interaction with an object on its other side.
    private void Candidate(Focus item, Vector3 at, float reach, bool available=true)
    {
        if(!available) return;
        float d=FlatDistance(at);
        if (d>reach) return;
        var dot=Looking(at);
        if(dot<-.28f && d>.80f) return;
        float score=d+(.95f-dot)*.58f;
        if(score<_best) { _best=score; _focus=item; }
    }

    private void PickFocus()
    {
        _focus=Focus.None;
        _best=float.MaxValue;
        if(!_stood) { _focus=Focus.Rise; return; }
        bool homeSide=Pos.X>2.15f && Pos.Z>1;
        bool neighborSide=Pos.X>2.15f && Pos.Z<1;
        Candidate(Focus.Pipe,Pipe,2.0f,homeSide);
        Candidate(Focus.Window,Sash,1.95f,homeSide);
        Candidate(Focus.Bed,new Vector3(7.35f,1,8.65f),1.9f,homeSide);
        Candidate(Focus.Mug,new Vector3(8.52f,1,7.78f),1.4f,homeSide);
        Candidate(Focus.Chair,new Vector3(5.20f,.65f,8.85f),1.4f,homeSide);
        Candidate(Focus.Home,HomeDoor,2.05f,Pos.Z>1f);
        Candidate(Focus.Neighbor,NextDoor,2.15f,Pos.Z<.8f);
        Candidate(Focus.Loop,LoopDoor,2.1f,InHall);
        Candidate(Focus.Desk,Post,2.1f,InHall);
        Candidate(Focus.OtherPipe,OppositePipe,2.05f,neighborSide);
        Candidate(Focus.OtherBed,new Vector3(7.35f,1,-1.8f),2.6f,neighborSide);
    }

    private string Prompt() => _focus switch {
        Focus.Rise => "E   Встать с койки",
        Focus.Pipe or Focus.OtherPipe => "E   Постучать",
        Focus.Window => _windowShut ? "E   Коснуться стекла" : "E   Прикрыть форточку",
        Focus.Home => _home.Target>.5f ? "E   Прикрыть дверь" : "E   Открыть дверь",
        Focus.Neighbor when _beat==Beat.Vigil => "Удерживать E   Придержать дверь",
        Focus.Neighbor when HasHandle => "E   Надеть ручку на стержень",
        Focus.Neighbor when _installed => "E   Потянуть дверь",
        Focus.Neighbor => "E   Осмотреть замок",
        Focus.Loop => "E   Нажать на ручку",
        Focus.Desk => "E   Прочесть сменный журнал",
        Focus.Bed or Focus.OtherBed => "E   Осмотреть койку",
        Focus.Mug => "E   Взять кружку в руки",
        Focus.Chair => "E   Коснуться сиденья",
        _ => ""
    };

    private void Interact(Focus item)
    {
        switch(item)
        {
            case Focus.Rise:
                _stood=true;
                _patient.GlobalPosition=new Vector3(6.42f,.035f,8.12f);
                _patient.Awake=true;
                _patient.Mobile=true;
                Mark("stood");
                break;
            case Focus.Pipe:
            case Focus.OtherPipe:
                Tap(item==Focus.OtherPipe?OppositePipe:Pipe);
                break;
            case Focus.Window:
                if(!_windowShut)
                {
                    _windowShut=true;
                    _sashTime=_time;
                    _ward.WindowClosed();
                    _sound.Cue("latch",Sash);
                    _hud.Caption("Шпингалет вошёл в паз. В палате стих сквозняк.");
                    Mark("window shut");
                }
                else _hud.Caption(_world==0?"На стекле медленно исчезает след ладони.":"Между рамами лежит снег. Снаружи деревья ещё в листьях.");
                break;
            case Focus.Home:
                _home.Target=_home.Target>.5f?.15f:1;
                _sound.Cue("hinge",HomeDoor);
                if(_home.Target<.5f) { _draft=false; _nextReminder=_time+4; }
                break;
            case Focus.Neighbor:
                if(_beat==Beat.Vigil) break;
                if(_carried)
                {
                    _carried=false; _installed=true; _neighbor.Lever.Visible=true; _neighbor.Target=1;
                    _pocketHandle.Visible=false;
                    Phase(Beat.Neighbor);
                    _sound.Cue("latch",NextDoor);
                    _hud.Caption("Квадрат вошёл до упора. Дверь подалась внутрь.");
                }
                else if(_installed)
                {
                    _neighbor.Target=_neighbor.Target>.55f?.28f:1;
                    _sound.Cue("hinge",NextDoor);
                }
                else _hud.Caption("Вместо ручки — короткий квадратный стержень. На металле видна свежая царапина.");
                break;
            case Focus.Loop: OpenLoop(); break;
            case Focus.Desk:
                _visitedPost=true;
                _hud.Caption(_world==0?"Сменный журнал   /   «В шестой опять сквозит. Заявку на оконную раму оставила у телефона».":_world==1?"Сменный журнал   /   Страницы слиплись. Верхний лист светлее там, где на нём годами лежала трубка.":"Сменный журнал   /   На бланке завтрашнее число. Рядом сохнет круглый след от кружки.",6);
                if(!_heard && !_firstCalmKnock) _nextPulse=Math.Min(_nextPulse,_time+5);
                break;
            case Focus.Bed:
                _hud.Caption(_world==0?"Светлая простыня прохладна. У края всё ещё видно, где я сидел.":"Железо проступило сквозь эмаль. Вмятина на краю матраса осталась моей.");
                break;
            case Focus.Mug:
                _hud.Caption(_world==0?"Чай ещё тёплый. На ободке скол, точно под большим пальцем.":"Пыль сорвалась с ободка. Тот же скол под большим пальцем.");
                break;
            case Focus.Chair:
                _hud.Caption(_world==0?"Под пальцами прогибается потёртый дерматин. Кто-то часто сидит у этой койки.":"От сиденья осталась рама. В ней зажат клочок потёртого дерматина.");
                break;
            case Focus.OtherBed:
                _hud.Caption("Подушка лежит на середине пустого матраса. В стене напротив — три свежие белые выбоины.");
                break;
        }
    }

    private void Tap(Vector3 point)
    {
        if(_taps.Count>=12) return;
        _echoStart=-1;
        _taps.Add(_time);
        _sound.Cue("knock",point);
        _ward.Rattle();
        _initialPulses=0;
        _firstCalmKnock=true;
        _nextPulse=_time+35;
        Mark($"tap {_taps.Count}");
    }

    private void PlayPattern(double delay)
    {
        if(_pattern.Count==0) return;
        _echoIndex=0;
        _echoStart=_time+delay;
    }

    private void TickRhythm()
    {
        // Keep the first stroke while the player stays at the pipe, even through a long pause.
        // After at least two strokes a quiet interval ends the phrase.
        if (_taps.Count==1 && FlatDistance(Pipe)>3.2f && FlatDistance(OppositePipe)>3.2f)
            _taps.Clear();
        if(_taps.Count>=2 && _time-_taps[^1]>1.42)
        {
                _pattern.Clear();
                foreach(var t in _taps) _pattern.Add(t-_taps[0]);
                _heard=true;
                PlayPattern(.42);
                Mark($"remembered {string.Join(",",_pattern.ConvertAll(t=>t.ToString("0.00")))}");
            _taps.Clear();
        }
        if(_echoStart>=0)
        {
            while(_echoIndex<_pattern.Count && _time>=_echoStart+_pattern[_echoIndex])
            {
                Vector3 point=_beat==Beat.Vigil?NextDoor:OppositePipe;
                _sound.Cue("knock",point);
                _ward.Rattle();
                if(_beat==Beat.Vigil) _neighbor.Lever.Rotation=new Vector3(0,0,.22f);
                Mark($"echo {_echoIndex}");
                _echoIndex++;
            }
            if(_echoIndex==_pattern.Count)
            {
                _echoStart=-1;
                if(_beat==Beat.Morning)
                    _hud.Caption(_windowShut
                        ? "Из-за стены вернулся мой ритм. Кронштейн вздрогнул на последнем ударе."
                        : "Из-за стены вернулся мой ритм. Рядом с кроватью дрожит открытая форточка.",5);
            }
        }
        _neighbor.Lever.Rotation=_neighbor.Lever.Rotation.Lerp(Vector3.Zero,.08f);
        if(!_stood || _heard || _taps.Count>0 || _time<_nextPulse) return;
        if(_initialPulses==0)
        {
            _initialPulses=3;
            _firstCalmKnock=true;
            _hud.Caption("Три удара. Со стороны батареи в шестой палате.",4);
        }
        _sound.Cue("knock",Pipe);
        _ward.Rattle();
        _initialPulses--;
        _nextPulse=_time+(_initialPulses==0?28:.52);
    }

    private void OpenLoop()
    {
        if(_beat==Beat.Morning)
        {
            _sound.Cue("latch",LoopDoor);
            _hud.Caption("За дверью тихо звякнула посуда. Ручка опускается, но язычок заедает.");
            return;
        }
        if(_beat!=Beat.Loop && _beat!=Beat.Abandoned)
        {
            _hud.Caption("Под пальцами только пустая накладка.");
            return;
        }
        if(_loopStart>=0 || _scareStart>=0) return;
        _passes++;
        double interval=_previousOpen<0?0:_time-_previousOpen;
        bool rushing=_passes>=3 && interval<_previousInterval*.87;
        _previousOpen=_time;
        _previousInterval=interval;
        _sound.Cue("hinge",LoopDoor);
        if(_passes==1) Phase(Beat.Loop);
        if(_passes<=2)
        {
            _loop.Target=1;
            _loopStart=_time;
            Mark($"loop opening {_passes}");
        }
        else
        {
            _scareStart=_time;
            _loop.Pulled=rushing;
            _loop.Target=rushing?.92f:.24f;
            _loop.OutsideHand.Visible=true;
            _hud.Caption(rushing?"Дверь дёрнулась раньше, чем я успел нажать.":"Ручка медленно поворачивается у меня под ладонью.");
            Mark(rushing?"fast grab":"slow grab");
        }
    }

    private void TickStory(float dt)
    {
        if(!_stood) return;
        if(!_visitedHall && InHall)
        {
            _visitedHall=true;
            _nextPulse=Math.Min(_nextPulse,_time+22);
        }
        if(!_visitedPost && InHall && Pos.Z>8)
        {
            _visitedPost=true;
            _nextPulse=Math.Min(_nextPulse,_time+10);
            _sound.Cue("voices",Post);
        }
        if(!_draft && _time>_nextReminder && InHall && FlatDistance(HomeDoor)>2)
        {
            _draft=true;
            _home.Target=1;
            _sound.Cue("hinge",HomeDoor);
        }
        if(_beat==Beat.Morning && _heard && _windowShut && _echoStart<0 && _time>_sashTime+.35)
        {
            // Swap the bed while it is outside the patient's view.
            if(Looking(new Vector3(7.35f,1,8.2f))<.25f || !InHome)
            {
                World(1);
                Phase(Beat.Abandoned);
                _nextReminder=_time+9;
                _sound.Cue("hinge",LoopDoor);
            }
        }
        if(_beat==Beat.Abandoned && _time>_nextReminder)
        {
            _nextReminder=_time+27;
            _sound.Cue("hinge",LoopDoor);
            _hud.Caption("Со стороны дальнего конца коридора тянет холодом. Дверь там ходит на петлях.",4);
        }
        if(_loopStart>=0 && _time-_loopStart>.58)
        {
            // The swinging leaf masks the seam; the same post now lies behind the patient.
            _loopStart=-1;
            _patient.GlobalPosition=new Vector3(Mathf.Clamp(Pos.X,-.72f,.72f),.04f,3.0f);
            _loop.Snap(0);
            if(_passes==1)
            {
                World(2);
                _hud.Caption("Мимо снова проплыла табличка шестой. Пост по-прежнему за спиной.");
                PlayPattern(2.5);
            }
            else
            {
                World(1);
                _hud.Caption("У дальнего стекла та же полоса света. С другой стороны ручки чиркнул металл.");
            }
            Mark($"loop seam {_passes}");
        }
        if(_scareStart>=0)
        {
            float pressure=Mathf.Clamp((float)(_time-_scareStart)*2.2f,0,1);
            _loop.Lever.Rotation=new Vector3(0,0,.46f*pressure);
        }
        if(_scareStart>=0 && _time-_scareStart>(_loop.Pulled?.23:1.2))
        {
            _scareStart=-1;
            _loop.Pulled=true;
            _loop.Target=0;
            _loop.OutsideHand.Visible=false;
            _loop.Lever.Visible=false;
            _carried=true;
            _pocketHandle.Visible=true;
            _sound.Cue("handle",LoopDoor);
            _patient.Jolt();
            _hud.Tear(.72f);
            _hud.Caption("Тяжёлая ручка осталась в ладони. Я убрал её в карман.",5);
            World(0);
            Phase(Beat.Handle);
            _nextReminder=_time+7;
        }
        if(_beat==Beat.Handle && _time>_nextReminder)
        {
            _pocketHandle.Visible=false;
            _nextReminder=_time+18;
            _sound.Cue("knock",NextDoor);
            _hud.Caption("У двери соседней палаты дребезжит голый стержень замка.");
            // Only the room behind the viewer changes. The handle continues to exist in the pocket.
            if(!InHome) _ward.RoomState(_turnAway?0:1);
            _turnAway=!_turnAway;
            Mark($"room shift carried={_carried}");
        }
        if(_beat==Beat.Neighbor && Pos.X>2.82f && Pos.Z<.8f)
        {
            Phase(Beat.Footsteps);
            _nextFootstep=_time+.8;
            _nextReminder=_time+4;
            _home.Target=1;
            _ward.RoomState(0);
            _sound.Cue("step",new Vector3(0,0,-11));
            _hud.Caption("В коридоре шагнули. В шестой, напротив, всё ещё горит свет.");
        }
        if(_beat==Beat.Footsteps || _beat==Beat.Return || _beat==Beat.Vigil)
        {
            if(_time>_nextFootstep)
            {
                var elapsed=(float)(_time-_phaseStart);
                var distance=Mathf.Max(1.1f,8-elapsed*.23f);
                var behind=Pos-_patient.Gaze*distance;
                _sound.Cue("step",new Vector3(Mathf.Clamp(behind.X,-1.4f,1.4f),.12f,Mathf.Clamp(behind.Z,-11,11)));
                _nextFootstep=_time+Math.Max(.57,1.38-elapsed*.026);
            }
            if(_beat==Beat.Footsteps)
            {
                if(InHome)
                {
                    Phase(Beat.Return);
                    _nextReminder=_time+7;
                    _sound.Cue("step",HomeDoor);
                }
                else if(_time>_nextReminder)
                {
                    _nextReminder=_time+22;
                    _sound.Cue("wind",HomeDoor);
                    _hud.Caption("Полоса света от открытой двери шестой ложится поперёк коридора.");
                }
            }
            if(_beat==Beat.Return)
            {
                if(Looking(HomeDoor)>.5f || !InHome)
                {
                    Phase(Beat.Vigil);
                    World(2);
                    _ward.RoomState(1);
                    _neighbor.Target=.53f;
                    _nextReminder=_time+6;
                    _hud.Tear(.45f);
                    _sound.Cue("voices",HomeDoor);
                    _hud.Caption("Коридор пуст. За моей спиной тихо говорят. Из соседней двери отзывается знакомый стук.",6);
                    PlayPattern(.7);
                }
                else if(_time>_nextReminder)
                {
                    _nextReminder=_time+10;
                    _sound.Cue("step",HomeDoor);
                    _hud.Caption("Последний шаг остановился у меня за спиной.");
                }
            }
            if(_beat==Beat.Vigil)
            {
                _ward.Finale((float)(_time-_phaseStart));
                if(_time>_nextReminder)
                {
                    _nextReminder=_time+7;
                    if(_echoStart<0 && _taps.Count==0) PlayPattern(.1);
                    _sound.Cue("voices",HomeDoor);
                }
                if(InHall && Pos.Z>9.55f) Finish(false);
            }
        }
    }

    private void TickGrip(float dt)
    {
        if(_beat!=Beat.Vigil) return;
        bool grip=_focus==Focus.Neighbor && Input.IsActionPressed("use");
        if(grip)
        {
            if(!_braced)
            {
                _braced=true;
                _patient.Hold(true);
                _sound.Cue("hinge",NextDoor);
                _hud.Caption("Пружина тянет створку обратно. Под дверью проезжает тень колеса.",3);
                _neighbor.Target=.72f;
            }
            _holdTime+=dt;
            if(_holdTime>1.7f && !_metHand)
            {
                _metHand=true;
                _neighbor.OutsideHand.Visible=true;
                _sound.Cue("handle",NextDoor);
            }
            if(_holdTime>=2.8f) Finish(true);
        }
        else if(_braced)
        {
            _braced=false;
            _metHand=false;
            _holdTime=0;
            _patient.Hold(false);
            _neighbor.OutsideHand.Visible=false;
            _neighbor.Target=.48f;
            _sound.Cue("hinge",NextDoor);
        }
    }

    private void Finish(bool stayed)
    {
        if(_beat==Beat.Ending) return;
        Phase(Beat.Ending);
        _endTime=_time;
        _patient.Mobile=false;
        _hud.Prompt("");
        if(stayed)
        {
            _neighbor.Target=.91f;
            _hud.Caption("Чья-то ладонь приняла вес двери. Теперь её держат с той стороны.",2.7f);
            _endingText="С той стороны";
        }
        else
        {
            _neighbor.Pulled=true;
            _neighbor.Target=0;
            _sound.Cue("latch",NextDoor);
            _hud.Caption("Позади щёлкнула защёлка.",2.2f);
            _endingText="Сквозняк";
        }
        Mark(stayed?"ending held":"ending left");
    }

    private void TickEnding()
    {
        if(!_quiet && _time-_endTime>1)
        {
            _quiet=true;
            _sound.Silence();
        }
        if(!_ended && _time-_endTime>4.8)
        {
            _ended=true;
            _hud.End(_endingText);
        }
    }
}
