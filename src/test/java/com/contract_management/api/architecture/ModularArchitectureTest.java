package com.contract_management.api.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModularArchitectureTest {
    private static final String ROOT = "com.contract_management.api";
    private static final Pattern MODULE = Pattern.compile(ROOT.replace(".", "\\.") + "\\.modules\\.([^.]+)\\.(.+)");
    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);

    // Relacionamentos JPA e contratos de dados existentes são preservados explicitamente.
    private static final Map<String, Set<String>> DEPENDENCIES = Map.of(
            "ativo", Set.of(),
            "secretaria", Set.of("ativo", "contrato"),
            "servidor", Set.of("ativo", "secretaria", "equipe"),
            "contrato", Set.of("ativo", "secretaria", "servidor", "equipe"),
            "equipe", Set.of("ativo", "contrato", "servidor"),
            "ferias", Set.of("ativo", "servidor", "secretaria", "contrato"),
            "impressora", Set.of("secretaria", "contrato")
    );

    @Test
    void modulosAcessamSomenteContratosPublicadosDosOutrosModulos() {
        for (var source : CLASSES) {
            var origin = MODULE.matcher(source.getPackageName());
            if (!origin.matches()) continue;
            var owner = origin.group(1);
            assertTrue(DEPENDENCIES.containsKey(owner), "Documente o novo módulo: " + owner);
            for (var dependency : source.getDirectDependenciesFromSelf()) {
                var target = MODULE.matcher(dependency.getTargetClass().getPackageName());
                if (!target.matches() || owner.equals(target.group(1))) continue;
                assertTrue(DEPENDENCIES.get(owner).contains(target.group(1)), dependency::getDescription);
                var layer = target.group(2);
                assertTrue(layer.equals("api") || layer.startsWith("api.")
                        || layer.equals("model") || layer.startsWith("model.")
                        || layer.equals("dto") || layer.startsWith("dto."),
                        () -> "Acesso à implementação de outro módulo: " + dependency.getDescription());
            }
        }
    }

    @Test
    void controllersNaoAcessamRepositorios() {
        noClasses().that().resideInAPackage("..controller..")
                .should().dependOnClassesThat().resideInAPackage("..repository..")
                .check(CLASSES);
    }

    @Test
    void modelosEDtosNaoDependemDeServicosOuControllers() {
        noClasses().that().resideInAnyPackage("..model..", "..dto..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..service..", "..controller..", "..repository..", "..scheduler..")
                .check(CLASSES);
    }

    @Test
    void contratosEntreModulosNaoDependemDoSpringDataOuDosServicos() {
        noClasses().that().resideInAPackage("..modules.*.api..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.springframework..", "..repository..", "..service..", "..controller..")
                .check(CLASSES);
    }

    @Test
    void componentesComunsNaoDependemDosModulosDeNegocio() {
        noClasses().that().resideInAPackage("..common..")
                .should().dependOnClassesThat().resideInAPackage("..modules..")
                .check(CLASSES);
    }
}
