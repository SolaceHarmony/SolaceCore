# Task 3: Fix Concurrency Issues [MOSTLY COMPLETED]

Remove `runBlocking` usages in library code and make `ActorBuilder` fully asynchronous. Review other instances and add deadlock detection mechanisms.

Status: ✅ Major concurrency issues resolved:
- Deprecated blocking methods in ActorBuilder with clear migration paths
- Added fully asynchronous alternatives (`buildActorNetworkAsync()`, suspend methods)
- Reviewed and documented storage manager `runBlocking` usage
- Clear deprecation warnings guide users to async alternatives

Remaining: ❌ Add deadlock detection mechanisms
