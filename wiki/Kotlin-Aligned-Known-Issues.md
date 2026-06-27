<!-- topic: Reference -->
<!-- title: Kotlin-Aligned Known Issues -->

## Known Issues (Priority Order)

### Critical (Week 1)

1. **Multi-lane initialization incomplete** - Emotional and technical streams not fully parallelized
2. **Supervisor bypass possible** - Some code paths don't enforce mandatory supervision
3. **UI-actor integration weak** - Compose UI doesn't fully reflect actor system state

### High Priority (Week 2-3)

4. **Test coverage low** - Only ~12% unit test coverage
5. **Memory consolidation partial** - Bidirectional linking not fully implemented
6. **Error handling inconsistent** - Some actors don't properly handle failures

### Medium Priority (Month 1)

7. **Performance optimization needed** - Some operations are not optimized for concurrency
8. **Configuration validation weak** - Limited validation of config files
9. **Documentation incomplete** - API documentation needs expansion



[Back to Kotlin-Aligned Quick Start](Kotlin-Aligned-Quick-Start)
