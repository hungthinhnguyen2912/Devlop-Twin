package com.devtwin.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HealthControllerTests {

    private final HealthController controller = new HealthController();

    @Test
    void returnsOkStatus() {
        assertThat(controller.health().status()).isEqualTo("ok");
    }
}
