package br.pucminas.iceibank.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

public class RegistroEventos {
    private final String nomeAgencia; private final Path caminhoArquivo; private final ObjectMapper mapper = new ObjectMapper();
    public RegistroEventos(String nomeAgencia) throws IOException {
        this.nomeAgencia=nomeAgencia; Path pasta=Paths.get("data"); Files.createDirectories(pasta);
        this.caminhoArquivo=pasta.resolve("eventos-"+nomeAgencia+".jsonl");
    }
    public synchronized Map<String,Object> registrar(String tipo,int[] timestampVetorial,Map<String,Object> detalhes) throws IOException {
        Map<String,Object> evento=new LinkedHashMap<>(); evento.put("agencia",nomeAgencia); evento.put("tipo",tipo);
        evento.put("timestampVetorial",timestampVetorial); evento.put("horaParede", Instant.now().toString()); evento.put("detalhes",detalhes);
        Files.writeString(caminhoArquivo, mapper.writeValueAsString(evento)+System.lineSeparator(), StandardOpenOption.CREATE,StandardOpenOption.APPEND);
        System.out.println("[Vetor "+Arrays.toString(timestampVetorial)+"] "+tipo+" "+detalhes); return evento;
    }
}
