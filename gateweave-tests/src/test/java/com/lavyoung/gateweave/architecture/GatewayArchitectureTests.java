package com.lavyoung.gateweave.architecture;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.lavyoung.gateweave")
class GatewayArchitectureTests {

    @ArchTest
    static final ArchRule APPLICATION_MUST_NOT_DEPEND_ON_PLUGIN = noClasses()
            .that().resideInAPackage("..engine.application..")
            .should().dependOnClassesThat().resideInAPackage("..plugin..")
            .because("the application layer selects protocol contracts without knowing plugin implementations");

    @ArchTest
    static final ArchRule PLUGIN_MUST_NOT_DEPEND_ON_ENGINE = noClasses()
            .that().resideInAPackage("..plugin..")
            .should().dependOnClassesThat().resideInAPackage("..engine..")
            .because("protocol plugins depend on stable contracts instead of the engine implementation");

    @ArchTest
    static final ArchRule CORE_MUST_NOT_DEPEND_ON_HTTP_CLIENT = noClasses()
            .that().resideInAnyPackage("..contract..", "..engine.application..", "..engine.domain..")
            .should().dependOnClassesThat(JavaClass.Predicates.belongToAnyOf(
                    HttpClient.class, HttpRequest.class, HttpResponse.class))
            .because("HTTP client types belong to the protocol adapter and runtime assembly boundaries");

    @ArchTest
    static final ArchRule DOMAIN_MUST_NOT_DEPEND_ON_FRAMEWORKS = noClasses()
            .that().resideInAPackage("..engine.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "io.netty..", "org.apache.dubbo..")
            .because("the domain model must remain independent of frameworks and vendor SDKs");
}
