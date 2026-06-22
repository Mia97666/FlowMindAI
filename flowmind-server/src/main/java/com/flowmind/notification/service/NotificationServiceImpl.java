package com.flowmind.notification.service;

import com.flowmind.notification.entity.NotificationMessage;
import com.flowmind.notification.repository.NotificationMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知服务实现。
 */
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMessageRepository repository;

    @Override
    public NotificationMessage create(
            String receiver,
            String title,
            String content,
            String bizType,
            Long bizId
    ) {
        NotificationMessage message = new NotificationMessage();
        message.setReceiver(receiver);
        message.setTitle(title);
        message.setContent(content);
        message.setBizType(bizType);
        message.setBizId(bizId);
        message.setReadFlag(false);
        message.setCreatedAt(LocalDateTime.now());
        return repository.save(message);
    }

    @Override
    public List<NotificationMessage> list(String receiver) {
        return repository.findByReceiverOrderByCreatedAtDesc(receiver);
    }

    @Override
    public NotificationMessage markRead(Long id) {
        NotificationMessage message = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("通知不存在：" + id));
        message.setReadFlag(true);
        message.setReadAt(LocalDateTime.now());
        return repository.save(message);
    }
}
