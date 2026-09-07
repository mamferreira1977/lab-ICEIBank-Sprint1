package br.pucminas.iceibank.dto;

import java.math.BigDecimal;

public record CriarContaRequest(long id, String nomeUsuario, BigDecimal saldoInicial) {}
