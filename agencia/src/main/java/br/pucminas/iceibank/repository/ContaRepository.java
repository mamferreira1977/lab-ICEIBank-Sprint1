
package br.pucminas.iceibank.repository;

import br.pucminas.iceibank.model.Conta;

import java.math.BigDecimal;
import java.util.Optional;

public interface ContaRepository {

    boolean salvarSeAusente(Conta conta);

    Optional<Conta> buscarPorId(long id);

    int quantidade();

    void atualizar(Conta conta);

    boolean creditarTransferencia(
            long idConta,
            BigDecimal valor,
            String transferenciaId
    );

    boolean estornarTransferencia(
            long idConta,
            BigDecimal valor,
            String transferenciaId
    );
}
