package com.example.s3upload;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * S3文件上传演示应用程序主启动类
 * 
 * 这是一个Spring Boot 3应用程序，演示如何集成AWS S3进行文件上传操作
 * 
 * @author Generated
 * @version 1.0.0
 */
@SpringBootApplication
public class S3UploadApplication {

    /**
     * 应用程序入口点
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(S3UploadApplication.class, args);
    }
} 