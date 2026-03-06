package com.pandachatter.producer.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaProducerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
public class KafkaConfig {

    /**
     * Inject the Spring-managed ObjectMapper (with JavaTimeModule) into the
     * auto-configured ProducerFactory so LocalDateTime serializes correctly.
     */
    @Bean
    public DefaultKafkaProducerFactoryCustomizer objectMapperProducerCustomizer(ObjectMapper objectMapper) {
        return factory -> {
            @SuppressWarnings("unchecked")
            DefaultKafkaProducerFactory<Object, Object> typedFactory =
                    (DefaultKafkaProducerFactory<Object, Object>) factory;
            typedFactory.setValueSerializer(new JsonSerializer<>(objectMapper));
        };
    }

    @Bean
    public NewTopic chatMessagesTopic() {
        return TopicBuilder.name("chat-messages")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
