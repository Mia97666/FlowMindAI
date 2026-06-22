package com.flowmind.form.service;

import com.flowmind.form.dto.RuntimeFormResponse;

/**
 * 运行时表单解析服务。
 */
public interface RuntimeFormService {

    RuntimeFormResponse resolveStartForm(Long definitionId);

    RuntimeFormResponse resolveTaskForm(Long taskId);

    RuntimeFormResponse resolveInstanceForm(Long instanceId, String nodeId);
}
