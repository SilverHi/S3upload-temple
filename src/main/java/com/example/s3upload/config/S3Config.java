package com.example.s3upload.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import java.net.URI;
import java.time.Duration;

/**
 * AWS S3配置类
 * 
 * 此类负责创建和配置AWS S3客户端实例。
 * 它会根据配置属性创建一个S3客户端Bean，供其他组件使用。
 * 
 * 配置读取顺序：
 * 1. 首先从环境变量读取（优先级最高）
 * 2. 然后从application.yml/properties配置文件读取
 * 3. 使用默认值（如果配置了的话）
 * 
 * @author Generated
 * @version 1.0.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(S3Properties.class)
public class S3Config {

    private final S3Properties s3Properties;

    /**
     * 创建AWS S3客户端Bean
     * 
     * 此方法会根据配置属性创建一个完全配置的S3客户端实例。
     * 客户端配置包括：
     * - 认证凭据（访问密钥和秘密密钥）
     * - 区域设置
     * - 超时配置
     * - 自定义终端点（如果配置了的话）
     * - 路径样式访问（如果启用的话）
     * 
     * @return 配置好的S3Client实例，如果配置无效则返回null
     */
    @Bean
    public S3Client s3Client() {
        log.info("正在初始化AWS S3客户端配置...");
        
        // 验证配置是否完整
        if (!s3Properties.isValid()) {
            log.error("S3配置不完整，无法创建S3客户端。缺失的配置项: {}", 
                     s3Properties.getMissingConfigurations());
            log.info("请设置以下环境变量或在application.yml中配置:");
            log.info("  环境变量: AWS_S3_ACCESS_KEY, AWS_S3_SECRET_KEY, AWS_S3_REGION, AWS_S3_BUCKET_NAME");
            log.info("  配置文件: aws.s3.access-key, aws.s3.secret-key, aws.s3.region, aws.s3.bucket-name");
            return null;
        }

        try {
            // 创建AWS认证凭据
            AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(
                s3Properties.getAccessKey(),
                s3Properties.getSecretKey()
            );
            log.debug("AWS凭据创建成功");

            // 配置客户端超时设置
            ClientOverrideConfiguration clientConfig = ClientOverrideConfiguration.builder()
                .apiCallTimeout(Duration.ofMillis(s3Properties.getConnectionTimeout()))
                .apiCallAttemptTimeout(Duration.ofMillis(s3Properties.getReadTimeout()))
                .build();
            log.debug("客户端超时配置创建成功: 连接超时={}ms, 读取超时={}ms", 
                     s3Properties.getConnectionTimeout(), s3Properties.getReadTimeout());

            // 构建S3客户端
            S3ClientBuilder clientBuilder = S3Client.builder()
                .region(Region.of(s3Properties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .overrideConfiguration(clientConfig);

            // 如果配置了自定义终端点，则使用它（通常用于本地测试或私有云）
            if (s3Properties.getEndpointUrl() != null && !s3Properties.getEndpointUrl().trim().isEmpty()) {
                clientBuilder.endpointOverride(URI.create(s3Properties.getEndpointUrl()));
                log.info("使用自定义S3终端点: {}", s3Properties.getEndpointUrl());
            }

            // 如果启用了路径样式访问，则配置它（主要用于兼容某些S3兼容存储）
            if (s3Properties.isPathStyleAccess()) {
                clientBuilder.forcePathStyle(true);
                log.info("启用S3路径样式访问");
            }

            S3Client s3Client = clientBuilder.build();
            
            log.info("AWS S3客户端初始化成功!");
            log.info("配置详情: 区域={}, 存储桶={}, 路径样式访问={}", 
                    s3Properties.getRegion(), 
                    s3Properties.getBucketName(), 
                    s3Properties.isPathStyleAccess());
            
            return s3Client;
            
        } catch (Exception e) {
            log.error("创建S3客户端时发生错误: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获取S3配置属性的只读访问
     * 
     * @return S3配置属性实例
     */
    @Bean
    public S3Properties s3Properties() {
        return s3Properties;
    }
} 