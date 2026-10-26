# ADR 0001: Data Plane and Control Plane Boundaries

## Status

Accepted.

## Decision

GateWeave has two core bounded contexts. `gateweave-engine` owns request execution,
and `gateweave-center` owns registration and configuration management. Neither module
depends on the other's implementation. Communication uses immutable records from
`gateweave-contract`, translated by anti-corruption adapters at both boundaries.

Built-in protocol implementations live in `gateweave-plugin` and implement extension
interfaces owned by `gateweave-contract`. Service onboarding is optional and isolated in
`gateweave-spring-boot-starter`.

## Consequences

- Domain models remain local to their bounded context.
- New protocols are added as plugins without modifying routing domain logic.
- Non-Java services can integrate through standard control-plane APIs.
- Package dependency rules are enforced by ArchUnit.
