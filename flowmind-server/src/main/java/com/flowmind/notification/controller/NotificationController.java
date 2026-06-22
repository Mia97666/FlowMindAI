package com.flowmind.notification.controller;

import com.flowmind.notification.entity.NotificationMessage;
import com.flowmind.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 通知中心接口。
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 查询某个用户的站内通知。
     */
    @GetMapping
    public List<NotificationMessage> list(@RequestParam String receiver) {
        return notificationService.list(receiver);
    }

    /**
     * 标记通知已读。
     */
    @PostMapping("/{id}/read")
    public NotificationMessage markRead(@PathVariable Long id) {
        return notificationService.markRead(id);
    }
}
