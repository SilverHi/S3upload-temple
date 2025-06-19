package com.example.s3upload.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * S3文件上传请求DTO
 * 
 * 用于封装文件上传请求的参数信息
 * 
 * @author Generated
 * @version 1.0.0
 */
@Data
public class S3UploadRequest {

    /**
     * 文件内容（Base64编码）
     * 必填字段
     */
    @NotBlank(message = "文件内容不能为空")
    private String fileContent;

    /**
     * 文件名称
     * 必填字段，包含文件扩展名
     */
    @NotBlank(message = "文件名不能为空")
    private String fileName;

    /**
     * 文件在S3中的路径前缀（可选）
     * 如果不提供，将使用默认路径
     * 例如：images/, documents/, uploads/2024/
     */
    private String pathPrefix;

    /**
     * 文件内容类型（MIME类型）
     * 可选字段，如果不提供将根据文件扩展名自动推断
     * 例如：image/jpeg, application/pdf, text/plain
     */
    private String contentType;

    /**
     * 是否覆盖已存在的文件
     * 默认为false，如果文件已存在将返回错误
     */
    private boolean overwrite = false;
} 