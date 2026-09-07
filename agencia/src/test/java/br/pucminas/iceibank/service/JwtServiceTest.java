package br.pucminas.iceibank.service;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    private static final String SEGREDO = "01234567890123456789012345678901";

    @Test
    void geraEValidaTokenAssinado() {
        JwtService jwt = new JwtService(SEGREDO, 60);
        String token = jwt.gerarToken("silvia");
        var claims = jwt.validarEObterClaims(token);
        assertEquals("silvia", claims.getSubject());
        assertEquals("ICEIBank", claims.getIssuer());
        assertEquals("access", claims.get("tipo"));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
    }

    @Test
    void rejeitaTokenExpirado() throws Exception {
        JwtService jwt = new JwtService(SEGREDO, 1);
        String token = jwt.gerarToken("silvia");
        Thread.sleep(1200);
        assertThrows(ExpiredJwtException.class, () -> jwt.validarEObterClaims(token));
    }

    @Test
    void rejeitaSegredoCurto() {
        assertThrows(IllegalArgumentException.class, () -> new JwtService("curta", 60));
    }
}
