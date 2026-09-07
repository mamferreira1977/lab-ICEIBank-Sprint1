package br.pucminas.iceibank.dto;

import java.math.BigDecimal;

public record TransferenciaRequest(long idOrigem, long idDestino, BigDecimal valor) {}
