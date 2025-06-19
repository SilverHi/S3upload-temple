package com.example.s3upload.controller;

import com.example.s3upload.dto.S3UploadRequest;
import com.example.s3upload.dto.S3UploadResponse;
import com.example.s3upload.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.s3.model.S3Object;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AWS S3文件操作REST控制器
 * 
 * 提供以下REST API端点：
 * - POST /api/s3/upload - 上传文件到S3
 * - GET /api/s3/test-connection - 测试S3连接
 * - DELETE /api/s3/delete/{s3Key} - 删除S3中的文件
 * - GET /api/s3/list - 列出S3存储桶中的文件
 * - GET /api/s3/health - 健康检查
 * 
 * 所有接口都包含详细的错误处理和响应格式化。
 * 
 * @author Generated
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/s3")
@RequiredArgsConstructor
@Validated
public class S3Controller {

    private final S3Service s3Service;

    /**
     * 测试S3连接
     * 
     * 此接口用于验证S3配置是否正确，以及应用程序是否能够成功连接到AWS S3服务。
     * 主要用于部署后的配置验证和健康检查。
     * 
     * GET /api/s3/test-connection
     * 
     * @return ResponseEntity<S3UploadResponse> 连接测试结果
     */
    @GetMapping("/test-connection")
    public ResponseEntity<S3UploadResponse> testConnection() {
        log.info("收到S3连接测试请求");
        
        try {
            S3UploadResponse response = s3Service.testConnection();
            
            if (response.isSuccess()) {
                log.info("S3连接测试成功");
                return ResponseEntity.ok(response);
            } else {
                log.warn("S3连接测试失败: {}", response.getMessage());
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            }
            
        } catch (Exception e) {
            log.error("S3连接测试发生异常: {}", e.getMessage(), e);
            S3UploadResponse errorResponse = S3UploadResponse.failure(
                "连接测试异常: " + e.getMessage(), 
                "TEST_EXCEPTION"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 上传文件到S3
     * 
     * 此接口接收包含Base64编码文件内容的请求，并将文件上传到AWS S3存储桶。
     * 支持自定义文件路径、内容类型检测和文件覆盖控制。
     * 
     * POST /api/s3/upload
     * Content-Type: application/json
     * 
     * 请求体示例：
     * {
     *   "fileContent": "base64编码的文件内容",
     *   "fileName": "example.jpg",
     *   "pathPrefix": "images/",
     *   "contentType": "image/jpeg",
     *   "overwrite": false
     * }
     * 
     * @param uploadRequest 文件上传请求对象
     * @return ResponseEntity<S3UploadResponse> 上传结果
     */
    @PostMapping("/upload")
    public ResponseEntity<S3UploadResponse> uploadFile(@Valid @RequestBody S3UploadRequest uploadRequest) {
        log.info("收到文件上传请求: 文件名={}, 路径前缀={}", 
                uploadRequest.getFileName(), uploadRequest.getPathPrefix());
        
        try {
            S3UploadResponse response = s3Service.uploadFile(uploadRequest);
            
            if (response.isSuccess()) {
                log.info("文件上传成功: {}", response.getS3Key());
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else {
                log.warn("文件上传失败: {}", response.getMessage());
                
                // 根据错误类型返回不同的HTTP状态码
                HttpStatus status = determineHttpStatus(response.getErrorCode());
                return ResponseEntity.status(status).body(response);
            }
            
        } catch (Exception e) {
            log.error("文件上传发生异常: {}", e.getMessage(), e);
            S3UploadResponse errorResponse = S3UploadResponse.failure(
                "上传异常: " + e.getMessage(), 
                "UPLOAD_EXCEPTION"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 删除S3中的文件
     * 
     * 此接口用于删除S3存储桶中的指定文件。
     * 
     * DELETE /api/s3/delete/{s3Key}
     * 
     * @param s3Key 要删除的文件在S3中的键（路径），需要进行URL编码
     * @return ResponseEntity<S3UploadResponse> 删除结果
     */
    @DeleteMapping("/delete/{s3Key}")
    public ResponseEntity<S3UploadResponse> deleteFile(
            @PathVariable @NotBlank(message = "S3文件键不能为空") String s3Key) {
        
        log.info("收到文件删除请求: {}", s3Key);
        
        try {
            S3UploadResponse response = s3Service.deleteFile(s3Key);
            
            if (response.isSuccess()) {
                log.info("文件删除成功: {}", s3Key);
                return ResponseEntity.ok(response);
            } else {
                log.warn("文件删除失败: {}", response.getMessage());
                HttpStatus status = determineHttpStatus(response.getErrorCode());
                return ResponseEntity.status(status).body(response);
            }
            
        } catch (Exception e) {
            log.error("文件删除发生异常: {}", e.getMessage(), e);
            S3UploadResponse errorResponse = S3UploadResponse.failure(
                "删除异常: " + e.getMessage(), 
                "DELETE_EXCEPTION"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 列出S3存储桶中的文件
     * 
     * 此接口用于获取S3存储桶中的文件列表，支持前缀过滤和数量限制。
     * 
     * GET /api/s3/list?prefix=images/&maxKeys=50
     * 
     * @param prefix 文件键前缀，用于过滤文件（可选）
     * @param maxKeys 最大返回数量，默认为50，最大为1000
     * @return ResponseEntity<Map<String, Object>> 文件列表和元数据
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listFiles(
            @RequestParam(required = false) String prefix,
            @RequestParam(defaultValue = "50") int maxKeys) {
        
        log.info("收到文件列表请求: 前缀={}, 最大数量={}", prefix, maxKeys);
        
        // 限制最大返回数量
        if (maxKeys > 1000) {
            maxKeys = 1000;
        }
        
        try {
            List<S3Object> s3Objects = s3Service.listFiles(prefix, maxKeys);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "文件列表获取成功");
            response.put("totalCount", s3Objects.size());
            response.put("files", s3Objects.stream().map(obj -> {
                Map<String, Object> fileInfo = new HashMap<>();
                fileInfo.put("key", obj.key());
                fileInfo.put("size", obj.size());
                fileInfo.put("lastModified", obj.lastModified());
                fileInfo.put("eTag", obj.eTag());
                fileInfo.put("storageClass", obj.storageClass().toString());
                return fileInfo;
            }).toList());
            
            log.info("文件列表获取成功，共 {} 个文件", s3Objects.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取文件列表发生异常: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取文件列表失败: " + e.getMessage());
            errorResponse.put("errorCode", "LIST_EXCEPTION");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 应用程序健康检查
     * 
     * 提供应用程序基本状态信息，包括S3配置状态。
     * 
     * GET /api/s3/health
     * 
     * @return ResponseEntity<Map<String, Object>> 健康检查结果
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        log.debug("收到健康检查请求");
        
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", java.time.LocalDateTime.now());
        health.put("service", "S3 Upload Service");
        health.put("version", "1.0.0");
        
        // 检查S3连接状态
        try {
            S3UploadResponse s3Test = s3Service.testConnection();
            health.put("s3Connection", s3Test.isSuccess() ? "UP" : "DOWN");
            if (!s3Test.isSuccess()) {
                health.put("s3Error", s3Test.getMessage());
            }
        } catch (Exception e) {
            health.put("s3Connection", "DOWN");
            health.put("s3Error", e.getMessage());
        }
        
        return ResponseEntity.ok(health);
    }

    /**
     * 根据错误代码确定合适的HTTP状态码
     * 
     * @param errorCode 错误代码
     * @return HttpStatus HTTP状态码
     */
    private HttpStatus determineHttpStatus(String errorCode) {
        if (errorCode == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        
        return switch (errorCode) {
            case "CONFIGURATION_ERROR" -> HttpStatus.SERVICE_UNAVAILABLE;
            case "INVALID_FILE_CONTENT" -> HttpStatus.BAD_REQUEST;
            case "FILE_ALREADY_EXISTS" -> HttpStatus.CONFLICT;
            case "FILE_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "BUCKET_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "AccessDenied" -> HttpStatus.FORBIDDEN;
            case "InvalidBucketName" -> HttpStatus.BAD_REQUEST;
            case "NoSuchBucket" -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    /**
     * 全局异常处理器 - 处理验证错误
     * 
     * @param ex 验证异常
     * @return ResponseEntity<S3UploadResponse> 错误响应
     */
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<S3UploadResponse> handleValidationException(
            org.springframework.web.bind.MethodArgumentNotValidException ex) {
        
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .reduce((a, b) -> a + "; " + b)
            .orElse("请求参数验证失败");
        
        log.warn("请求参数验证失败: {}", errorMessage);
        
        S3UploadResponse errorResponse = S3UploadResponse.failure(
            "请求参数验证失败: " + errorMessage, 
            "VALIDATION_ERROR"
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * 全局异常处理器 - 处理其他异常
     * 
     * @param ex 异常
     * @return ResponseEntity<S3UploadResponse> 错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<S3UploadResponse> handleGenericException(Exception ex) {
        log.error("处理请求时发生未预期的异常: {}", ex.getMessage(), ex);
        
        S3UploadResponse errorResponse = S3UploadResponse.failure(
            "服务器内部错误: " + ex.getMessage(), 
            "INTERNAL_ERROR"
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
} 