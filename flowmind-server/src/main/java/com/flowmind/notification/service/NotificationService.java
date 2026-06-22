package com.flowmind.notification.service;

import com.flowmind.notification.entity.NotificationMessage;

import java.util.List;

/**
 * 通知服务接口。
 */
public interface NotificationService {

    /**
     * 创建站内通知。
     */
    NotificationMessage create(
            String receiver,
            String title,
            String content,
            String bizType,
            Long bizId
    );

    /**
     * 查询某个用户的通知。
     */
    List<NotificationMessage> list(String receiver);

    /**
     * 标记已读。
     */
    NotificationMessage markRead(Long id);
}
