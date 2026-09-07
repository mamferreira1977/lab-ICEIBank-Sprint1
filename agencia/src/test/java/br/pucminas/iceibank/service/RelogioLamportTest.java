package br.pucminas.iceibank.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RelogioLamportTest {
    @Test
    void aplicaAsTresRegrasDoRelogio() {
        RelogioLamport r = new RelogioLamport();
        assertEquals(1, r.eventoLocal());
        assertEquals(2, r.aoEnviar());
        assertEquals(6, r.aoReceber(5));
        assertEquals(7, r.aoReceber(2));
    }
}
