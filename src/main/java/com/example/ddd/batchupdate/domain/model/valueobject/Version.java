package com.example.ddd.batchupdate.domain.model.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * 版本号值对象
 * 用于实现乐观锁控制
 */
@Value
@EqualsAndHashCode
public class Version {
    Long value;

    private Version(Long value) {
        if (value == null || value < 0) {
            throw new IllegalArgumentException("版本号必须大于等于0");
        }
        this.value = value;
    }

    /**
     * 创建初始版本号
     */
    public static Version initial() {
        return new Version(1L);
    }

    /**
     * 从数值创建版本号
     */
    public static Version of(Long value) {
        return new Version(value);
    }

    /**
     * 创建下一个版本号
     */
    public Version next() {
        return new Version(value + 1);
    }

    /**
     * 检查版本号是否匹配
     */
    public boolean matches(Version other) {
        return this.value.equals(other.value);
    }

    /**
     * 检查版本号是否过时
     */
    public boolean isOutdated(Version other) {
        return this.value < other.value;
    }

    /**
     * 获取版本号的字符串表示
     */
    public String toVersionString() {
        return String.valueOf(value);
    }
}