package com.ahmed.logistics.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Value("${app.async.core-pool-size:5}")
    private int corePoolSize;

    @Value("${app.async.max-pool-size:20}")
    private int maxPoolSize;

    @Value("${app.async.queue-capacity:100}")
    private int queueCapacity;

    @Value("${app.async.thread-name-prefix:logistics-async-}")
    private String threadNamePrefix;

    @Value("${app.async.await-termination-seconds:30}")
    private int awaitTerminationSeconds;

    @Override
    @Bean(name = {"taskExecutor", "asyncExecutor"})
    public ThreadPoolTaskExecutor getAsyncExecutor() {
        log.info("Initializing async ThreadPoolTaskExecutor: corePoolSize={}, maxPoolSize={}, queueCapacity={}, threadNamePrefix={}",
                corePoolSize, maxPoolSize, queueCapacity, threadNamePrefix);

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds);
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            log.error("Async execution error in method '{}.{}' with message: {}",
                    method.getDeclaringClass().getSimpleName(),
                    method.getName(),
                    throwable.getMessage(),
                    throwable);
        };
    }
}
