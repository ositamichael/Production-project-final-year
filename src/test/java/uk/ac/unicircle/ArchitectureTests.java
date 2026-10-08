package uk.ac.unicircle;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ArchitectureTests {

    @Test
    void modularBoundariesAreValid() {
        ApplicationModules.of(UniCircleApplication.class).verify();
    }
}
