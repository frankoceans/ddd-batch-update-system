package com.example.ddd.batchupdate.domain.model.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.UUID;

/**
 * 流ID值对象
 * 确保流ID的完整性和有效性
 */
@Value
@EqualsAndHashCode
public class StreamId implements Serializable {
    String value;

    private StreamId(String value) {
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException("流ID不能为空");
        }
        this.value = value;
    }

    /**
     * 创建流ID
     */
    public static StreamId of(String value) {
        return new StreamId(value);
    }

    /**
     * 生成随机流ID
     */
    public static StreamId generate() {
        return new StreamId(UUID.randomUUID().toString());
    }

    /**
     * 从字符串创建流ID
     */
    public static StreamId fromString(String value) {
        return new StreamId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}