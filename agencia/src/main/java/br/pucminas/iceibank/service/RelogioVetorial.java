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
    public static int comparar(int[] a, int[] b) {
        if (a == null || b == null || a.length != b.length) {
            throw new IllegalArgumentException("Vetores inválidos.");
        }

        boolean menor = false;
        boolean maior = false;

        for (int i = 0; i < a.length; i++) {
            if (a[i] < 0 || b[i] < 0) {
                throw new IllegalArgumentException("Vetor inválido.");
            }

            if (a[i] < b[i]) menor = true;
            if (a[i] > b[i]) maior = true;
        }

        if (menor && maior) return 2; // Concorrentes
        if (menor) return -1;         // a aconteceu antes de b
        if (maior) return 1;          // a aconteceu depois de b
        return 0;                     // Iguais
    }
}
