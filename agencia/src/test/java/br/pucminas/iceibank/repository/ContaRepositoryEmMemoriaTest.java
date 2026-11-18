package br.pucminas.iceibank.repository;
import br.pucminas.iceibank.config.DataPathConfig;
import br.pucminas.iceibank.exception.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;

import br.pucminas.iceibank.model.Conta;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class ContaRepositoryEmMemoriaTest {
    @TempDir
    Path pastaTemporaria;
    @Test
    void salvaBuscaEImpedeIdDuplicado() {
        DataPathConfig dataPathConfig =
                new DataPathConfig(pastaTemporaria.toString(), 0);

        ContaRepository repo = new ContaRepositoryEmMemoria(
                new ObjectMapper(),
                dataPathConfig
        );
        assertTrue(repo.salvarSeAusente(new Conta(0, "Ana", new BigDecimal("100"))));
        assertFalse(repo.salvarSeAusente(new Conta(0, "Outra", BigDecimal.ZERO)));
        assertEquals("Ana", repo.buscarPorId(0).orElseThrow().getNomeUsuario());
        assertEquals(1, repo.quantidade());
    }
    @Test
    void versaoDaContaPermaneceAposReiniciarRepositorio() {

        DataPathConfig dataPathConfig =
                new DataPathConfig(pastaTemporaria.toString(), 0);

        ObjectMapper objectMapper = new ObjectMapper();

        ContaRepositoryEmMemoria repo =
                new ContaRepositoryEmMemoria(
                        objectMapper,
                        dataPathConfig
                );

        // Cria uma conta com saldo inicial de R$ 100
        repo.salvarSeAusente(
                new Conta(1002, "Usuario Teste", new BigDecimal("100"))
        );

        // A versão inicial deve ser zero
        assertEquals(0L, repo.consultarVersao(1002));

        // Atualiza o saldo para R$ 150
        repo.atualizar(
                new Conta(1002, "Usuario Teste", new BigDecimal("150"))
        );

        // A versão deve aumentar para 1
        assertEquals(1L, repo.consultarVersao(1002));

        // Simula o reinício: cria outro repositório usando o mesmo arquivo
        ContaRepositoryEmMemoria repoReiniciado =
                new ContaRepositoryEmMemoria(
                        objectMapper,
                        dataPathConfig
                );

        // Verifica se a versão e o saldo foram recuperados
        assertEquals(1L, repoReiniciado.consultarVersao(1002));

        assertEquals(
                0,
                new BigDecimal("150").compareTo(
                        repoReiniciado.buscarPorId(1002)
                                .orElseThrow()
                                .getSaldo()
                )
        );
    }
    @Test
    void rejeitaOperacaoOfflineComVersaoDesatualizada() {

        DataPathConfig config =
                new DataPathConfig(pastaTemporaria.toString(), 0);

        ContaRepositoryEmMemoria repo =
                new ContaRepositoryEmMemoria(
                        new ObjectMapper(),
                        config
                );

        repo.salvarSeAusente(
                new Conta(1002, "Usuario Teste", new BigDecimal("900"))
        );

        // Simula uma alteração realizada enquanto o aplicativo estava offline.
        repo.atualizar(
                new Conta(1002, "Usuario Teste", new BigDecimal("950"))
        );

        // A versão atual é 1, mas o aplicativo conhece a versão 0.
        assertEquals(1L, repo.consultarVersao(1002));

        ApiException erro = assertThrows(
                ApiException.class,
                () -> repo.aplicarOperacaoOffline(
                        1002,
                        "operacao-teste-001",
                        "DEPOSITO",
                        new BigDecimal("100"),
                        new int[]{0, 0, 0},
                        new int[]{1, 0, 0},
                        0L
                )
        );

        // Confirma que o saldo não foi alterado.
        assertEquals(
                0,
                new BigDecimal("950").compareTo(
                        repo.buscarPorId(1002)
                                .orElseThrow()
                                .getSaldo()
                )
        );

        // Confirma que a versão permaneceu 1.
        assertEquals(1L, repo.consultarVersao(1002));
    }
    @Test
    void naoDuplicaOperacaoOfflineAposReiniciar() {

        DataPathConfig config =
                new DataPathConfig(pastaTemporaria.toString(), 0);

        ObjectMapper mapper = new ObjectMapper();

        ContaRepositoryEmMemoria repo =
                new ContaRepositoryEmMemoria(mapper, config);

        repo.salvarSeAusente(
                new Conta(1002, "Usuario Teste", new BigDecimal("900"))
        );

        // Primeira sincronização: depósito de R$ 100.
        boolean aplicada = repo.aplicarOperacaoOffline(
                1002,
                "operacao-unica-001",
                "DEPOSITO",
                new BigDecimal("100"),
                new int[]{0, 0, 0},
                new int[]{1, 0, 0},
                0L
        );

        assertTrue(aplicada);
        assertEquals(1L, repo.consultarVersao(1002));

        // Simula o reinício do backend.
        ContaRepositoryEmMemoria reiniciado =
                new ContaRepositoryEmMemoria(mapper, config);

        // Tenta enviar exatamente a mesma operação novamente.
        boolean aplicadaNovamente = reiniciado.aplicarOperacaoOffline(
                1002,
                "operacao-unica-001",
                "DEPOSITO",
                new BigDecimal("100"),
                new int[]{0, 0, 0},
                new int[]{1, 0, 0},
                0L
        );

        assertFalse(aplicadaNovamente);

        // O saldo deve ser R$ 1.000, e não R$ 1.100.
        assertEquals(
                0,
                new BigDecimal("1000").compareTo(
                        reiniciado.buscarPorId(1002)
                                .orElseThrow()
                                .getSaldo()
                )
        );

        assertEquals(1L, reiniciado.consultarVersao(1002));
    }
}
