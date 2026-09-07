package br.pucminas.iceibank.dto;

import java.math.BigDecimal;

public record CreditoRemotoRequest(BigDecimal valor, int timestampLamport, int origemAgencia) {}
