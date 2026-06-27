<!-- topic: Reference -->
<!-- title: Roadmap Phase 2 Production Infrastructure -->

## Phase 2: Production Infrastructure (2-3 months)

**Goal:** Enable production deployment and monitoring

### 2.1 Prometheus Metrics (1-2 weeks) 🔥 HIGH PRIORITY
**Why First:** Can't run production without visibility
**Location:** `lib/src/commonMain/kotlin/ai/solace/core/observability/`

**Tasks:**
- [ ] Prometheus metrics exposition endpoint
- [ ] Actor metrics (message rate, latency, errors)
- [ ] Storage metrics (operations, latency, size)
- [ ] Workflow metrics (execution time, success rate)
- [ ] JVM metrics (memory, GC, threads)
- [ ] Custom metric registration API

**Success Criteria:**
- `/metrics` endpoint exposes Prometheus format
- All key metrics are tracked
- Metrics have appropriate labels

### 2.2 Health Checks (1 week)
**Why Second:** Required for orchestration
**Location:** `lib/src/commonMain/kotlin/ai/solace/core/health/`

**Tasks:**
- [ ] Health check interface
- [ ] Actor system health check
- [ ] Storage health check
- [ ] Dependency health checks
- [ ] `/health` and `/ready` endpoints

**Success Criteria:**
- Liveness and readiness probes
- Health check aggregation
- Configurable health criteria

### 2.3 Kubernetes Deployment (1-2 weeks)
**Why Third:** Enable cloud deployment
**Location:** `k8s/` directory

**Tasks:**
- [ ] Deployment manifests
- [ ] Service definitions
- [ ] ConfigMaps for configuration
- [ ] Secrets for sensitive data
- [ ] Horizontal Pod Autoscaler
- [ ] Resource limits and requests
- [ ] Liveness and readiness probes
- [ ] PersistentVolumeClaims for storage

**Success Criteria:**
- One-command deployment to K8s
- Auto-scaling based on load
- Graceful shutdown and rolling updates

### 2.4 Production Docker Images (1 week)
**Why Fourth:** Optimize deployment
**Location:** `Dockerfile` and `.dockerignore`

**Tasks:**
- [ ] Multi-stage build optimization
- [ ] Minimal base image (alpine or distroless)
- [ ] Non-root user
- [ ] Health checks in Dockerfile
- [ ] Container registry CI/CD
- [ ] Version tagging strategy

**Success Criteria:**
- Image size < 200MB
- Build time < 5 minutes
- Security scan passes

### 2.5 Grafana Dashboards (1 week)
**Why Fifth:** Visualize metrics
**Location:** `grafana/dashboards/`

**Tasks:**
- [ ] System overview dashboard
- [ ] Actor performance dashboard
- [ ] Storage metrics dashboard
- [ ] Workflow execution dashboard
- [ ] Alert rules configuration

**Success Criteria:**
- Dashboards are importable
- Real-time updates
- Useful for operations team

### 2.6 Distributed Tracing (Optional, 1-2 weeks)
**Why Sixth:** Debug complex flows
**Location:** `lib/src/commonMain/kotlin/ai/solace/core/tracing/`

**Tasks:**
- [ ] OpenTelemetry integration
- [ ] Trace context propagation
- [ ] Span creation for actors and workflows
- [ ] Jaeger/Zipkin compatibility

**Success Criteria:**
- Request flow is traceable
- Integration with tracing backend
- Minimal performance impact

---


[Back to Roadmap](Roadmap)
