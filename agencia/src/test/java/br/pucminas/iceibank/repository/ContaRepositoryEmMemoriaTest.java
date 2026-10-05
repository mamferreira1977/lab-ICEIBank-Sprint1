package br.pucminas.iceibank.repository;

import br.pucminas.iceibank.config.DataPathConfig;
import br.pucminas.iceibank.model.Conta;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ContaRepositoryEmMemoriaTest {

    @TempDir
    Path tempDir;

    @Test
    void salvaBuscaEImpedeIdDuplicado() {

        ObjectMapper objectMapper = new ObjectMapper();
        DataPathConfig dataPathConfig = new DataPathConfig(tempDir.toString());

        ContaRepository repo =
                new ContaRepositoryEmMemoria(objectMapper, dataPathConfig);

        assertTrue(repo.salvarSeAusente(
                new Conta(0, "Ana", new BigDecimal("100"))
        ));

        assertFalse(repo.salvarSeAusente(
                new Conta(0, "Outra", BigDecimal.ZERO)
        ));

        assertEquals(
                "Ana",
                repo.buscarPorId(0).orElseThrow().getNomeUsuario()
        );

        assertEquals(1, repo.quantidade());
    }
}