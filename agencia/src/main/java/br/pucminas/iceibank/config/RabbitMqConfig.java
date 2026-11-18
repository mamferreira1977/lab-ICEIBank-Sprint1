package br.pucminas.iceibank.config;
import com.rabbitmq.client.ConnectionFactory; import org.springframework.amqp.core.*; import org.springframework.amqp.rabbit.connection.CachingConnectionFactory; import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter; import org.springframework.beans.factory.annotation.*; import org.springframework.context.annotation.*;
@Configuration public class RabbitMqConfig {public static final String EXCHANGE="iceibank.eventos"; @Bean public org.springframework.amqp.rabbit.connection.ConnectionFactory rabbitConnectionFactory(@Value("${rabbitmq.url}") String url)throws Exception{ConnectionFactory cf=new ConnectionFactory();cf.setUri(url);return new CachingConnectionFactory(cf);} @Bean Jackson2JsonMessageConverter jsonMessageConverter(){return new Jackson2JsonMessageConverter();} @Bean TopicExchange iceibankExchange(){return new TopicExchange(EXCHANGE,true,false);} @Bean("filaAgencia") Queue filaAgencia(AgenciaConfig c){return QueueBuilder.durable("fila-agencia-"+c.getIdAgencia()).build();} @Bean Binding bindingAgencia(@Qualifier("filaAgencia") Queue q,TopicExchange ex,AgenciaConfig c){return BindingBuilder.bind(q).to(ex).with("agencia."+c.getIdAgencia()+".creditar");} @Bean("filaAuditoria") Queue filaAuditoria(){return QueueBuilder.durable("fila-auditoria").build();} @Bean Binding bindingAuditoria(@Qualifier("filaAuditoria") Queue q,TopicExchange ex){return BindingBuilder.bind(q).to(ex).with("#");}@Bean("filaResultadoTransferencia")
Queue filaResultadoTransferencia(AgenciaConfig config) {
    return QueueBuilder.durable(
            "fila-resultado-transferencia-" + config.getIdAgencia()
    ).build();
}

    @Bean
    Binding bindingResultadoTransferencia(
            @Qualifier("filaResultadoTransferencia") Queue fila,
            TopicExchange exchange,
            AgenciaConfig config) {

        return BindingBuilder.bind(fila)
                .to(exchange)
                .with("agencia." + config.getIdAgencia()
                        + ".resultado-transferencia");
    }}
