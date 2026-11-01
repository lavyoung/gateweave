# GateWeave

GateWeave is an extensible API gateway organized around two bounded contexts:
the data plane (`gateweave-engine`) and the control plane (`gateweave-center`).

## Modules

| Module | Responsibility |
| --- | --- |
| `gateweave-contract` | Stable integration records, API results and extension SPI |
| `gateweave-plugin` | Built-in invokers, filters and codecs |
| `gateweave-engine` | Netty-based data-plane runtime |
| `gateweave-center` | Registration, configuration, releases and management API |
| `gateweave-spring-boot-starter` | Optional service metadata and registration integration |
| `gateweave-tests` | Cross-module architecture, integration and end-to-end tests |

The engine and center never depend on each other's implementation. They exchange
versioned records from `gateweave-contract` through explicit adapters.

## Package Direction

```text
engine: adapter -> application -> domain
center: web -> application -> domain
        persistence ---------> domain
        configuration -> application + persistence
```

Domain packages are framework independent. Cross-process records and plugin SPI
belong to `gateweave-contract`; vendor SDK types remain inside adapters or plugins.

`gateweave-engine` uses layer-first packages around the request pipeline.
`gateweave-center` uses capability-first packages such as `catalog`, `gateway` and `route`.
Each capability keeps controllers in `web`, request and response records in `web.request`
and `web.response`, use cases in `application`, ports and models in `domain`, and database
implementations in `persistence`.

## Build

```shell
mvn test
```

Run the control plane:

```shell
mvn -pl gateweave-center spring-boot:run
```

Run the data plane:

```shell
mvn -pl gateweave-engine spring-boot:run
```

Architecture decisions are documented under `docs/adr`.
