package com.example.ddd.batchupdate.config;

import com.example.ddd.batchupdate.domain.event.TransactionUpdatedEvent;
import com.example.ddd.batchupdate.infrastructure.messaging.DomainEventHandler;
import com.example.ddd.batchupdate.infrastructure.messaging.DomainEventPublisher;
import com.example.ddd.batchupdate.infrastructure.messaging.InMemoryDomainEventInfrastructure;
import com.example.ddd.batchupdate.infrastructure.repository.InMemoryTransactionRepository;
import com.example.ddd.batchupdate.infrastructure.repository.TransactionRepository;
import com.example.ddd.batchupdate.domain.service.TransactionDomainService;
import com.example.ddd.batchupdate.domain.service.TransactionDomainServiceImpl;
import com.example.ddd.batchupdate.application.TransactionApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * DDD批量更新系统配置类
 * 配置各种Bean和依赖注入
 */
@Slf4j
@Configuration
public class DddBatchUpdateConfig {
    
    /**
     * 配置领域事件基础设施
     */
    @Bean
    @Primary
    public DomainEventPublisher domainEventPublisher() {
        log.info("配置内存领域事件基础设施");
        return new InMemoryDomainEventInfrastructure();
    }
    
    /**
     * 配置交易仓储
     */
    @Bean
    @Primary
    public TransactionRepository transactionRepository() {
        log.info("配置内存交易仓储");
        return new InMemoryTransactionRepository();
    }
    
    /**
     * 配置交易领域服务
     */
    @Bean
    @ConditionalOnMissingBean
    public TransactionDomainService transactionDomainService() {
        log.info("配置交易领域服务");
        return new TransactionDomainServiceImpl();
    }
    
    /**
     * 配置交易应用服务
     */
    @Bean
    @ConditionalOnMissingBean
    public TransactionApplicationService transactionApplicationService(
            TransactionDomainService domainService,
            DomainEventPublisher eventPublisher) {
        log.info("配置交易应用服务");
        return new TransactionApplicationService(domainService, eventPublisher);
    }
    
    /**
     * 配置交易更新事件处理器
     */
    @Bean
    public DomainEventHandler<TransactionUpdatedEvent> transactionUpdatedEventHandler() {
        return new DomainEventHandler<TransactionUpdatedEvent>() {
            @Override
            public void handle(TransactionUpdatedEvent event) {
                log.info("处理交易更新事件: {}", event.getDescription());
                log.debug("事件详情:\n{}", event.getDetails());
                
                // 这里可以添加实际的业务处理逻辑
                // 例如：发送通知、记录审计日志、触发其他业务流程等
                
                if (event.isSuccessfulUpdate()) {
                    log.info("交易 {} 更新成功", event.getTransactionId());
                } else if (event.isFailedUpdate()) {
                    log.warn("交易 {} 更新失败", event.getTransactionId());
                }
            }
            
            @Override
            public Class<TransactionUpdatedEvent> getEventType() {
                return TransactionUpdatedEvent.class;
            }
            
            @Override
            public String getHandlerName() {
                return "TransactionUpdatedEventHandler";
            }
        };
    }
    
    /**
     * 注册领域事件处理器
     */
    @Bean
    public DomainEventHandlerRegistration eventHandlerRegistration(
            DomainEventPublisher publisher,
            DomainEventHandler<TransactionUpdatedEvent> handler) {
        log.info("注册交易更新事件处理器");
        return new DomainEventHandlerRegistration(publisher, handler);
    }
    
    /**
     * 领域事件处理器注册器
     */
    public static class DomainEventHandlerRegistration {
        
        public DomainEventHandlerRegistration(DomainEventPublisher publisher,
                                            DomainEventHandler<?> handler) {
            if (publisher instanceof InMemoryDomainEventInfrastructure) {
                ((InMemoryDomainEventInfrastructure) publisher).registerHandler(handler);
                log.info("注册事件处理器: {}", handler.getHandlerName());
            } else {
                log.warn("不支持的事件发布器类型，无法注册处理器");
            }
        }
    }
}