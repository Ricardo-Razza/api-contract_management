package com.contract_management.api.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigTest {

    @Test
    @DisplayName("Deve configurar origens permitidas e bloquear origens não listadas")
    void deveConfigurarOrigensPermitidas() {
        CorsConfig corsConfig = new CorsConfig();
        ReflectionTestUtils.setField(corsConfig, "allowedOrigins", List.of("http://localhost:4200", "https://meudominio.com"));

        WebMvcConfigurer configurer = corsConfig.corsConfigurer();
        TestCorsRegistry registry = new TestCorsRegistry();
        configurer.addCorsMappings(registry);

        Map<String, CorsConfiguration> configs = registry.getCorsConfigurations();
        assertThat(configs).containsKey("/**");

        CorsConfiguration config = configs.get("/**");
        assertThat(config.getAllowedOriginPatterns()).containsExactly("http://localhost:4200", "https://meudominio.com");
        assertThat(config.checkOrigin("http://localhost:4200")).isEqualTo("http://localhost:4200");
        assertThat(config.checkOrigin("https://meudominio.com")).isEqualTo("https://meudominio.com");
        assertThat(config.checkOrigin("http://evil.com")).isNull();
    }

    private static class TestCorsRegistry extends CorsRegistry {
        @Override
        public Map<String, CorsConfiguration> getCorsConfigurations() {
            return super.getCorsConfigurations();
        }
    }
}
