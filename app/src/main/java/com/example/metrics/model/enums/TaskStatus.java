package com.example.metrics.model.enums;

import lombok.Getter;

/**
 * 任务状态枚举
 *
 * <p>定义任务的生命周期状态：
 * <ul>
 *   <li>pending: 等待执行，任务已创建但尚未被消费者处理</li>
 *   <li>running: 执行中，任务正在被处理</li>
 *   <li>success: 执行成功，任务完成并返回结果</li>
 *   <li>failed: 执行失败，任务未能成功完成</li>
 * </ul>
 *
 */
@Getter
public enum TaskStatus {
    PENDING("pending"),
    RUNNING("running"),
    SUCCESS("success"),
    FAILED("failed");

    private final String value;

    TaskStatus(String value) {
        this.value = value;
    }

    /**
     * 根据字符串值获取枚举实例
     *
     * @param value 状态字符串值
     * @return 对应的TaskStatus枚举
     * @throws IllegalArgumentException 无效的状态值
     */
    public static TaskStatus fromValue(String value) {
        for (TaskStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown task status: " + value);
    }
}
