package com.nova.pollink.admin.controller;

import com.nova.pollink.admin.dal.entity.MessageEntity;
import com.nova.pollink.admin.dal.mapper.MessageMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/messages")
public class MessageController {

    private final MessageMapper messageMapper;
    private final RestTemplate restTemplate;
    private final String serverUrl;

    public MessageController(MessageMapper messageMapper,
                             @Value("${nova.pollink.admin.server-url:http://localhost:8080}") String serverUrl) {
        this.messageMapper = messageMapper;
        this.restTemplate = new RestTemplate();
        this.serverUrl = serverUrl.endsWith("/") ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;
    }

    @GetMapping
    public List<MessageEntity> listMessages(
            @RequestParam(required = false) String topic,
            @RequestParam(required = false, defaultValue = "20") int limit) {
        return messageMapper.selectRecent(limit);
    }

    @PostMapping("/send")
    public Map<String, String> sendTestMessage(@RequestBody Map<String, String> request) {
        restTemplate.postForObject(serverUrl + "/api/v1/push/message", request, Map.class);
        return Map.of("status", "ok");
    }
}
