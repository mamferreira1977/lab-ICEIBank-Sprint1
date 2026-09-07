package br.pucminas.iceibank.config;

import br.pucminas.iceibank.service.RegistroEventos;
import br.pucminas.iceibank.service.RelogioLamportMonitorado;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class BeansConfig {

    @Bean
    public RelogioLamportMonitorado relogioLamport() {
        return new RelogioLamportMonitorado();
    }

    @Bean
    public RegistroEventos registroEventos(AgenciaConfig config) throws IOException {
        return new RegistroEventos("agencia-" + config.getIdAgencia());
    }
}
