package edu.platform.education;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

    @Test
    void verifyModules() {
        ApplicationModules modules = ApplicationModules.of(EducationPlatformApplication.class);
        // Печатает дерево модулей в лог — удобно для отчёта
        modules.forEach(System.out::println);
        // Падает, если есть нарушения границ
        modules.verify();
    }
}