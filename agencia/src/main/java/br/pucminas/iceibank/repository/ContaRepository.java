package br.pucminas.iceibank.repository;

import br.pucminas.iceibank.model.Conta;

import java.util.Optional;

public interface ContaRepository {

    boolean salvarSeAusente(Conta conta);

    Optional<Conta> buscarPorId(long id);

    int quantidade();

    void atualizar(Conta conta);
}