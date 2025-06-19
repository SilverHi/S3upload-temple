package com.example.s3upload.service;

import com.example.s3upload.config.S3Properties;
import com.example.s3upload.dto.S3UploadRequest;
import com.example.s3upload.dto.S3UploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * AWS S3服务类
 * 
 * 提供与AWS S3交互的所有核心功能，包括：
 * - 文件上传
 * - 文件下载
 * - 文件删除
 * - 文件列表
 * - 存储桶操作
 * - 连接测试
 * 
 * 该服务类是S3操作的核心组件，封装了所有与AWS SDK的交互细节。
 * 
 * @author Generated
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final S3Properties s3Properties;

    /**
     * 测试S3连接
     * 
     * 此方法用于验证S3配置是否正确，以及是否能够成功连接到AWS S3服务。
     * 它会尝试列出指定存储桶的内容来验证连接。
     * 
     * @return 测试结果响应
     */
    public S3UploadResponse testConnection() {
        log.info("开始测试S3连接...");
        
        // 检查S3客户端是否可用
        if (s3Client == null) {
            log.error("S3客户端未初始化，可能是配置问题");
            return S3UploadResponse.configurationError(s3Properties.getMissingConfigurations());
        }

        // 验证配置完整性
        if (!s3Properties.isValid()) {
            log.error("S3配置不完整");
            return S3UploadResponse.configurationError(s3Properties.getMissingConfigurations());
        }

        try {
            // 尝试检查存储桶是否存在
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                .bucket(s3Properties.getBucketName())
                .build();
            
            s3Client.headBucket(headBucketRequest);
            log.info("S3连接测试成功！存储桶 '{}' 可访问", s3Properties.getBucketName());
            
            return S3UploadResponse.builder()
                .success(true)
                .message("S3连接测试成功")
                .bucketName(s3Properties.getBucketName())
                .uploadTime(LocalDateTime.now())
                .build();
                
        } catch (NoSuchBucketException e) {
            log.error("存储桶 '{}' 不存在", s3Properties.getBucketName());
            return S3UploadResponse.failure(
                "存储桶 '" + s3Properties.getBucketName() + "' 不存在", 
                "BUCKET_NOT_FOUND"
            );
        } catch (S3Exception e) {
            log.error("S3连接测试失败: {}", e.getMessage());
            return S3UploadResponse.failure(
                "S3连接失败: " + e.getMessage(), 
                e.awsErrorDetails().errorCode()
            );
        } catch (Exception e) {
            log.error("S3连接测试发生未知错误: {}", e.getMessage(), e);
            return S3UploadResponse.failure(
                "连接测试失败: " + e.getMessage(), 
                "UNKNOWN_ERROR"
            );
        }
    }

    /**
     * 上传文件到S3
     * 
     * 此方法负责将文件上传到AWS S3存储桶。支持以下功能：
     * - Base64编码的文件内容解码
     * - 自动内容类型检测
     * - 自定义文件路径
     * - 文件覆盖控制
     * - 详细的错误处理
     * 
     * @param uploadRequest 上传请求对象，包含文件内容和元数据
     * @return 上传结果响应
     */
    public S3UploadResponse uploadFile(S3UploadRequest uploadRequest) {
        log.info("开始上传文件: {}", uploadRequest.getFileName());
        
        // 检查S3客户端是否可用
        if (s3Client == null) {
            log.error("S3客户端未初始化");
            return S3UploadResponse.configurationError(s3Properties.getMissingConfigurations());
        }

        try {
            // 解码Base64文件内容
            byte[] fileBytes;
            try {
                fileBytes = Base64.getDecoder().decode(uploadRequest.getFileContent());
                log.debug("文件内容解码成功，大小: {} 字节", fileBytes.length);
            } catch (IllegalArgumentException e) {
                log.error("文件内容Base64解码失败: {}", e.getMessage());
                return S3UploadResponse.failure("文件内容格式无效，请确保是有效的Base64编码", "INVALID_FILE_CONTENT");
            }

            // 构建S3文件键（完整路径）
            String s3Key = buildS3Key(uploadRequest.getPathPrefix(), uploadRequest.getFileName());
            log.debug("生成的S3文件键: {}", s3Key);

            // 检查文件是否已存在（如果不允许覆盖）
            if (!uploadRequest.isOverwrite() && fileExists(s3Key)) {
                log.warn("文件已存在且不允许覆盖: {}", s3Key);
                return S3UploadResponse.failure("文件已存在，如需覆盖请设置overwrite=true", "FILE_ALREADY_EXISTS");
            }

            // 确定内容类型
            String contentType = determineContentType(uploadRequest.getContentType(), uploadRequest.getFileName());
            log.debug("确定的内容类型: {}", contentType);

            // 构建上传请求
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3Properties.getBucketName())
                .key(s3Key)
                .contentType(contentType)
                .contentLength((long) fileBytes.length)
                // 设置元数据
                .metadata(java.util.Map.of(
                    "original-filename", uploadRequest.getFileName(),
                    "upload-timestamp", LocalDateTime.now().toString(),
                    "uploaded-by", "s3-upload-service"
                ))
                .build();

            // 执行文件上传
            PutObjectResponse putObjectResponse = s3Client.putObject(
                putObjectRequest, 
                RequestBody.fromBytes(fileBytes)
            );

            log.info("文件上传成功: {}, ETag: {}", s3Key, putObjectResponse.eTag());

            // 生成文件访问URL
            String fileUrl = generateFileUrl(s3Key);

            // 返回成功响应
            return S3UploadResponse.success(
                s3Key,
                fileUrl,
                (long) fileBytes.length,
                contentType,
                s3Properties.getBucketName()
            );

        } catch (S3Exception e) {
            log.error("S3上传失败: {}", e.getMessage());
            return S3UploadResponse.failure(
                "S3上传失败: " + e.getMessage(),
                e.awsErrorDetails().errorCode()
            );
        } catch (Exception e) {
            log.error("文件上传发生未知错误: {}", e.getMessage(), e);
            return S3UploadResponse.failure(
                "文件上传失败: " + e.getMessage(),
                "UNKNOWN_ERROR"
            );
        }
    }

    /**
     * 检查文件是否存在于S3中
     * 
     * @param s3Key 文件在S3中的键
     * @return 如果文件存在返回true，否则返回false
     */
    public boolean fileExists(String s3Key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                .bucket(s3Properties.getBucketName())
                .key(s3Key)
                .build();
            
            s3Client.headObject(headObjectRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            log.warn("检查文件是否存在时发生错误: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从S3删除文件
     * 
     * @param s3Key 要删除的文件键
     * @return 删除结果响应
     */
    public S3UploadResponse deleteFile(String s3Key) {
        log.info("开始删除文件: {}", s3Key);
        
        if (s3Client == null) {
            return S3UploadResponse.configurationError(s3Properties.getMissingConfigurations());
        }

        try {
            // 检查文件是否存在
            if (!fileExists(s3Key)) {
                return S3UploadResponse.failure("文件不存在: " + s3Key, "FILE_NOT_FOUND");
            }

            // 执行删除操作
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(s3Properties.getBucketName())
                .key(s3Key)
                .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("文件删除成功: {}", s3Key);

            return S3UploadResponse.builder()
                .success(true)
                .message("文件删除成功")
                .s3Key(s3Key)
                .bucketName(s3Properties.getBucketName())
                .uploadTime(LocalDateTime.now())
                .build();

        } catch (S3Exception e) {
            log.error("S3删除失败: {}", e.getMessage());
            return S3UploadResponse.failure(
                "删除失败: " + e.getMessage(),
                e.awsErrorDetails().errorCode()
            );
        } catch (Exception e) {
            log.error("删除文件发生未知错误: {}", e.getMessage(), e);
            return S3UploadResponse.failure(
                "删除失败: " + e.getMessage(),
                "UNKNOWN_ERROR"
            );
        }
    }

    /**
     * 列出S3存储桶中的文件
     * 
     * @param prefix 文件键前缀（用于过滤）
     * @param maxKeys 最大返回数量
     * @return 文件列表
     */
    public List<S3Object> listFiles(String prefix, int maxKeys) {
        log.info("列出文件，前缀: {}, 最大数量: {}", prefix, maxKeys);
        
        if (s3Client == null) {
            throw new RuntimeException("S3客户端未初始化");
        }

        try {
            ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                .bucket(s3Properties.getBucketName())
                .maxKeys(maxKeys);
            
            if (prefix != null && !prefix.trim().isEmpty()) {
                requestBuilder.prefix(prefix);
            }

            ListObjectsV2Response response = s3Client.listObjectsV2(requestBuilder.build());
            log.info("找到 {} 个文件", response.contents().size());
            
            return response.contents();
            
        } catch (Exception e) {
            log.error("列出文件时发生错误: {}", e.getMessage(), e);
            throw new RuntimeException("列出文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建S3文件键（完整路径）
     * 
     * @param pathPrefix 路径前缀
     * @param fileName 文件名
     * @return 完整的S3文件键
     */
    private String buildS3Key(String pathPrefix, String fileName) {
        // 如果没有提供路径前缀，使用默认的基于时间的路径
        if (pathPrefix == null || pathPrefix.trim().isEmpty()) {
            String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            pathPrefix = "uploads/" + datePrefix + "/";
        }
        
        // 确保路径前缀以斜杠结尾
        if (!pathPrefix.endsWith("/")) {
            pathPrefix += "/";
        }
        
        // 为文件名添加UUID前缀以避免冲突
        String uniqueFileName = UUID.randomUUID().toString() + "_" + fileName;
        
        return pathPrefix + uniqueFileName;
    }

    /**
     * 确定文件的内容类型
     * 
     * @param providedContentType 用户提供的内容类型
     * @param fileName 文件名
     * @return 确定的内容类型
     */
    private String determineContentType(String providedContentType, String fileName) {
        // 如果用户提供了内容类型，优先使用
        if (providedContentType != null && !providedContentType.trim().isEmpty()) {
            return providedContentType.trim();
        }
        
        // 根据文件扩展名推断内容类型
        String lowerCaseFileName = fileName.toLowerCase();
        
        if (lowerCaseFileName.endsWith(".jpg") || lowerCaseFileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerCaseFileName.endsWith(".png")) {
            return "image/png";
        } else if (lowerCaseFileName.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerCaseFileName.endsWith(".pdf")) {
            return "application/pdf";
        } else if (lowerCaseFileName.endsWith(".txt")) {
            return "text/plain";
        } else if (lowerCaseFileName.endsWith(".doc")) {
            return "application/msword";
        } else if (lowerCaseFileName.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        } else if (lowerCaseFileName.endsWith(".xls")) {
            return "application/vnd.ms-excel";
        } else if (lowerCaseFileName.endsWith(".xlsx")) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else {
            // 默认为二进制流
            return "application/octet-stream";
        }
    }

    /**
     * 生成文件的访问URL
     * 
     * @param s3Key 文件在S3中的键
     * @return 文件访问URL
     */
    private String generateFileUrl(String s3Key) {
        try {
            // 构建标准的S3 URL
            String encodedKey = URLEncoder.encode(s3Key, StandardCharsets.UTF_8)
                .replace("+", "%20"); // 处理空格编码
            
            if (s3Properties.getEndpointUrl() != null && !s3Properties.getEndpointUrl().trim().isEmpty()) {
                // 使用自定义终端点
                return s3Properties.getEndpointUrl() + "/" + s3Properties.getBucketName() + "/" + encodedKey;
            } else {
                // 使用标准AWS S3 URL
                return String.format("https://%s.s3.%s.amazonaws.com/%s",
                    s3Properties.getBucketName(),
                    s3Properties.getRegion(),
                    encodedKey);
            }
        } catch (Exception e) {
            log.warn("生成文件URL时发生错误: {}", e.getMessage());
            return "无法生成URL";
        }
    }
} 