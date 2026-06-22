package com.flowmind.chat;

import com.flowmind.chat.dto.ChatRequest;
import com.flowmind.chat.dto.ChatResponse;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @PostMapping
    public ChatResponse chat(
            @Valid @RequestBody ChatRequest request
    ) {

        return new ChatResponse(
                "收到问题：" + request.getQuestion()
        );

    }

}