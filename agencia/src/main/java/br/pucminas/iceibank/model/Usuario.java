package br.pucminas.iceibank.model;

public record Usuario(String nome, String usuario, String senhaHash, String criadoEm) {
}
