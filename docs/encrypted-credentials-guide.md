# 自定义加密凭据使用指南

## 📋 概述

你们公司使用了一个自定义的AWS凭据加密管理方案，将敏感的AWS访问密钥进行AES加密存储，而不是明文保存。这是一个很好的安全实践，可以有效防止凭据泄露。

## 🔧 文件结构

你的Windows用户目录的`.aws`文件夹中应该包含以下加密文件：

```
C:\Users\{你的用户名}\.aws\
├── ad.aes     # 加密的AWS凭据文件
├── aes.iv     # AES加密的初始化向量
├── aes.key    # AES加密密钥
└── aes.salt   # 加密盐值
```

## 🔐 工作原理

### 1. 标准AWS SDK凭据 vs 你们的方案

**标准方案（明文存储）：**
```
~/.aws/credentials:
[default]
aws_access_key_id = AKIA...
aws_secret_access_key = xxx...
```

**你们的方案（加密存储）：**
- `ad.aes`: 包含加密的AWS访问密钥和秘密密钥
- `aes.iv`: AES算法的初始化向量
- `aes.key`: 用于解密的AES密钥
- `aes.salt`: 加密过程中使用的盐值

### 2. 我们项目中的支持

我已经在项目中创建了 `CustomAwsCredentialsProvider` 类来支持你们的加密凭据方案。这个类会：

1. **自动检测** 你的`.aws`目录中的加密文件
2. **读取加密材料** (密钥、IV、盐值)
3. **解密凭据文件** 使用AES算法
4. **解析凭据内容** 支持多种格式
5. **缓存凭据** 提高性能(5分钟缓存)

## 🚀 使用方法

### 1. 确保文件存在

确认你的 `C:\Users\{用户名}\.aws\` 目录包含所有必需文件：
```cmd
dir C:\Users\%USERNAME%\.aws\
```

应该能看到：`ad.aes`, `aes.iv`, `aes.key`, `aes.salt`

### 2. 运行项目

项目会按照以下优先级尝试获取凭据：

1. **自定义加密凭据** (你们的方案) - **最高优先级**
2. **环境变量** (`AWS_S3_ACCESS_KEY`, `AWS_S3_SECRET_KEY`)
3. **配置文件** (`application.yml`)
4. **AWS默认凭据链** (标准 `~/.aws/credentials`)

### 3. 验证凭据

启动应用后，检查日志输出：

**成功的情况：**
```
INFO  - 已添加自定义加密凭据提供器到凭据链
INFO  - AWS凭据链创建成功，凭据验证通过
INFO  - AWS S3客户端初始化成功!
```

**失败的情况：**
```
WARN  - 自定义加密凭据提供器初始化失败，将跳过: xxx
ERROR - AWS凭据链验证失败: xxx
```

## 🛠️ 故障排除

### 问题1：加密文件不存在
```
WARN - 必需的凭据文件不存在: C:\Users\xxx\.aws\ad.aes
```

**解决方案：**
- 确保你运行了公司的凭据生成工具
- 检查文件路径是否正确
- 确认所有4个文件都存在

### 问题2：解密失败
```
ERROR - 解密AWS凭据失败: xxx
```

**可能原因：**
- 加密文件损坏
- 密钥文件不匹配
- 加密算法参数不正确

**解决方案：**
- 重新运行凭据生成工具
- 检查文件是否被意外修改

### 问题3：凭据格式不支持
```
ERROR - 凭据解析失败: 不支持的凭据格式
```

**解决方案：**
- 查看解密后的凭据内容格式
- 联系我调整 `parseCredentials` 方法

## 🔧 自定义配置

### 支持的凭据格式

我们的解析器支持以下格式：

**JSON格式：**
```json
{
  "accessKeyId": "AKIA...",
  "secretAccessKey": "xxx..."
}
```

**键值对格式：**
```
aws_access_key_id=AKIA...
aws_secret_access_key=xxx...
```

**CSV格式：**
```
AKIA...,xxx...
```

### 调整解密算法

如果你们的加密方案有特殊参数，可能需要修改 `CustomAwsCredentialsProvider` 中的 `decryptCredentials` 方法。

常见的调整点：
- 加密模式 (CBC/GCM/ECB)
- 填充方式 (PKCS5Padding/NoPadding)
- 密钥长度 (128/192/256位)

## 📝 最佳实践

### 1. 安全建议
- 定期更新加密密钥
- 不要将加密文件提交到版本控制
- 确保只有授权用户能访问`.aws`目录

### 2. 备份建议
- 备份加密文件到安全位置
- 记录密钥恢复流程
- 建立密钥轮换计划

### 3. 监控建议
- 监控凭据使用情况
- 设置凭据过期提醒
- 记录凭据访问日志

## 🆘 需要帮助？

如果遇到问题，请提供以下信息：

1. **错误日志** - 应用启动时的完整错误信息
2. **文件检查** - 确认4个加密文件都存在
3. **凭据格式** - 解密后的凭据内容格式（脱敏处理）
4. **环境信息** - 操作系统、Java版本等

我可以根据你们的具体加密方案调整代码。

## 📚 相关资料

- [AWS SDK凭据提供器文档](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html)
- [Java AES加密解密](https://docs.oracle.com/javase/8/docs/technotes/guides/security/crypto/CryptoSpec.html)
- [Spring Boot配置外部化](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config) 