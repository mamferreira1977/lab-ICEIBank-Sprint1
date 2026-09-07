package br.pucminas.iceibank.dto;

public record LoginResponse(String token, String tipo, long expiraEmSegundos) {}
