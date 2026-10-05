package br.pucminas.iceibank.service;

public class RelogioVetorial {
    private final int idAgencia;
    private final int[] vetor;

    public RelogioVetorial(int idAgencia, int numeroAgencias) {
        this.idAgencia = idAgencia;
        this.vetor = new int[numeroAgencias];
    }

    public synchronized int[] eventoLocal() { vetor[idAgencia]++; return vetor.clone(); }
    public synchronized int[] aoEnviar() { vetor[idAgencia]++; return vetor.clone(); }
    public synchronized int[] aoReceber(int[] recebido) {
        if (recebido == null || recebido.length != vetor.length) throw new IllegalArgumentException("Vetor recebido inválido.");
        for (int i=0;i<vetor.length;i++) vetor[i]=Math.max(vetor[i], recebido[i]);
        vetor[idAgencia]++; return vetor.clone();
    }
    public synchronized int[] valorAtual() { return vetor.clone(); }
}
