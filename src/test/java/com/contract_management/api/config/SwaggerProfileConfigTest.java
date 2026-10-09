package com.contract_management.api.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class SwaggerProfileConfigTest {

    @Test
    @DisplayName("Deve conter Swagger desabilitado no application-prod.yaml")
    void deveDesabilitarSwaggerEmProd() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application-prod.yaml"));
        Properties props = yaml.getObject();
        assertThat(props).isNotNull();
        assertThat(props.getProperty("springdoc.api-docs.enabled")).isEqualTo("false");
        assertThat(props.getProperty("springdoc.swagger-ui.enabled")).isEqualTo("false");
    }

    @Test
    @DisplayName("Deve conter Swagger habilitado no application.yaml")
    void deveHabilitarSwaggerEmBase() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yaml"));
        Properties props = yaml.getObject();
        assertThat(props).isNotNull();
        assertThat(props.getProperty("springdoc.api-docs.enabled")).isEqualTo("true");
        assertThat(props.getProperty("springdoc.swagger-ui.enabled")).isEqualTo("true");
    }
}
