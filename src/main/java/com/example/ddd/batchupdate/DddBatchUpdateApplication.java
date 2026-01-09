package com.example.ddd.batchupdate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * DDD批量更新系统主应用程序
 */
@Slf4j
@SpringBootApplication
public class DddBatchUpdateApplication {
    
    public static void main(String[] args) {
        log.info("启动DDD批量更新系统...");
        SpringApplication.run(DddBatchUpdateApplication.class, args);
        log.info("DDD批量更新系统启动完成！");
    }
}