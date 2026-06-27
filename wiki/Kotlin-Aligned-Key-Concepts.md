<!-- topic: Reference -->
<!-- title: Kotlin-Aligned Key Concepts -->

## Key Concepts (TL;DR)

### Startup Sequence

```
Actor System Initialization
    ↓
Model Warmup (ping-pong)
    ↓
Negotiation (MCP → Functions → XML → Choice → CLI-only)
    ↓
Initialize Supervisor (mandatory)
    ↓
Ready (accept user input)
```

### Tool Execution

```
User → Main Actor → Advisor Actor → Supervisor Actor → MCP → Execution → Supervisor → Main Actor → User
```

**No shortcuts**: Supervisor is mandatory, cannot be bypassed.

### Multi-Actor System

- **Main Actor**: Handles user requests and coordinates
- **Advisor Actor**: Plans and deliberates using workflow engine
- **Supervisor Actor**: Approves/belays/revises all actions
- **Tool Actors**: Execute via MCP JSON-RPC
- **Memory Actor**: Manages neutral history and context
- **Mood Actor**: Provides emotional intelligence

Each actor gets filtered context (doesn't see others' thoughts).

### Neutral History

ALL conversation stored as XML:

- Timestamp-based ordering (ms since epoch)
- Actor attribution (provider/model:persona)
- MCP JSON-RPC for tools
- Complete provenance with Kotlin Flow streaming

Provider formats (OpenAI JSON) are TRANSPORT only.



[Back to Kotlin-Aligned Quick Start](Kotlin-Aligned-Quick-Start)
