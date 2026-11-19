extends RefCounted
class_name DurkaRunState

# Only world appearance changes. Physical evidence and doors belong to one world.
enum Reality { LIVING, ABANDONED, DISTORTED }
enum Chapter { WAKE, ECHO, LOOP, HANDLE, RETURN, FINALE, FINISHED }

var reality: Reality = Reality.LIVING
var chapter: Chapter = Chapter.WAKE
var knock_times: Array[float] = []
var echo_pattern: Array[float] = []
var echo_established: bool = false
var window_closed: bool = false
var handle_owned: bool = false
var handle_installed: bool = false
var far_door_attempts: int = 0
var last_door_attempt: float = -999.0
var room_revisited: bool = false
var ending: String = ""
var exit_unlocked: bool = false

func record_knock(at_seconds: float) -> bool:
    # Time intervals, not specific beats: every rhythm of >=2 knocks is valid.
    if knock_times.is_empty() or at_seconds - knock_times[-1] > 2.5:
        knock_times.clear()
    knock_times.append(at_seconds)
    if knock_times.size() >= 2:
        echo_pattern.clear()
        for i in range(1, knock_times.size()):
            echo_pattern.append(maxf(0.08, knock_times[i] - knock_times[i - 1]))
        echo_established = true
        if chapter == Chapter.WAKE:
            chapter = Chapter.ECHO
        return true
    return false

func close_window() -> void:
    window_closed = true
    reality = Reality.ABANDONED
    if chapter < Chapter.LOOP:
        chapter = Chapter.LOOP

func open_far_door(at_seconds: float) -> bool:
    if handle_owned:
        return true
    far_door_attempts += 1
    var rapid: bool = at_seconds - last_door_attempt < 1.7
    last_door_attempt = at_seconds
    if far_door_attempts >= 3 or (far_door_attempts >= 2 and rapid):
        handle_owned = true
        reality = Reality.DISTORTED
        chapter = Chapter.HANDLE
        return true
    chapter = Chapter.LOOP
    return false

func install_handle() -> bool:
    if not handle_owned:
        return false
    handle_installed = true
    exit_unlocked = true
    chapter = Chapter.RETURN
    return true

func start_finale() -> void:
    if handle_installed:
        chapter = Chapter.FINALE

func finish(choice: String) -> bool:
    if chapter != Chapter.FINALE or (choice != "hold" and choice != "leave"):
        return false
    ending = choice
    chapter = Chapter.FINISHED
    return true

func enter_reality(new_reality: Reality) -> void:
    reality = new_reality
    # Never erase acquired handle or opened lock.
