package com.pandachatter.producer.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandachatter.common.model.ChatMessage;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, ChatMessage> producerFactory(ObjectMapper objectMapper) {
        JsonSerializer<ChatMessage> serializer = new JsonSerializer<>(objectMapper);
        serializer.setAddTypeInfo(false);
        return new DefaultKafkaProducerFactory<>(
                Map.of(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers),
                new StringSerializer(),
                serializer
        );
    }

    @Bean
    public KafkaTemplate<String, ChatMessage> kafkaTemplate(ProducerFactory<String, ChatMessage> pf) {
        return new KafkaTemplate<>(pf);
    }

    @Bean
    public NewTopic chatMessagesTopic() {
        return TopicBuilder.name("chat-messages")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
