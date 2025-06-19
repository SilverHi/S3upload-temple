package com.example.s3upload.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * S3文件上传响应DTO
 * 
 * 用于封装文件上传操作的结果信息
 * 
 * @author Generated
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class S3UploadResponse {

    /**
     * 操作是否成功
     */
    private boolean success;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 文件在S3中的完整键（路径）
     */
    private String s3Key;

    /**
     * 文件的访问URL
     */
    private String fileUrl;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件内容类型
     */
    private String contentType;

    /**
     * 存储桶名称
     */
    private String bucketName;

    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;

    /**
     * 错误代码（如果操作失败）
     */
    private String errorCode;

    /**
     * 创建成功响应的静态方法
     * 
     * @param s3Key 文件在S3中的键
     * @param fileUrl 文件访问URL
     * @param fileSize 文件大小
     * @param contentType 文件内容类型
     * @param bucketName 存储桶名称
     * @return 成功响应对象
     */
    public static S3UploadResponse success(String s3Key, String fileUrl, Long fileSize, 
                                          String contentType, String bucketName) {
        return S3UploadResponse.builder()
            .success(true)
            .message("文件上传成功")
            .s3Key(s3Key)
            .fileUrl(fileUrl)
            .fileSize(fileSize)
            .contentType(contentType)
            .bucketName(bucketName)
            .uploadTime(LocalDateTime.now())
            .build();
    }

    /**
     * 创建失败响应的静态方法
     * 
     * @param message 错误消息
     * @param errorCode 错误代码
     * @return 失败响应对象
     */
    public static S3UploadResponse failure(String message, String errorCode) {
        return S3UploadResponse.builder()
            .success(false)
            .message(message)
            .errorCode(errorCode)
            .uploadTime(LocalDateTime.now())
            .build();
    }

    /**
     * 创建配置错误响应的静态方法
     * 
     * @param missingConfigs 缺失的配置项
     * @return 配置错误响应对象
     */
    public static S3UploadResponse configurationError(String missingConfigs) {
        return S3UploadResponse.builder()
            .success(false)
            .message("S3配置不完整，请检查以下配置项: " + missingConfigs)
            .errorCode("CONFIGURATION_ERROR")
            .uploadTime(LocalDateTime.now())
            .build();
    }
} 