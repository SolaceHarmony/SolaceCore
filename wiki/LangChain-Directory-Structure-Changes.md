<!-- topic: Reference -->
<!-- title: LangChain Directory Structure Changes -->

## 6. Directory Structure Changes

Add new directories:
```
lib/
├── src/
│   ├── commonMain/
│   │   └── kotlin/
│   │       └── ai/
│   │           └── solace/
│   │               └── core/
│   │                   ├── actor/        (existing)
│   │                   ├── channels/     (existing)
│   │                   ├── common/       (existing)
│   │                   ├── chain/        (new)
│   │                   │   ├── base/
│   │                   │   ├── llm/
│   │                   │   ├── memory/
│   │                   │   └── tools/
│   │                   ├── metrics/      (new)
│   │                   └── tools/        (new)
```



[Back to LangChain Code Changes](LangChain-Code-Changes)
