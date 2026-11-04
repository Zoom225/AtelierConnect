package com.atelierconnect.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SwaggerConfigTest {

    @Test
    void documentationUsesAtelierConnectBrandAndKeepsJwtSecurity() {
        OpenAPI documentation = new SwaggerConfig().openAPI();

        assertThat(documentation.getInfo().getTitle()).isEqualTo("AtelierConnect API");
        assertThat(documentation.getInfo().getDescription()).contains("ateliers collaboratifs");
        assertThat(documentation.getInfo().getContact().getName()).isEqualTo("AtelierConnect");
        assertThat(documentation.getInfo().getContact().getEmail()).isNull();
        assertThat(documentation.getSecurity()).anySatisfy(requirement ->
                assertThat(requirement).containsKey("Bearer Auth"));
        SecurityScheme bearer = documentation.getComponents().getSecuritySchemes().get("Bearer Auth");
        assertThat(bearer.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(bearer.getScheme()).isEqualTo("bearer");
        assertThat(bearer.getBearerFormat()).isEqualTo("JWT");
    }
}
