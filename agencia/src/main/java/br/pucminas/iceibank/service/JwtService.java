package br.pucminas.iceibank.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey chave;
    private final long expiracaoSegundos;

    public JwtService(@Value("${jwt.secret:ICEIBank-Sprint1-chave-secreta-local-com-mais-de-32-bytes}") String segredo,
                      @Value("${jwt.expiration-seconds:600}") long expiracaoSegundos) {
        if (segredo == null || segredo.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT_SECRET deve possuir pelo menos 32 bytes para HS256.");
        }
        if (expiracaoSegundos <= 0) {
            throw new IllegalArgumentException("jwt.expiration-seconds deve ser maior que zero.");
        }
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
        this.expiracaoSegundos = expiracaoSegundos;
    }

    public String gerarToken(String usuario) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .issuer("ICEIBank")
                .subject(usuario)
                .claim("tipo", "access")
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plusSeconds(expiracaoSegundos)))
                .signWith(chave)
                .compact();
    }

    public Claims validarEObterClaims(String token) {
        Claims claims = Jwts.parser().verifyWith(chave).build()
                .parseSignedClaims(token).getPayload();
        if (!"ICEIBank".equals(claims.getIssuer())) {
            throw new io.jsonwebtoken.JwtException("issuer inválido");
        }
        return claims;
    }

    public long getExpiracaoSegundos() { return expiracaoSegundos; }
}
