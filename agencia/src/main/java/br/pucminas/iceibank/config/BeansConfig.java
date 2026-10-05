package br.pucminas.iceibank.config;
import br.pucminas.iceibank.service.*; import org.springframework.context.annotation.*; import java.io.IOException;
@Configuration public class BeansConfig {
 @Bean public RelogioVetorial relogioVetorial(AgenciaConfig c){return new RelogioVetorial(c.getIdAgencia(),AgenciaConfig.NUMERO_AGENCIAS);}
 @Bean public RegistroEventos registroEventos(AgenciaConfig c)throws IOException{return new RegistroEventos("agencia-"+c.getIdAgencia());}
}
