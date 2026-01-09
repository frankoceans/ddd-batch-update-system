package com.example.ddd.batchupdate.domain.model.entity;

import com.example.ddd.batchupdate.domain.model.valueobject.StreamId;
import com.example.ddd.batchupdate.domain.model.valueobject.Timestamps;
import com.example.ddd.batchupdate.domain.model.valueobject.TransactionStatus;
import com.example.ddd.batchupdate.domain.model.valueobject.Version;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 金融交易聚合根
 * 交易聚合的边界，包含交易的所有相关数据和业务规则
 */
@Value
@EqualsAndHashCode
public class FinancialTransaction implements Serializable {
    
    /**
     * 聚合根ID
     */
    String id;
    
    /**
     * 流ID
     */
    StreamId streamId;
    
    /**
     * 交易状态
     */
    TransactionStatus status;
    
    /**
     * 更新时间
     */
    java.time.LocalDateTime updateTime;
    
    /**
     * 更新人
     */
    String updateBy;
    
    /**
     * 创建时间
     */
    java.time.LocalDateTime createTime;
    
    /**
     * 创建人
     */
    String createBy;
    
    /**
     * 版本号（用于乐观锁）
     */
    Version version;
    
    /**
     * 交易记录集合
     */
    Set<TransactionRecord> records;

    private FinancialTransaction(String id, StreamId streamId, TransactionStatus status,
                               java.time.LocalDateTime updateTime, String updateBy,
                               java.time.LocalDateTime createTime, String createBy,
                               Version version, Set<TransactionRecord> records) {
        if (Objects.isNull(id) || id.trim().isEmpty()) {
            throw new IllegalArgumentException("交易ID不能为空");
        }
        if (Objects.isNull(streamId)) {
            throw new IllegalArgumentException("流ID不能为空");
        }
        if (Objects.isNull(status)) {
            throw new IllegalArgumentException("交易状态不能为空");
        }
        if (Objects.isNull(updateTime)) {
            throw new IllegalArgumentException("更新时间不能为空");
        }
        if (Objects.isNull(updateBy) || updateBy.trim().isEmpty()) {
            throw new IllegalArgumentException("更新人不能为空");
        }
        if (Objects.isNull(createTime)) {
            throw new IllegalArgumentException("创建时间不能为空");
        }
        if (Objects.isNull(createBy) || createBy.trim().isEmpty()) {
            throw new IllegalArgumentException("创建人不能为空");
        }
        if (Objects.isNull(version)) {
            throw new IllegalArgumentException("版本号不能为空");
        }
        if (Objects.isNull(records)) {
            throw new IllegalArgumentException("交易记录不能为空");
        }
        
        this.id = id;
        this.streamId = streamId;
        this.status = status;
        this.updateTime = updateTime;
        this.updateBy = updateBy;
        this.createTime = createTime;
        this.createBy = createBy;
        this.version = version;
        this.records = Collections.unmodifiableSet(new HashSet<>(records));
        
        // 验证业务规则
        validateBusinessRules();
    }

