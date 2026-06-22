package com.flowmind.notification.repository;

import com.flowmind.notification.entity.NotificationMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 通知数据访问接口。
 */
public interface NotificationMessageRepository extends JpaRepository<NotificationMessage, Long> {

    /**
     * 查询某个用户收到的通知。
     */
    List<NotificationMessage> findByReceiverOrderByCreatedAtDesc(String receiver);
}
