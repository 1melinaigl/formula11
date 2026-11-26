package com.formula11.unit.arquitectura;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SliceAssignment;
import com.tngtech.archunit.library.dependencies.SliceIdentifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.repository.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Hace cumplir la arquitectura en capas descripta en el README.
 *
 * Las reglas se ejecutan desde métodos @Test de JUnit (y no con campos @ArchTest) porque
 * Surefire no ejecuta los tests del motor de ArchUnit en este proyecto: los reporta como 0 tests.
 */
class ArquitecturaTest {

    private static final Set<String> AREAS = Set.of(
            "usuarios", "jugadores", "metricas", "cotizaciones", "mercado", "portfolio", "senales");

    private static JavaClasses clases;

    @BeforeAll
    static void importarClasesProductivas() {
        clases = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.formula11");
    }

    // Dto puede acceder a Service porque los DTOs de respuesta se construyen a partir de los
    // resultados que devuelven los services (por ejemplo, RankingResultado). La dirección inversa
    // (Service -> Dto) sigue prohibida por dtoSoloEsAccedidoPorControllerYDto.
    @Test
    void capasRespetanDependencias() {
        verificar(layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .layer("Controller").definedBy("com.formula11.controller..")
                .layer("Service").definedBy("com.formula11.service..")
                .layer("Persistence").definedBy("com.formula11.persistence..")
                .layer("Model").definedBy("com.formula11.model..")
                .layer("Dto").definedBy("com.formula11.dto..")
                .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
                .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller", "Dto")
                .whereLayer("Persistence").mayOnlyBeAccessedByLayers("Service")
                .whereLayer("Model").mayOnlyBeAccessedByLayers("Controller", "Service", "Persistence", "Dto")
                .because("las dependencias entre capas deben respetar el flujo controller-service-persistence y aislar model"));
    }

    @Test
    void modelNoDependeDeCapasExternas() {
        verificar(noClasses()
                .that().resideInAPackage("com.formula11.model..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("com.formula11.controller..", "com.formula11.service..",
                        "com.formula11.persistence..")
                .because("el modelo no debe depender de controller, service ni persistence"));
    }

    @Test
    void modelNoDependeDeSpringWebNiDto() {
        verificar(noClasses()
                .that().resideInAPackage("com.formula11.model..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.springframework.web..", "com.formula11.dto..")
                .because("el modelo debe permanecer independiente de Spring Web y de los DTOs"));
    }

    @Test
    void dtoSoloEsAccedidoPorControllerYDto() {
        verificar(noClasses()
                .that().resideOutsideOfPackages("com.formula11.controller..", "com.formula11.dto..")
                .should().dependOnClassesThat().resideInAPackage("com.formula11.dto..")
                .because("los DTOs son parte del contrato HTTP y solo los usan controller y dto"));
    }

    @Test
    void restControllersTerminanEnController() {
        verificar(classes()
                .that().areAnnotatedWith(RestController.class)
                .should().haveSimpleNameEndingWith("Controller")
                .because("las clases anotadas con @RestController deben terminar en Controller"));
    }

    @Test
    void servicesTerminanEnService() {
        verificar(classes()
                .that().areAnnotatedWith(Service.class)
                .should().haveSimpleNameEndingWith("Service")
                .because("las clases anotadas con @Service deben terminar en Service"));
    }

    @Test
    void repositoriosTerminanEnRepository() {
        verificar(classes()
                .that().areAssignableTo(Repository.class)
                .should().haveSimpleNameEndingWith("Repository")
                .because("los repositorios de Spring Data deben terminar en Repository"));
    }

    @Test
    void excepcionesTerminanEnException() {
        verificar(classes()
                .that().areAssignableTo(Throwable.class)
                .should().haveSimpleNameEndingWith("Exception")
                .because("los tipos de excepción deben terminar en Exception"));
    }

    @ParameterizedTest(name = "sin ciclos entre las áreas de {0}")
    @ValueSource(strings = {"controller", "service", "persistence", "model", "dto"})
    void areasSinCiclos(String capa) {
        verificar(slices()
                .assignedFrom(areasDe(capa))
                .should().beFreeOfCycles()
                .because("las áreas de " + capa + " no deben depender cíclicamente entre sí"));
    }

    @Test
    void capasNoTienenClasesEnLaRaiz() {
        verificar(noClasses()
                .should().resideInAnyPackage(
                        "com.formula11.controller",
                        "com.formula11.service",
                        "com.formula11.persistence",
                        "com.formula11.dto")
                .because("las clases de cada capa deben estar dentro de un subpaquete de área"));
    }

    private static void verificar(ArchRule regla) {
        regla.check(clases);
    }

    private static SliceAssignment areasDe(String capa) {
        String paqueteCapa = "com.formula11." + capa + ".";

        return new SliceAssignment() {
            @Override
            public SliceIdentifier getIdentifierOf(JavaClass javaClass) {
                String paquete = javaClass.getPackageName();
                if (!paquete.startsWith(paqueteCapa)) {
                    return SliceIdentifier.ignore();
                }
                String area = paquete.substring(paqueteCapa.length()).split("\\.")[0];
                return AREAS.contains(area) ? SliceIdentifier.of(area) : SliceIdentifier.ignore();
            }

            @Override
            public String getDescription() {
                return "áreas funcionales de " + capa;
            }
        };
    }
}
