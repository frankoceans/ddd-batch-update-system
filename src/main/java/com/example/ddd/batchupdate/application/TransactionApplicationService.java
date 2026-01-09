package com.example.ddd.batchupdate.application;

import com.example.ddd.batchupdate.domain.model.entity.FinancialTransaction;
import com.example.ddd.batchupdate.domain.model.entity.TransactionRecord;
import com.example.ddd.batchupdate.domain.model.valueobject.StreamId;
import com.example.ddd.batchupdate.domain.model.valueobject.TransactionStatus;
import com.example.ddd.batchupdate.domain.service.TransactionDomainService;
import com.example.ddd.batchupdate.infrastructure.messaging.DomainEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 交易应用服务
 * 协调领域服务和基础设施，实现用例逻辑
 */
@Slf4j
@Service
public class TransactionApplicationService {
    
    private final TransactionDomainService domainService;
    private final DomainEventPublisher eventPublisher;
    
    public TransactionApplicationService(TransactionDomainService domainService,
                                      DomainEventPublisher eventPublisher) {
        this.domainService = domainService;
        this.eventPublisher = eventPublisher;
    }
    
    /**
     * 创建交易
     */
    @Transactional
    public CreateTransactionResult createTransaction(CreateTransactionCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("命令不能为空");
        }
        
