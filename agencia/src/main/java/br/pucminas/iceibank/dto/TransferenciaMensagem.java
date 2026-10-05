package br.pucminas.iceibank.dto; import java.math.BigDecimal; public record TransferenciaMensagem(long idConta, BigDecimal valor, int[] vetorEnvio, int origemAgencia, long idOrigem) {}
