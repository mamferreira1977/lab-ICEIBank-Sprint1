package br.pucminas.iceibank.repository;

import br.pucminas.iceibank.model.Conta;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class ContaRepositoryEmMemoriaTest {
    @Test
    void salvaBuscaEImpedeIdDuplicado() {
        ContaRepository repo = new ContaRepositoryEmMemoria();
        assertTrue(repo.salvarSeAusente(new Conta(0, "Ana", new BigDecimal("100"))));
        assertFalse(repo.salvarSeAusente(new Conta(0, "Outra", BigDecimal.ZERO)));
        assertEquals("Ana", repo.buscarPorId(0).orElseThrow().getNomeUsuario());
        assertEquals(1, repo.quantidade());
    }
}
