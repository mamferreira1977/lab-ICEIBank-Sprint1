package br.pucminas.iceibank.model;

import java.math.BigDecimal;

public record TransferenciaRegistro(
        String transferenciaId,
        long idOrigem,
        long idDestino,
        BigDecimal valor,
        String estado
) {}
