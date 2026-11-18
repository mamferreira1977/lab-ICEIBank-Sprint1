
package br.pucminas.iceibank.dto;

import java.math.BigDecimal;

public record SincronizarRequest(
        String tipo,
        BigDecimal valor,
        int[] vetorConhecido,
        String operacaoId,
        Long versaoConhecida
) {
}