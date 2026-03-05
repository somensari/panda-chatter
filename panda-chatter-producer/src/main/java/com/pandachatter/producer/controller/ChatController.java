package com.pandachatter.producer.controller;

import com.pandachatter.common.model.ChatMessage;
import com.pandachatter.producer.service.MessageProducerService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Controller
public class ChatController {

    private final MessageProducerService producerService;
    // In-memory list of messages sent during this session
    private final List<ChatMessage> sentMessages = Collections.synchronizedList(new ArrayList<>());

    public ChatController(MessageProducerService producerService) {
        this.producerService = producerService;
    }

    @GetMapping("/")
    public String index(Model model) {
        List<ChatMessage> reversed = new ArrayList<>(sentMessages);
        Collections.reverse(reversed);
        model.addAttribute("sentMessages", reversed);
        return "chat";
    }

    @PostMapping("/send")
    public String send(
            @RequestParam String username,
            @RequestParam String content,
            RedirectAttributes redirectAttributes) {

        if (username.isBlank() || content.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Username and message cannot be empty.");
            return "redirect:/";
        }

        ChatMessage message = producerService.sendMessage(username, content);
        sentMessages.add(message);
        redirectAttributes.addFlashAttribute("successMsg", "Message sent!");
        return "redirect:/";
    }
}
