package com.atelierconnect.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtConfigTest {

    @Test
    void refuseUneCleAbsenteOuTropCourte() {
        JwtConfig config = new JwtConfig();
        for (String secret : new String[] {"", "court"}) {
            ReflectionTestUtils.setField(config, "secret", secret);
            assertThatThrownBy(config::validateSecret)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("JWT_SECRET doit contenir au moins 32 octets.");
        }
    }

    @Test
    void accepteUneCleDeTrenteDeuxOctets() {
        JwtConfig config = new JwtConfig();
        ReflectionTestUtils.setField(config, "secret", "0123456789abcdef0123456789abcdef");
        assertThatCode(config::validateSecret).doesNotThrowAnyException();
    }
}
