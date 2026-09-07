package br.pucminas.iceibank.service;

/**
 * Adaptador de observabilidade. O algoritmo permanece integralmente em RelogioLamport.java.
 * Esta classe apenas guarda o último valor retornado para exibição no endpoint /status.
 */
public class RelogioLamportMonitorado extends RelogioLamport {
    private volatile int valorAtual;

    @Override
    public synchronized int eventoLocal() {
        valorAtual = super.eventoLocal();
        return valorAtual;
    }

    @Override
    public synchronized int aoEnviar() {
        valorAtual = super.aoEnviar();
        return valorAtual;
    }

    @Override
    public synchronized int aoReceber(int timestampRecebido) {
        valorAtual = super.aoReceber(timestampRecebido);
        return valorAtual;
    }

    public int valorAtual() {
        return valorAtual;
    }
}
