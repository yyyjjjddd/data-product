package com.example.metrics.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步任务配置
 *
 * <p>配置用于执行异步任务的线程池：
 * <ul>
 *   <li>核心线程数：5</li>
 *   <li>最大线程数：10</li>
 *   <li>队列容量：100</li>
 *   <li>线程名前缀：task-consumer-</li>
 * </ul>
 *
 * <p>线程池用于消费RabbitMQ任务队列中的任务。
 */
@Configuration
public class AsyncConfig {

    /**
     * 创建任务执行器线程池
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("task-consumer-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
