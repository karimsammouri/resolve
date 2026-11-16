package com.karimsammouri.emailservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String EMAIL_EXCHANGE = "resolve.email.exchange";
    public static final String EMAIL_QUEUE = "resolve.email.queue";
    public static final String EMAIL_ROUTING_KEY = "email.notification";

    public static final String EMAIL_DLX = "resolve.email.dlx";
    public static final String EMAIL_DLQ = "resolve.email.dlq";
    public static final String EMAIL_DLQ_ROUTING_KEY = "email.failed";

    @Bean
    public DirectExchange emailExchange() {
        return new DirectExchange(EMAIL_EXCHANGE, true, false);
    }

    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE)
                .deadLetterExchange(EMAIL_DLX)
                .deadLetterRoutingKey(EMAIL_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding emailBinding(@Qualifier("emailQueue") Queue queue,
                                @Qualifier("emailExchange") DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(EMAIL_ROUTING_KEY);
    }

    @Bean
    public DirectExchange emailDeadLetterExchange() {
        return new DirectExchange(EMAIL_DLX, true, false);
    }

    @Bean
    public Queue emailDeadLetterQueue() {
        return QueueBuilder.durable(EMAIL_DLQ).build();
    }

    @Bean
    public Binding emailDeadLetterBinding(@Qualifier("emailDeadLetterQueue") Queue queue,
                                          @Qualifier("emailDeadLetterExchange") DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(EMAIL_DLQ_ROUTING_KEY);
    }

    @Bean
    public ApplicationRunner initializeRabbitMQ(ConnectionFactory connectionFactory,
                                                @Qualifier("emailExchange") DirectExchange emailExchange,
                                                @Qualifier("emailQueue") Queue emailQueue,
                                                @Qualifier("emailBinding") Binding emailBinding,
                                                @Qualifier("emailDeadLetterExchange") DirectExchange emailDeadLetterExchange,
                                                @Qualifier("emailDeadLetterQueue") Queue emailDeadLetterQueue,
                                                @Qualifier("emailDeadLetterBinding") Binding emailDeadLetterBinding) {
        return args -> {
            RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);

            rabbitAdmin.declareExchange(emailDeadLetterExchange);
            rabbitAdmin.declareQueue(emailDeadLetterQueue);
            rabbitAdmin.declareBinding(emailDeadLetterBinding);

            rabbitAdmin.declareExchange(emailExchange);
            rabbitAdmin.declareQueue(emailQueue);
            rabbitAdmin.declareBinding(emailBinding);
        };
    }

    @Bean
    public JacksonJsonMessageConverter jacksonJsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