    /**
     * 创建新的金融交易
     */
    public static FinancialTransaction create(String id, StreamId streamId, 
                                             Set<TransactionRecord> records, 
                                             String createBy) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        
        return new FinancialTransaction(
            id,
            streamId,
            TransactionStatus.PENDING,
            now,
            createBy,
            now,
            createBy,
            Version.initial(),
            records
        );
    }

    /**
     * 批量更新交易记录
     */
    public FinancialTransaction batchUpdateRecords(Map<String, TransactionRecord> updates, 
                                                 String updateBy) {
        if (updates == null || updates.isEmpty()) {
            return this;
        }

        // 检查当前状态是否允许更新
        if (!this.status.allowsUpdate()) {
            throw new IllegalStateException(
                String.format("当前状态 %s 不允许批量更新", this.status.getDescription())
            );
        }

        // 验证更新数据
        validateBatchUpdateData(updates);

        // 创建更新后的记录集合
        Set<TransactionRecord> updatedRecords = new HashSet<>(this.records);
        
        for (Map.Entry<String, TransactionRecord> entry : updates.entrySet()) {
            String recordId = entry.getKey();
            TransactionRecord newRecord = entry.getValue();
            
            // 查找现有记录
            Optional<TransactionRecord> existingRecord = updatedRecords.stream()
                .filter(r -> r.getRecordId().equals(recordId))
                .findFirst();
            
            if (existingRecord.isPresent()) {
                // 更新现有记录
                updatedRecords.remove(existingRecord.get());
                updatedRecords.add(newRecord);
            } else {
                // 添加新记录
                updatedRecords.add(newRecord);
            }
        }

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        
        return new FinancialTransaction(
            this.id,
            this.streamId,
            this.status,
            now,
            updateBy,
            this.createTime,
            this.createBy,
            this.version.next(),
            updatedRecords
        );
    }

    /**
     * 更新交易状态
     */
    public FinancialTransaction updateStatus(TransactionStatus newStatus, String updateBy) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                String.format("状态转换不合法: %s -> %s", this.status.getDescription(), newStatus.getDescription())
            );
        }

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        
        return new FinancialTransaction(
            this.id,
            this.streamId,
            newStatus,
            now,
            updateBy,
            this.createTime,
            this.createBy,
            this.version.next(),
            this.records
        );
    }

    /**
     * 标记为处理中
     */
    public FinancialTransaction markAsProcessing(String updateBy) {
        return updateStatus(TransactionStatus.PROCESSING, updateBy);
    }

    /**
     * 标记为成功
     */
    public FinancialTransaction markAsSuccess(String updateBy) {
        return updateStatus(TransactionStatus.SUCCESS, updateBy);
    }

    /**
     * 标记为失败
     */
    public FinancialTransaction markAsFailed(String updateBy) {
        return updateStatus(TransactionStatus.FAILED, updateBy);
    }

    /**
     * 获取交易记录数量
     */
    public int getRecordCount() {
        return records.size();
    }

    /**
     * 获取指定状态的记录数量
     */
    public long getRecordCountByStatus(TransactionStatus status) {
        return records.stream()
            .filter(record -> record.getStatus() == status)
            .count();
    }

    /**
     * 获取成功处理的记录数量
     */
    public long getSuccessRecordCount() {
        return getRecordCountByStatus(TransactionStatus.SUCCESS);
    }

    /**
     * 获取失败处理的记录数量
     */
    public long getFailedRecordCount() {
        return getRecordCountByStatus(TransactionStatus.FAILED);
    }

    /**
     * 检查是否可以批量更新
     */
    public boolean canBatchUpdate() {
        return this.status.allowsUpdate() && !this.records.isEmpty();
    }

    /**
     * 检查是否所有记录都已完成处理
     */
    public boolean isAllRecordsCompleted() {
        return records.stream()
            .allMatch(record -> record.getStatus().isCompleted());
    }

    /**
     * 检查是否有处理中的记录
     */
    public boolean hasProcessingRecords() {
        return records.stream()
            .anyMatch(record -> record.getStatus().isProcessing());
    }

    /**
     * 获取处理进度百分比
     */
    public double getProcessingProgress() {
        if (records.isEmpty()) {
            return 0.0;
        }
        
        long completedRecords = records.stream()
            .filter(record -> record.getStatus().isCompleted())
            .count();
        
        return (completedRecords * 100.0) / records.size();
    }

    /**
     * 验证批量更新数据
     */
    private void validateBatchUpdateData(Map<String, TransactionRecord> updates) {
        if (updates.size() > 1000) {
            throw new IllegalArgumentException("单次批量更新不能超过1000条记录");
        }

        // 验证所有更新的记录都属于当前聚合
        for (TransactionRecord record : updates.values()) {
            if (!record.getStreamId().equals(this.streamId)) {
                throw new IllegalArgumentException(
                    String.format("记录 %s 不属于当前流 %s", record.getRecordId(), this.streamId)
                );
            }
        }
    }

    /**
     * 验证业务规则
     */
    private void validateBusinessRules() {
        // 验证创建时间和更新时间的逻辑关系
        if (this.updateTime.isBefore(this.createTime)) {
            throw new IllegalArgumentException("更新时间不能早于创建时间");
        }

        // 验证聚合内的所有记录都属于同一个流
        Set<String> uniqueStreamIds = records.stream()
            .map(record -> record.getStreamId().getValue())
            .collect(Collectors.toSet());
        
        if (uniqueStreamIds.size() > 1) {
            throw new IllegalArgumentException("聚合内的所有记录必须属于同一个流");
        }
    }

    /**
     * 获取不可变记录集合
     */
    public Set<TransactionRecord> getRecords() {
        return records;
    }

    /**
     * 获取按状态分组的记录
     */
    public Map<TransactionStatus, Set<TransactionRecord>> getRecordsByStatus() {
        return records.stream()
            .collect(Collectors.groupingBy(
                TransactionRecord::getStatus,
                Collectors.toSet()
            ));
    }
}