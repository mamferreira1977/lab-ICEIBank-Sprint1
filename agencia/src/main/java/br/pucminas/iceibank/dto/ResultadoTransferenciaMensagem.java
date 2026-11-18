package br.pucminas.iceibank.dto;

public record ResultadoTransferenciaMensagem(
        String transferenciaId,
        long idOrigem,
        long idDestino,
        int origemAgencia,
        boolean sucesso,
        String motivo
) {}
