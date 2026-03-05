package com.pandachatter.consumer.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandachatter.common.model.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Controller
public class FeedController {

    private static final Logger log = LoggerFactory.getLogger(FeedController.class);

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper;

    public FeedController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @GetMapping("/")
    public String index() {
        return "feed";
    }

    /**
     * SSE endpoint — browsers connect here to receive live chat messages.
     */
    @GetMapping(value = "/feed/stream", produces = "text/event-stream")
    @ResponseBody
    public SseEmitter stream() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);
        log.info("New SSE client connected. Total: {}", emitters.size());

        emitter.onCompletion(() -> {
            emitters.remove(emitter);
            log.debug("SSE client disconnected. Total: {}", emitters.size());
        });
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            log.debug("SSE client timed out. Total: {}", emitters.size());
        });
        emitter.onError(ex -> {
            emitters.remove(emitter);
            log.debug("SSE client error: {}", ex.getMessage());
        });

        return emitter;
    }

    /**
     * Called by the Kafka listener to push a message to all connected browsers.
     */
    public void broadcast(ChatMessage message) {
        String json;
        try {
            json = objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize message: {}", e.getMessage());
            return;
        }

        List<SseEmitter> stale = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("message").data(json));
            } catch (IOException e) {
                stale.add(emitter);
            }
        }
        emitters.removeAll(stale);
    }
}
