package com.padell.padell.entity.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TypeParticipantConverterTest {
    private final TypeParticipantConverter converter = new TypeParticipantConverter();

    @Test
    void litEtConserveLesTroisValeursSqlHistoriques() {
        assertThat(converter.convertToEntityAttribute("GLOBAL")).isEqualTo(TypeParticipant.PARTENAIRE);
        assertThat(converter.convertToEntityAttribute("SITE")).isEqualTo(TypeParticipant.ABONNE);
        assertThat(converter.convertToEntityAttribute("LIBRE")).isEqualTo(TypeParticipant.VISITEUR);
        assertThat(converter.convertToDatabaseColumn(TypeParticipant.PARTENAIRE)).isEqualTo("GLOBAL");
        assertThat(converter.convertToDatabaseColumn(TypeParticipant.ABONNE)).isEqualTo("SITE");
        assertThat(converter.convertToDatabaseColumn(TypeParticipant.VISITEUR)).isEqualTo("LIBRE");
    }
}
