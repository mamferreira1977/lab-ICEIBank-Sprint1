package br.pucminas.iceibank.model;

import java.math.BigDecimal;

public class Conta {
    private final long id;
    private final String nomeUsuario;
    private BigDecimal saldo;

    public Conta(long id, String nomeUsuario, BigDecimal saldo) {
        this.id = id;
        this.nomeUsuario = nomeUsuario;
        this.saldo = saldo;
    }

    public long getId() {
        return id;
    }

    public String getNomeUsuario() {
        return nomeUsuario;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public synchronized void depositar(BigDecimal valor) {
        saldo = saldo.add(valor);
    }

    public synchronized void sacar(BigDecimal valor) {
        saldo = saldo.subtract(valor);
    }
}
