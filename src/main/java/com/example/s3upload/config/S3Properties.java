package com.example.s3upload.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AWS S3配置属性类
 * 
 * 此类用于绑定和管理AWS S3相关的配置参数。
 * 配置的优先级：
 * 1. 环境变量（最高优先级）
 * 2. application.yml/application.properties配置文件
 * 3. 默认值（如果有的话）
 * 
 * 环境变量映射规则：
 * - aws.s3.access-key -> AWS_S3_ACCESS_KEY
 * - aws.s3.secret-key -> AWS_S3_SECRET_KEY
 * - aws.s3.region -> AWS_S3_REGION
 * - aws.s3.bucket-name -> AWS_S3_BUCKET_NAME
 * 
 * @author Generated
 * @version 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "aws.s3")
public class S3Properties {

    /**
     * AWS访问密钥ID
     * 环境变量：AWS_S3_ACCESS_KEY
     * 配置文件：aws.s3.access-key
     */
    private String accessKey;

    /**
     * AWS秘密访问密钥
     * 环境变量：AWS_S3_SECRET_KEY
     * 配置文件：aws.s3.secret-key
     */
    private String secretKey;

    /**
     * AWS区域
     * 环境变量：AWS_S3_REGION
     * 配置文件：aws.s3.region
     * 默认值：us-east-1
     */
    private String region = "us-east-1";

    /**
     * S3存储桶名称
     * 环境变量：AWS_S3_BUCKET_NAME
     * 配置文件：aws.s3.bucket-name
     */
    private String bucketName;

    /**
     * S3终端点URL（可选，用于本地测试或私有云）
     * 环境变量：AWS_S3_ENDPOINT_URL
     * 配置文件：aws.s3.endpoint-url
     */
    private String endpointUrl;

    /**
     * 是否使用路径样式访问（可选，默认false）
     * 环境变量：AWS_S3_PATH_STYLE_ACCESS
     * 配置文件：aws.s3.path-style-access
     */
    private boolean pathStyleAccess = false;

    /**
     * 连接超时时间（毫秒）
     * 配置文件：aws.s3.connection-timeout
     * 默认值：30000（30秒）
     */
    private int connectionTimeout = 30000;

    /**
     * 读取超时时间（毫秒）
     * 配置文件：aws.s3.read-timeout
     * 默认值：60000（60秒）
     */
    private int readTimeout = 60000;

    /**
     * 验证必需的配置是否已设置
     * 
     * @return 如果所有必需的配置都已设置，则返回true；否则返回false
     */
    public boolean isValid() {
        return accessKey != null && !accessKey.trim().isEmpty() &&
               secretKey != null && !secretKey.trim().isEmpty() &&
               bucketName != null && !bucketName.trim().isEmpty() &&
               region != null && !region.trim().isEmpty();
    }

    /**
     * 获取缺失的配置项列表
     * 
     * @return 缺失的配置项描述字符串
     */
    public String getMissingConfigurations() {
        StringBuilder missing = new StringBuilder();
        
        if (accessKey == null || accessKey.trim().isEmpty()) {
            missing.append("AWS访问密钥ID (aws.s3.access-key 或环境变量 AWS_S3_ACCESS_KEY); ");
        }
        
        if (secretKey == null || secretKey.trim().isEmpty()) {
            missing.append("AWS秘密访问密钥 (aws.s3.secret-key 或环境变量 AWS_S3_SECRET_KEY); ");
        }
        
        if (bucketName == null || bucketName.trim().isEmpty()) {
            missing.append("S3存储桶名称 (aws.s3.bucket-name 或环境变量 AWS_S3_BUCKET_NAME); ");
        }
        
        if (region == null || region.trim().isEmpty()) {
            missing.append("AWS区域 (aws.s3.region 或环境变量 AWS_S3_REGION); ");
        }
        
        return missing.toString();
    }
} 