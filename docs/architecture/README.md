# GateWeave Architecture

GateWeave separates request execution from gateway management:

- The data plane accepts traffic, matches routes, executes filters and invokes resources.
- The control plane owns service registration, routes, policies and configuration releases.
- Integration contracts are versioned independently of both domain models.
- The Spring Boot starter is optional; standard protocols remain the primary integration path.

The data plane uses layer-first packages around one request-execution pipeline. The
control plane uses capability-first packages such as `catalog` and `route`, with
shallow `web`, `application`, `domain`, `persistence` and `configuration` packages inside
each capability. Controllers stay in `web`; request and response records are separated
into `web.request` and `web.response`. Maven modules define deployment and publication
boundaries, while packages define DDD boundaries.

Each executable module keeps its Spring Boot application class in the module root package.
Module-local tests stay in that module; `gateweave-tests` is reserved for cross-module
architecture, integration and end-to-end verification.
