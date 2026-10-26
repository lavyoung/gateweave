# GateWeave Architecture

GateWeave separates request execution from gateway management:

- The data plane accepts traffic, matches routes, executes filters and invokes resources.
- The control plane owns service registration, routes, policies and configuration releases.
- Integration contracts are versioned independently of both domain models.
- The Spring Boot starter is optional; standard protocols remain the primary integration path.

Each bounded context uses domain, application, adapter and bootstrap packages. Maven
modules define deployment and publication boundaries, while packages define DDD layers.
