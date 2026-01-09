package com.example.ddd.batchupdate.domain.model.valueobject;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 交易状态枚举值对象
 * 定义交易的所有可能状态及状态转换规则
 */
@Getter
@AllArgsConstructor
public enum TransactionStatus {
    
    /**
     * 待处理状态
     */
    PENDING("待处理", "交易已创建，等待处理"),
    
    /**
     * 处理中状态
     */
    PROCESSING("处理中", "交易正在处理中"),
    
    /**
     * 成功状态
     */
    SUCCESS("成功", "交易处理成功"),
    
    /**
     * 失败状态
     */
    FAILED("失败", "交易处理失败"),
    
    /**
     * 已取消状态
     */
    CANCELLED("已取消", "交易已被取消"),
    
    /**
     * 已回滚状态
     */
    ROLLBACK("已回滚", "交易已回滚到之前状态");

    private final String description;
    private final String detail;

    /**
     * 检查状态是否有效
     */
    public boolean isValid() {
        return this != PENDING && this != CANCELLED && this != ROLLBACK;
    }

    /**
     * 检查状态是否表示完成
     */
    public boolean isCompleted() {
        return this == SUCCESS || this == FAILED || this == CANCELLED || this == ROLLBACK;
    }

    /**
     * 检查状态是否表示处理中
     */
    public boolean isProcessing() {
        return this == PROCESSING;
    }

    /**
     * 检查状态是否允许更新
     */
    public boolean allowsUpdate() {
        return this == PENDING || this == FAILED;
    }

    /**
     * 验证状态转换是否合法
     */
    public boolean canTransitionTo(TransactionStatus targetStatus) {
        switch (this) {
            case PENDING:
                return targetStatus == PROCESSING || targetStatus == CANCELLED;
            case PROCESSING:
                return targetStatus == SUCCESS || targetStatus == FAILED || targetStatus == CANCELLED;
            case SUCCESS:
            case FAILED:
            case CANCELLED:
            case ROLLBACK:
                return false; // 已完成状态不能转换
            default:
                return false;
        }
    }

    /**
     * 获取状态转换的描述
     */
    public String getTransitionDescription(TransactionStatus targetStatus) {
        if (!canTransitionTo(targetStatus)) {
            return String.format("不能从状态 '%s' 转换到 '%s'", this.getDescription(), targetStatus.getDescription());
        }
        return String.format("状态转换: %s -> %s", this.getDescription(), targetStatus.getDescription());
    }
}