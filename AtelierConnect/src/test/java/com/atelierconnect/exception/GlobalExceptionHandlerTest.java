package com.atelierconnect.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.converter.HttpMessageNotReadableException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void requeteInvalideNeDivulguePasLeDetailTechnique() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        var response = handler.handleUnreadableRequest(
                new HttpMessageNotReadableException("valeur technique confidentielle"));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getMessage())
                .isEqualTo("La requête contient une valeur invalide ou un format incorrect.")
                .doesNotContain("confidentielle");
    }
}
