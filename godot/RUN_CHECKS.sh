#!/bin/sh
set -eu
GODOT=${GODOT:-godot}
"$GODOT" --headless --path "$(dirname "$0")" --editor --quit
"$GODOT" --headless --path "$(dirname "$0")" --script res://tests/check_state.gd
