package com.duoc.gestionguias.config;

import java.util.Map;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${app.rabbitmq.queue.pendientes}")
    private String pendientesQueueName;

    @Value("${app.rabbitmq.queue.errores}")
    private String erroresQueueName;

    @Value("${app.rabbitmq.routing.pendientes}")
    private String pendientesRoutingKey;

    @Value("${app.rabbitmq.routing.errores}")
    private String erroresRoutingKey;

    /*
     * Exchange principal del sistema de guias.
     * Permite enrutar mensajes hacia la cola principal o hacia la cola de errores.
     */
    @Bean
    public DirectExchange guiasExchange() {
        return new DirectExchange(exchangeName, true, false);
    }

    /*
     * Cola principal.
     * Aqui se almacenan las guias pendientes de procesamiento.
     *
     * Si un mensaje expira o es rechazado, se redirige hacia el exchange principal
     * usando la routing key de errores.
     */
    @Bean
    public Queue guiasPendientesQueue() {
        return new Queue(
                pendientesQueueName,
                true,
                false,
                false,
                Map.of(
                        "x-dead-letter-exchange", exchangeName,
                        "x-dead-letter-routing-key", erroresRoutingKey
                )
        );
    }

    /*
     * Cola de errores.
     * Aqui se almacenan los mensajes que no pudieron ser procesados correctamente.
     */
    @Bean
    public Queue guiasErroresQueue() {
        return new Queue(erroresQueueName, true);
    }

    /*
     * Binding de la cola principal con el exchange.
     */
    @Bean
    public Binding pendientesBinding() {
        return BindingBuilder
                .bind(guiasPendientesQueue())
                .to(guiasExchange())
                .with(pendientesRoutingKey);
    }

    /*
     * Binding de la cola de errores con el exchange.
     */
    @Bean
    public Binding erroresBinding() {
        return BindingBuilder
                .bind(guiasErroresQueue())
                .to(guiasExchange())
                .with(erroresRoutingKey);
    }

    /*
     * Permite que Spring administre RabbitMQ.
     */
    @Bean
    public AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        rabbitAdmin.setAutoStartup(true);
        return rabbitAdmin;
    }

    /*
     * Convierte objetos Java a JSON para enviarlos como mensajes RabbitMQ.
     */
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /*
     * RabbitTemplate configurado para enviar objetos Java como JSON.
     * Este componente sera usado por los productores de mensajes.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter
    ) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter);
        return rabbitTemplate;
    }

    /*
     * Fuerza la declaracion de exchange, colas y bindings al iniciar la aplicacion.
     * Esto deja visible la configuracion en RabbitMQ Management para las evidencias.
     */
    @Bean
    public CommandLineRunner declararComponentesRabbitMQ(
            AmqpAdmin amqpAdmin,
            DirectExchange guiasExchange,
            Queue guiasPendientesQueue,
            Queue guiasErroresQueue,
            Binding pendientesBinding,
            Binding erroresBinding
    ) {
        return args -> {
            amqpAdmin.declareExchange(guiasExchange);
            amqpAdmin.declareQueue(guiasPendientesQueue);
            amqpAdmin.declareQueue(guiasErroresQueue);
            amqpAdmin.declareBinding(pendientesBinding);
            amqpAdmin.declareBinding(erroresBinding);
        };
    }
}