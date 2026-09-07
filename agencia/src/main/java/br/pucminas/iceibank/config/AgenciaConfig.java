package br.pucminas.iceibank.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AgenciaConfig {
    public static final int NUMERO_AGENCIAS = 3;

    private final int idAgencia;
    private final int offset;

    public AgenciaConfig(@Value("${agencia.id}") int idAgencia,
                         @Value("${agencia.offset}") int offset) {
        this.idAgencia = idAgencia;
        this.offset = offset;
    }

    public int getIdAgencia() {
        return idAgencia;
    }

    public int getOffset() {
        return offset;
    }

    public int agenciaResponsavel(long idConta) {
        return Math.floorMod(idConta, NUMERO_AGENCIAS);
    }

    public String urlAgencia(int id) {
        if (id < 0 || id >= NUMERO_AGENCIAS) {
            throw new IllegalArgumentException("Agência inválida: " + id);
        }
        return "http://localhost:" + (4000 + offset + id);
    }
}
