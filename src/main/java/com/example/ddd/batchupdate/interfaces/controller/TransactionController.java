package com.example.ddd.batchupdate.interfaces.controller;

import com.example.ddd.batchupdate.application.TransactionApplicationService;
import com.example.ddd.batchupdate.domain.model.valueobject.TransactionStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 交易REST接口控制器
 * 提供HTTP API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/transactions")
@CrossOrigin(origins = "*")
public class TransactionController {
    
    private final TransactionApplicationService applicationService;
    
    public TransactionController(TransactionApplicationService applicationService) {
        this.applicationService = applicationService;
    }
    
    /**
     * 创建交易
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TransactionApplicationService.CreateTransactionResult>> createTransaction(
            @RequestBody CreateTransactionRequest request) {
        
        log.info("收到创建交易请求");
        
        try {
            TransactionApplicationService.CreateTransactionCommand command = 
                new TransactionApplicationService.CreateTransactionCommand(
                    request.getStreamId(),
                    request.getRecordData(),
                    request.getOperator()
                );
            
            TransactionApplicationService.CreateTransactionResult result = applicationService.createTransaction(command);
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(ApiResponse.success(result));
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.failure(result.getErrorMessage()));
            }
            
        } catch (Exception e) {
            log.error("创建交易失败", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.failure("创建交易失败: " + e.getMessage()));
        }
    }
    
    /**
     * 批量更新交易状态
     */
    @PutMapping("/batch-status")
    public ResponseEntity<ApiResponse<TransactionApplicationService.BatchUpdateStatusResult>> batchUpdateStatus(
            @RequestBody BatchUpdateStatusRequest request) {
        
        log.info("收到批量更新状态请求");
        
        try {
            TransactionApplicationService.BatchUpdateStatusCommand command = 
                new TransactionApplicationService.BatchUpdateStatusCommand(
                    request.getTransactionIds(),
                    request.getNewStatus(),
                    request.getOperator()
                );
            
            TransactionApplicationService.BatchUpdateStatusResult result = applicationService.batchUpdateStatus(command);
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(ApiResponse.success(result));
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.failure("批量更新失败"));
            }
            
        } catch (Exception e) {
            log.error("批量更新状态失败", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.failure("批量更新失败: " + e.getMessage()));
        }
    }
    
    /**
     * 批量处理交易记录
     */
    @PutMapping("/batch-records")
    public ResponseEntity<ApiResponse<TransactionApplicationService.BatchProcessRecordsResult>> batchProcessRecords(
            @RequestBody BatchProcessRecordsRequest request) {
        
        log.info("收到批量处理记录请求");
        
        try {
            // 这里需要将DTO转换为TransactionRecord对象
            // 实际项目中应该使用映射器
            Map<String, com.example.ddd.batchupdate.domain.model.entity.TransactionRecord> updates = 
                Map.of(); // 模拟空更新
            
            TransactionApplicationService.BatchProcessRecordsCommand command = 
                new TransactionApplicationService.BatchProcessRecordsCommand(
                    request.getStreamId(),
                    updates,
                    request.getOperator()
                );
            
            TransactionApplicationService.BatchProcessRecordsResult result = applicationService.batchProcessRecords(command);
            
            if (result.isProcessed()) {
                return ResponseEntity.ok(ApiResponse.success(result));
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.failure("批量处理失败"));
            }
            
        } catch (Exception e) {
            log.error("批量处理记录失败", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.failure("批量处理失败: " + e.getMessage()));
        }
    }
    
    /**
     * 查询交易
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<ApiResponse<TransactionApplicationService.QueryTransactionResult>> queryTransaction(
            @PathVariable String transactionId) {
        
        log.info("收到查询交易请求: {}", transactionId);
        
        try {
            TransactionApplicationService.QueryTransactionCommand command = 
                new TransactionApplicationService.QueryTransactionCommand(transactionId);
            
            TransactionApplicationService.QueryTransactionResult result = applicationService.queryTransaction(command);
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(ApiResponse.success(result));
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            log.error("查询交易失败", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.failure("查询失败: " + e.getMessage()));
        }
    }
    
    /**
     * 批量更新状态请求
     */
    public static class BatchUpdateStatusRequest {
        private Set<String> transactionIds;
        private TransactionStatus newStatus;
        private String operator;
        
        // Getters and Setters
        public Set<String> getTransactionIds() { return transactionIds; }
        public void setTransactionIds(Set<String> transactionIds) { this.transactionIds = transactionIds; }
        
        public TransactionStatus getNewStatus() { return newStatus; }
        public void setNewStatus(TransactionStatus newStatus) { this.newStatus = newStatus; }
        
        public String getOperator() { return operator; }
        public void setOperator(String operator) { this.operator = operator; }
    }
    
    /**
     * 批量处理记录请求
     */
    public static class BatchProcessRecordsRequest {
        private String streamId;
        private Map<String, String> updates; // 简化版本，实际应该是更复杂的结构
        private String operator;
        
        // Getters and Setters
        public String getStreamId() { return streamId; }
        public void setStreamId(String streamId) { this.streamId = streamId; }
        
        public Map<String, String> getUpdates() { return updates; }
        public void setUpdates(Map<String, String> updates) { this.updates = updates; }
        
        public String getOperator() { return operator; }
        public void setOperator(String operator) { this.operator = operator; }
    }
    
    /**
     * 创建交易请求
     */
    public static class CreateTransactionRequest {
        private String streamId;
        private List<String> recordData;
        private String operator;
        
        // Getters and Setters
        public String getStreamId() { return streamId; }
        public void setStreamId(String streamId) { this.streamId = streamId; }
        
        public List<String> getRecordData() { return recordData; }
        public void setRecordData(List<String> recordData) { this.recordData = recordData; }
        
        public String getOperator() { return operator; }
        public void setOperator(String operator) { this.operator = operator; }
    }
}