        try {
            // 1. 创建交易聚合
            String transactionId = UUID.randomUUID().toString();
            StreamId streamId = StreamId.of(command.getStreamId());
            Set<TransactionRecord> records = createTransactionRecords(command);
            
            FinancialTransaction transaction = FinancialTransaction.create(
                transactionId, streamId, records, command.getOperator());
            
            // 2. 保存交易
            // 实际项目中应该通过仓储保存，这里假设已经保存
            log.info("创建交易成功: {}", transactionId);
            
            return CreateTransactionResult.success(transaction);
            
        } catch (Exception e) {
            log.error("创建交易失败", e);
            return CreateTransactionResult.failure(e.getMessage());
        }
    }
    
    /**
     * 批量更新交易状态
     */
    @Transactional
    public BatchUpdateStatusResult batchUpdateStatus(BatchUpdateStatusCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("命令不能为空");
        }
        
        try {
            // 1. 验证命令
            if (command.getTransactionIds() == null || command.getTransactionIds().isEmpty()) {
                throw new IllegalArgumentException("交易ID列表不能为空");
            }
            
            // 2. 批量更新
            TransactionDomainService.BatchUpdateResult result = domainService.batchUpdateTransactionStatus(
                command.getTransactionIds(), command.getNewStatus(), command.getOperator());
            
            // 3. 发布事件
            for (String transactionId : result.getUpdatedTransactionIds()) {
                // 这里应该获取实际交易对象来发布事件
                // 为了演示，我们假设发布成功状态的事件
                log.info("发布交易更新事件: {}", transactionId);
            }
            
            log.info("批量更新状态完成: {} 个成功, {} 个失败", 
                result.getUpdatedTransactionIds().size(), result.getFailedTransactionIds().size());
            
            return BatchUpdateStatusResult.from(result);
            
        } catch (Exception e) {
            log.error("批量更新状态失败", e);
            return BatchUpdateStatusResult.failure(e.getMessage());
        }
    }
    
    /**
     * 批量处理交易记录
     */
    @Transactional
    public BatchProcessRecordsResult batchProcessRecords(BatchProcessRecordsCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("命令不能为空");
        }
        
        try {
            // 1. 解析流ID
            StreamId streamId = StreamId.of(command.getStreamId());
            
            // 2. 批量处理
            TransactionDomainService.BatchProcessingResult result = domainService.processBatchTransactionRecords(
                streamId, command.getUpdates(), command.getOperator());
            
            log.info("批量处理记录完成: {} 个成功, {} 个失败", 
                result.getSuccessCount(), result.getFailedCount());
            
            return BatchProcessRecordsResult.from(result);
            
        } catch (Exception e) {
            log.error("批量处理记录失败", e);
            return BatchProcessRecordsResult.failure(e.getMessage());
        }
    }
    
    /**
     * 查询交易
     */
    @Transactional(readOnly = true)
    public QueryTransactionResult queryTransaction(QueryTransactionCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("查询命令不能为空");
        }
        
        try {
            // 实际项目中应该通过仓储查询
            // 这里返回模拟结果
            log.debug("查询交易: {}", command.getTransactionId());
            
            return QueryTransactionResult.success(null); // 实际应该返回查询结果
            
        } catch (Exception e) {
            log.error("查询交易失败", e);
            return QueryTransactionResult.failure(e.getMessage());
        }
    }
    
    /**
     * 创建交易记录
     */
    private Set<TransactionRecord> createTransactionRecords(CreateTransactionCommand command) {
        Set<TransactionRecord> records = new HashSet<>();
        StreamId streamId = StreamId.of(command.getStreamId());
        
        for (String recordData : command.getRecordData()) {
            String recordId = UUID.randomUUID().toString();
            TransactionRecord record = TransactionRecord.create(
                recordId, streamId, recordData, command.getOperator());
            records.add(record);
        }
        
        return records;
    }
    
    /**
     * 创建交易命令
     */
    public static class CreateTransactionCommand {
        private final String streamId;
        private final List<String> recordData;
        private final String operator;
        
        public CreateTransactionCommand(String streamId, List<String> recordData, String operator) {
            this.streamId = streamId;
            this.recordData = recordData != null ? recordData : List.of();
            this.operator = operator;
        }
        
        public String getStreamId() { return streamId; }
        public List<String> getRecordData() { return recordData; }
        public String getOperator() { return operator; }
    }
    
    /**
     * 创建交易结果
     */
    public static class CreateTransactionResult {
        private final boolean success;
        private final FinancialTransaction transaction;
        private final String errorMessage;
        
        private CreateTransactionResult(boolean success, FinancialTransaction transaction, String errorMessage) {
            this.success = success;
            this.transaction = transaction;
            this.errorMessage = errorMessage;
        }
        
        public static CreateTransactionResult success(FinancialTransaction transaction) {
            return new CreateTransactionResult(true, transaction, null);
        }
        
        public static CreateTransactionResult failure(String errorMessage) {
            return new CreateTransactionResult(false, null, errorMessage);
        }
        
        public boolean isSuccess() { return success; }
        public FinancialTransaction getTransaction() { return transaction; }
        public String getErrorMessage() { return errorMessage; }
    }
    
    /**
     * 批量更新状态命令
     */
    public static class BatchUpdateStatusCommand {
        private final Set<String> transactionIds;
        private final TransactionStatus newStatus;
        private final String operator;
        
        public BatchUpdateStatusCommand(Set<String> transactionIds, TransactionStatus newStatus, String operator) {
            this.transactionIds = transactionIds != null ? transactionIds : Set.of();
            this.newStatus = newStatus;
            this.operator = operator;
        }
        
        public Set<String> getTransactionIds() { return transactionIds; }
        public TransactionStatus getNewStatus() { return newStatus; }
        public String getOperator() { return operator; }
    }
    
    /**
     * 批量更新状态结果
     */
    public static class BatchUpdateStatusResult {
        private final boolean success;
        private final Set<String> updatedTransactionIds;
        private final Set<String> failedTransactionIds;
        private final List<String> errorMessages;
        
        private BatchUpdateStatusResult(boolean success, Set<String> updatedTransactionIds,
                                      Set<String> failedTransactionIds, List<String> errorMessages) {
            this.success = success;
            this.updatedTransactionIds = updatedTransactionIds;
            this.failedTransactionIds = failedTransactionIds;
            this.errorMessages = errorMessages != null ? errorMessages : List.of();
        }
        
        public static BatchUpdateStatusResult from(TransactionDomainService.BatchUpdateResult result) {
            return new BatchUpdateStatusResult(
                result.isSuccess(),
                result.getUpdatedTransactionIds(),
                result.getFailedTransactionIds(),
                result.getErrorMessages()
            );
        }
        
        public static BatchUpdateStatusResult failure(String errorMessage) {
            return new BatchUpdateStatusResult(false, Set.of(), Set.of(), List.of(errorMessage));
        }
        
        public boolean isSuccess() { return success; }
        public Set<String> getUpdatedTransactionIds() { return updatedTransactionIds; }
        public Set<String> getFailedTransactionIds() { return failedTransactionIds; }
        public List<String> getErrorMessages() { return errorMessages; }
    }
    
    /**
     * 批量处理记录命令
     */
    public static class BatchProcessRecordsCommand {
        private final String streamId;
        private final Map<String, TransactionRecord> updates;
        private final String operator;
        
        public BatchProcessRecordsCommand(String streamId, Map<String, TransactionRecord> updates, String operator) {
            this.streamId = streamId;
            this.updates = updates != null ? updates : Map.of();
            this.operator = operator;
        }
        
        public String getStreamId() { return streamId; }
        public Map<String, TransactionRecord> getUpdates() { return updates; }
        public String getOperator() { return operator; }
    }
    
    /**
     * 批量处理记录结果
     */
    public static class BatchProcessRecordsResult {
        private final boolean processed;
        private final int totalRecords;
        private final int successCount;
        private final int failedCount;
        private final Map<String, String> failureReasons;
        
        private BatchProcessRecordsResult(boolean processed, int totalRecords, int successCount,
                                        int failedCount, Map<String, String> failureReasons) {
            this.processed = processed;
            this.totalRecords = totalRecords;
            this.successCount = successCount;
            this.failedCount = failedCount;
            this.failureReasons = failureReasons != null ? failureReasons : Map.of();
        }
        
        public static BatchProcessRecordsResult from(TransactionDomainService.BatchProcessingResult result) {
            return new BatchProcessRecordsResult(
                result.isProcessed(),
                result.getTotalRecords(),
                result.getSuccessCount(),
                result.getFailedCount(),
                result.getFailureReasons()
            );
        }
        
        public static BatchProcessRecordsResult failure(String errorMessage) {
            return new BatchProcessRecordsResult(false, 0, 0, 0, Map.of("error", errorMessage));
        }
        
        public boolean isProcessed() { return processed; }
        public int getTotalRecords() { return totalRecords; }
        public int getSuccessCount() { return successCount; }
        public int getFailedCount() { return failedCount; }
        public Map<String, String> getFailureReasons() { return failureReasons; }
    }
    
    /**
     * 查询交易命令
     */
    public static class QueryTransactionCommand {
        private final String transactionId;
        
        public QueryTransactionCommand(String transactionId) {
            this.transactionId = transactionId;
        }
        
        public String getTransactionId() { return transactionId; }
    }
    
    /**
     * 查询交易结果
     */
    public static class QueryTransactionResult {
        private final boolean success;
        private final FinancialTransaction transaction;
        private final String errorMessage;
        
        private QueryTransactionResult(boolean success, FinancialTransaction transaction, String errorMessage) {
            this.success = success;
            this.transaction = transaction;
            this.errorMessage = errorMessage;
        }
        
        public static QueryTransactionResult success(FinancialTransaction transaction) {
            return new QueryTransactionResult(true, transaction, null);
        }
        
        public static QueryTransactionResult failure(String errorMessage) {
            return new QueryTransactionResult(false, null, errorMessage);
        }
        
        public boolean isSuccess() { return success; }
        public FinancialTransaction getTransaction() { return transaction; }
        public String getErrorMessage() { return errorMessage; }
    }
}