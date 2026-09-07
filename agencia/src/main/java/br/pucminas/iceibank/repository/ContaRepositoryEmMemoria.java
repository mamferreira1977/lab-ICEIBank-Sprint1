package br.pucminas.iceibank.repository;

import br.pucminas.iceibank.model.Conta;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class ContaRepositoryEmMemoria implements ContaRepository {
    private final ConcurrentMap<Long, Conta> contas = new ConcurrentHashMap<>();

    @Override
    public boolean salvarSeAusente(Conta conta) {
        return contas.putIfAbsent(conta.getId(), conta) == null;
    }

    @Override
    public Optional<Conta> buscarPorId(long id) {
        return Optional.ofNullable(contas.get(id));
    }

    @Override
    public int quantidade() {
        return contas.size();
    }
}
