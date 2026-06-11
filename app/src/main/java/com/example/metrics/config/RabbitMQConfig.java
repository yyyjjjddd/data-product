package com.example.metrics.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * RabbitMQ配置
 *
 * <p>配置任务队列相关的Exchange、Queue、Binding：
 * <ul>
 *   <li>task.exchange: Direct类型的交换机</li>
 *   <li>task.queue: 主任务队列，消息处理后进入死信队列</li>
 *   <li>task.retry.queue: 重试队列，消息TTL过期后转回主队列</li>
 *   <li>task.dlq: 死信队列，存储最终失败的消息</li>
 * </ul>
 *
 */
@Configuration
public class RabbitMQConfig {

    /** 交换机名称 */
    public static final String TASK_EXCHANGE = "task.exchange";
    /** 主任务队列 */
    public static final String TASK_QUEUE = "task.queue";
    /** 任务路由键 */
    public static final String TASK_ROUTING_KEY = "task.execute";
    /** 重试队列名称 */
    public static final String TASK_RETRY_QUEUE = "task.retry.queue";
    /** 重试路由键 */
    public static final String TASK_RETRY_ROUTING_KEY = "task.retry";
    /** 死信队列名称 */
    public static final String TASK_DLQ = "task.dlq";
    /** 死信路由键 */
    public static final String TASK_DLQ_ROUTING_KEY = "task.dead";

    /** 最大重试次数 */
    public static final int MAX_RETRY_COUNT = 3;
    /** 重试延迟时间（毫秒） */
    public static final int RETRY_DELAY_MS = 5000;

    /**
     * 创建Direct类型的交换机
     */
    @Bean
    public DirectExchange taskExchange() {
        return new DirectExchange(TASK_EXCHANGE);
    }

    /**
     * 创建主任务队列
     *
     * <p>配置死信队列，消息处理失败时进入死信队列。
     */
    @Bean
    public Queue taskQueue() {
        return QueueBuilder.durable(TASK_QUEUE)
                .withArgument("x-dead-letter-exchange", TASK_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", TASK_DLQ_ROUTING_KEY)
                .build();
    }

    /**
     * 绑定主任务队列到交换机
     */
    @Bean
    public Binding taskBinding() {
        return BindingBuilder.bind(taskQueue())
                .to(taskExchange())
                .with(TASK_ROUTING_KEY);
    }

    /**
     * 创建重试队列
     *
     * <p>配置TTL，消息在此队列中等待指定时间后自动转回主队列。
     */
    @Bean
    public Queue taskRetryQueue() {
        return QueueBuilder.durable(TASK_RETRY_QUEUE)
                .withArgument("x-dead-letter-exchange", TASK_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", TASK_ROUTING_KEY)
                .withArgument("x-message-ttl", RETRY_DELAY_MS)
                .build();
    }

    /**
     * 绑定重试队列到交换机
     */
    @Bean
    public Binding taskRetryBinding() {
        return BindingBuilder.bind(taskRetryQueue())
                .to(taskExchange())
                .with(TASK_RETRY_ROUTING_KEY);
    }

    /**
     * 创建死信队列
     *
     * <p>存储最终失败的消息，便于后续分析和处理。
     */
    @Bean
    public Queue taskDlq() {
        return QueueBuilder.durable(TASK_DLQ).build();
    }

    /**
     * 绑定死信队列到交换机
     */
    @Bean
    public Binding taskDlqBinding() {
        return BindingBuilder.bind(taskDlq())
                .to(taskExchange())
                .with(TASK_DLQ_ROUTING_KEY);
    }

    /**
     * JSON消息转换器
     *
     * <p>使用Jackson进行消息的序列化和反序列化。
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate配置
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
