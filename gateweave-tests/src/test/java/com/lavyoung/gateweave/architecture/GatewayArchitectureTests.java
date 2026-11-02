package com.lavyoung.gateweave.architecture;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = "com.lavyoung.gateweave")
class GatewayArchitectureTests {

    @ArchTest
    static final ArchRule APPLICATION_MUST_NOT_DEPEND_ON_PLUGIN = noClasses()
            .that().resideInAPackage("..engine.application..")
            .should().dependOnClassesThat().resideInAPackage("..plugin..")
            .because("the application layer selects protocol contracts without knowing plugin implementations");

    @ArchTest
    static final ArchRule APPLICATION_MUST_NOT_DEPEND_ON_ADAPTERS = noClasses()
            .that().resideInAnyPackage("..engine.application..", "..center..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..adapter..", "..web..", "..persistence..",
                    "..engine.configuration..", "..center..configuration..")
            .because("application use cases must remain independent of inbound, outbound and assembly details");

    @ArchTest
    static final ArchRule PLUGIN_MUST_NOT_DEPEND_ON_ENGINE = noClasses()
            .that().resideInAPackage("..plugin..")
            .should().dependOnClassesThat().resideInAPackage("..engine..")
            .because("protocol plugins depend on stable contracts instead of the engine implementation");

    @ArchTest
    static final ArchRule ENGINE_MUST_NOT_DEPEND_ON_CENTER = noClasses()
            .that().resideInAPackage("..engine..")
            .should().dependOnClassesThat().resideInAPackage("..center..")
            .because("the data plane communicates with the control plane through process contracts");

    @ArchTest
    static final ArchRule CENTER_MUST_NOT_DEPEND_ON_ENGINE = noClasses()
            .that().resideInAPackage("..center..")
            .should().dependOnClassesThat().resideInAPackage("..engine..")
            .because("the control plane publishes contracts without depending on the data-plane implementation");

    @ArchTest
    static final ArchRule CORE_MUST_NOT_DEPEND_ON_HTTP_CLIENT = noClasses()
            .that().resideInAnyPackage("..contract..", "..engine.application..", "..engine.domain..")
            .should().dependOnClassesThat(JavaClass.Predicates.belongToAnyOf(
                    HttpClient.class, HttpRequest.class, HttpResponse.class))
            .because("HTTP client types belong to the protocol adapter and runtime assembly boundaries");

    @ArchTest
    static final ArchRule DOMAIN_MUST_NOT_DEPEND_ON_FRAMEWORKS = noClasses()
            .that().resideInAnyPackage("..engine.domain..", "..center..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "io.netty..", "org.apache.dubbo..", "org.apache.shiro..",
                    "io.jsonwebtoken..", "com.fasterxml.jackson..", "java.sql..")
            .because("the domain model must remain independent of frameworks and vendor SDKs");

    @ArchTest
    static final ArchRule DOMAIN_MUST_NOT_DEPEND_ON_OUTER_LAYERS = noClasses()
            .that().resideInAnyPackage("..engine.domain..", "..center..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..application..", "..adapter..", "..web..", "..persistence..", "..configuration..")
            .because("dependencies must point from adapters and application services toward the domain");

    @ArchTest
    static final ArchRule CONTRACT_MUST_NOT_DEPEND_ON_RUNTIME_MODULES = noClasses()
            .that().resideInAPackage("..contract..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..engine..", "..center..", "..plugin..", "..starter..", "org.springframework..")
            .because("contracts must remain stable and independent of every runtime implementation");

    @ArchTest
    static final ArchRule CENTER_CAPABILITIES_MUST_BE_FREE_OF_CYCLES = slices()
            .matching("com.lavyoung.gateweave.center.(*)..")
            .should().beFreeOfCycles()
            .because("control-plane capabilities must evolve without cyclic package dependencies");

    @ArchTest
    static final ArchRule ENGINE_LAYERS_MUST_BE_FREE_OF_CYCLES = slices()
            .matching("com.lavyoung.gateweave.engine.(*)..")
            .should().beFreeOfCycles()
            .because("data-plane package dependencies must preserve an acyclic execution pipeline");
}
