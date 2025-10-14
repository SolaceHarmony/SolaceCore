#!/bin/bash
# Simple template generator for new actors.
# Usage: ./generate_actor.sh MyActor

name=$1
if [ -z "$name" ]; then
  echo "Usage: $0 ActorName" >&2
  exit 1
fi
cat > "$name.kt" <<'TEMPLATE'
package ai.solace.custom

import ai.solace.core.actor.Actor

class $name : Actor() {
    override suspend fun process() {
        // TODO: implement actor logic
    }
}
TEMPLATE

echo "Created $name.kt"

