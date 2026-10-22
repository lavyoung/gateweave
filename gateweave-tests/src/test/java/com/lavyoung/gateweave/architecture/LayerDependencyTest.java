package com.lavyoung.gateweave.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/** Guards the dependency direction of the DDD package structure. */
@AnalyzeClasses(
        packages = "com.lavyoung.gateweave",
        importOptions = ImportOption.DoNotIncludeTests.class)
class LayerDependencyTest {

    @ArchTest
    static final ArchRule DOMAIN_IS_FRAMEWORK_INDEPENDENT = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "io.netty..",
                    "jakarta.persistence..",
                    "org.apache.ibatis..");

    @ArchTest
    static final ArchRule APPLICATION_DOES_NOT_DEPEND_ON_OUTER_LAYERS = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..adapter..",
                    "..infrastructure..",
                    "..bootstrap..");

    @ArchTest
    static final ArchRule TOP_LEVEL_PACKAGES_ARE_FREE_OF_CYCLES = slices()
            .matching("com.lavyoung.gateweave.(*)..")
            .should().beFreeOfCycles();
}
