# AWS S3文件上传演示项目

这是一个基于Spring Boot 3的AWS S3文件上传演示项目，展示了如何在Java应用程序中集成AWS S3服务进行文件上传、下载、删除等操作。

## 项目特性

- ✅ **Spring Boot 3** - 使用最新的Spring Boot框架
- ✅ **AWS SDK v2** - 集成AWS Java SDK v2进行S3操作
- ✅ **配置灵活** - 支持环境变量和配置文件两种配置方式
- ✅ **详细注释** - 代码包含详细的中文注释，适合学习
- ✅ **错误处理** - 完善的错误处理和异常管理
- ✅ **REST API** - 提供完整的REST API接口
- ✅ **健康检查** - 内置连接测试和健康检查功能
- ✅ **多环境支持** - 支持开发、生产等多环境配置

## 项目结构

```
src/main/java/com/example/s3upload/
├── S3UploadApplication.java          # 主启动类
├── config/
│   ├── S3Config.java                 # S3客户端配置类
│   └── S3Properties.java             # S3配置属性类
├── controller/
│   └── S3Controller.java             # REST API控制器
├── dto/
│   ├── S3UploadRequest.java          # 上传请求DTO
│   └── S3UploadResponse.java         # 响应DTO
└── service/
    └── S3Service.java                # S3业务服务类

src/main/resources/
└── application.yml                   # 应用配置文件
```

## 配置说明

### 方式1：环境变量配置（推荐用于生产环境）

```bash
export AWS_S3_ACCESS_KEY="your-access-key"
export AWS_S3_SECRET_KEY="your-secret-key" 
export AWS_S3_REGION="us-east-1"
export AWS_S3_BUCKET_NAME="your-bucket-name"

# 可选配置
export AWS_S3_ENDPOINT_URL="https://s3.amazonaws.com"  # 自定义终端点
export AWS_S3_PATH_STYLE_ACCESS="false"                # 路径样式访问
```

### 方式2：配置文件配置

编辑 `src/main/resources/application.yml`：

```yaml
aws:
  s3:
    access-key: your-access-key
    secret-key: your-secret-key
    region: us-east-1
    bucket-name: your-bucket-name
    endpoint-url: ""                    # 留空使用默认AWS S3
    path-style-access: false            # 是否使用路径样式访问
    connection-timeout: 30000           # 连接超时（毫秒）
    read-timeout: 60000                 # 读取超时（毫秒）
```

### 配置优先级

1. **环境变量**（最高优先级）
2. **application.yml配置文件**
3. **代码默认值**

## API接口

### 1. 测试S3连接

```bash
GET /api/s3/test-connection
```

**响应示例：**
```json
{
  "success": true,
  "message": "S3连接测试成功",
  "bucketName": "your-bucket-name",
  "uploadTime": "2024-01-01T12:00:00"
}
```

### 2. 上传文件

```bash
POST /api/s3/upload
Content-Type: application/json
```

**请求体：**
```json
{
  "fileContent": "base64编码的文件内容",
  "fileName": "example.jpg",
  "pathPrefix": "images/",
  "contentType": "image/jpeg",
  "overwrite": false
}
```

**响应示例：**
```json
{
  "success": true,
  "message": "文件上传成功",
  "s3Key": "images/uuid_example.jpg",
  "fileUrl": "https://bucket.s3.region.amazonaws.com/images/uuid_example.jpg",
  "fileSize": 1024,
  "contentType": "image/jpeg",
  "bucketName": "your-bucket-name",
  "uploadTime": "2024-01-01T12:00:00"
}
```

### 3. 删除文件

```bash
DELETE /api/s3/delete/{s3Key}
```

### 4. 列出文件

```bash
GET /api/s3/list?prefix=images/&maxKeys=50
```

### 5. 健康检查

```bash
GET /api/s3/health
```

## 快速开始

### 1. 克隆项目

```bash
git clone <repository-url>
cd s3upload
```

### 2. 配置AWS凭据

选择以下任一方式：

**方式A：使用环境变量**
```bash
export AWS_S3_ACCESS_KEY="your-access-key"
export AWS_S3_SECRET_KEY="your-secret-key"
export AWS_S3_REGION="us-east-1"
export AWS_S3_BUCKET_NAME="your-bucket-name"
```

**方式B：修改配置文件**
编辑 `src/main/resources/application.yml` 中的AWS配置

### 3. 构建并运行

```bash
# 使用Maven
mvn clean package
mvn spring-boot:run

# 或者直接运行JAR
java -jar target/s3-upload-demo-1.0.0-SNAPSHOT.jar
```

### 4. 测试API

访问 `http://localhost:8080/api/s3/test-connection` 测试连接

## 开发环境配置

### 使用MinIO进行本地测试

1. 启动MinIO服务器：
```bash
docker run -p 9000:9000 -p 9001:9001 \
  --name minio \
  -e "MINIO_ROOT_USER=minioadmin" \
  -e "MINIO_ROOT_PASSWORD=minioadmin" \
  minio/minio server /data --console-address ":9001"
```

2. 使用开发配置启动应用：
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## 错误处理

当配置不完整时，应用会返回详细的错误信息：

```json
{
  "success": false,
  "message": "S3配置不完整，请检查以下配置项: AWS访问密钥ID (aws.s3.access-key 或环境变量 AWS_S3_ACCESS_KEY); ",
  "errorCode": "CONFIGURATION_ERROR",
  "uploadTime": "2024-01-01T12:00:00"
}
```

## 安全最佳实践

1. **生产环境使用环境变量** - 永远不要在代码或配置文件中硬编码敏感信息
2. **使用IAM角色** - 在AWS环境中优先使用IAM角色而不是访问密钥
3. **最小权限原则** - 确保AWS用户只有必要的S3权限
4. **启用存储桶加密** - 为S3存储桶启用服务端加密
5. **配置CORS策略** - 如果需要从浏览器直接访问，配置适当的CORS策略

## 常见问题

### Q: 为什么使用Base64编码传输文件？
A: Base64编码确保二进制文件能够通过JSON安全传输，虽然会增加约33%的大小，但简化了API设计。

### Q: 如何处理大文件上传？
A: 对于大文件，建议：
1. 增加超时配置
2. 考虑使用分段上传
3. 实现上传进度跟踪

### Q: 支持哪些S3兼容存储？
A: 支持所有S3兼容的对象存储，包括：
- AWS S3
- MinIO
- 阿里云OSS（S3兼容模式）
- 腾讯云COS（S3兼容模式）

## 技术栈

- **Java 17** - 编程语言
- **Spring Boot 3.2.0** - 应用框架
- **AWS SDK for Java v2** - AWS服务集成
- **Maven** - 构建工具
- **Lombok** - 代码简化
- **Jackson** - JSON处理

## 许可证

本项目仅用于学习和演示目的。 