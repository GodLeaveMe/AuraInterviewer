# AI面试官系统

一个基于AI的智能面试练习平台，帮助求职者提升面试技能和表现。系统采用微服务架构，集成多种AI模型，提供个性化的面试体验和专业的评估反馈。

## 🌟 项目特色

- 🤖 **多AI模型集成**: 集成OpenAI GPT、DeepSeek、SiliconFlow等多种AI服务商
- 📝 **智能评估算法**: 从技术准确性、表达清晰度、情感状态等多维度评分
- 🎯 **完整微服务架构**: 基于Spring Cloud的服务注册、API网关、独立服务模块
- 📊 **专业面试报告**: 自动生成包含分数、反馈和改进建议的详细报告
- 🎨 **自定义模板系统**: 支持创建、管理和分享个性化面试模板
- 👥 **企业级安全认证**: JWT无状态认证、RBAC权限模型、多层安全防护
- 📈 **全栈技术实现**: 后端微服务架构 + 前端现代化UI设计 + AI智能集成

## 🏗️ 技术架构

### 后端技术栈
- **框架**: Spring Boot 3.2.0 + Spring Cloud
- **服务注册**: Eureka Server
- **API网关**: Spring Cloud Gateway
- **数据库**: MySQL 8.0
- **ORM**: MyBatis Plus
- **缓存**: Redis
- **构建工具**: Maven

### 前端技术栈
- **框架**: Vue 3 + TypeScript
- **UI组件**: Element Plus
- **状态管理**: Pinia
- **路由**: Vue Router 4
- **构建工具**: Vite
- **样式**: SCSS

### 微服务架构
```
├── eureka-server (8761)     # 服务注册中心
├── gateway (8080)           # API网关
├── user-service (8081)      # 用户服务
├── interview-service (8082) # 面试服务
└── ai-service (8083)        # AI服务
```

## 🚀 快速开始

### 环境要求
- Java 18+
- Node.js 18+
- MySQL 8.0+
- Redis 6.0+
- Maven 3.6+

### 数据库配置
1. 创建数据库：
```sql
CREATE DATABASE ai_interviewer CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 导入数据库脚本：
```bash
mysql -u root -p ai_interviewer < database/init.sql
```

### 后端启动

1. **启动服务注册中心**
```bash
cd backend/eureka-server
mvn spring-boot:run
```

2. **启动API网关**
```bash
cd backend/gateway
mvn spring-boot:run
```

3. **启动用户服务**
```bash
cd backend/user-service
mvn spring-boot:run
```

4. **启动面试服务**
```bash
cd backend/interview-service
mvn spring-boot:run
```

5. **启动AI服务**
```bash
cd backend/ai-service
mvn spring-boot:run
```

### 前端启动

```bash
cd frontend
npm install
npm run dev
```

### 访问地址
- **前端应用**: http://localhost:3000
- **API网关**: http://localhost:8080
- **服务注册中心**: http://localhost:8761

## 👤 默认账户

- **管理员账户**: 
  - 用户名: `kimi`
  - 密码: `kimikimi`

## 🎯 主要功能

### 面试功能
- **创建面试**: 选择模板和AI模型创建面试会话
- **实时对话**: 与AI进行文本面试对话
- **智能评估**: 多维度评估回答质量、表达清晰度和情感状态
- **暂停/恢复**: 支持面试过程中的暂停和恢复
- **面试报告**: 生成详细的面试分析报告和改进建议

### 模板管理
- **公共模板**: 系统内置的面试模板
- **自定义模板**: 用户可创建个人面试模板
- **模板分类**: 按技术栈、难度等分类管理
- **模板分享**: 支持模板的公开和私有设置

### AI配置
- **多模型支持**: 集成OpenAI、DeepSeek、SiliconFlow等多种AI服务商
- **系统配置**: 管理员可配置系统AI模型
- **用户配置**: 用户可添加自定义AI API配置
- **模型切换**: 面试时可选择不同的AI模型和评估算法

### 用户管理
- **用户注册**: 支持新用户注册
- **权限控制**: 基于RBAC权限模型，管理员和普通用户权限分离
- **个人资料**: 用户信息管理和密码修改
- **面试历史**: 查看历史面试记录和表现统计

## 📁 项目结构

```
AI面试官/
├── backend/                 # 后端服务
│   ├── eureka-server/      # 服务注册中心
│   ├── gateway/            # API网关
│   ├── user-service/       # 用户服务
│   ├── interview-service/  # 面试服务
│   └── ai-service/         # AI服务
├── frontend/               # 前端应用
│   ├── src/
│   │   ├── components/     # 公共组件
│   │   ├── views/          # 页面组件
│   │   ├── stores/         # 状态管理
│   │   ├── api/            # API接口
│   │   └── router/         # 路由配置
│   └── public/             # 静态资源
├── database/               # 数据库脚本
└── README.md              # 项目说明
```

## 🔧 配置说明

### 数据库配置
- 用户名: `root`
- 密码: `123456`
- 数据库: `ai_interviewer`

### AI模型配置
系统支持以下AI服务商：
- OpenAI
- DeepSeek
- SiliconFlow
- 自定义API

## 🔒 安全特性

- **JWT Token认证**: 实现Token自动刷新机制，提升用户体验
- **RBAC权限模型**: 细粒度的角色权限控制，确保数据安全
- **密码安全策略**: 采用MD5加密存储用户密码，防止明文泄露
- **跨域安全配置**: 配置CORS策略，防止恶意跨域请求
- **API访问限制**: 实现接口访问频率限制，防止恶意攻击
- **数据权限隔离**: 确保用户只能访问自己的面试数据

## 📊 性能优化

- **Redis缓存**: 提升系统响应速度，优化数据库查询性能
- **数据库优化**: 优化索引和查询性能，支持高并发访问
- **前端优化**: 实现接口防抖、懒加载等前端性能优化策略
- **微服务解耦**: 服务独立部署，提高系统可用性和扩展性

## 🚀 部署指南

### Docker部署
```bash
# 构建镜像
docker-compose build

# 启动服务
docker-compose up -d
```

### 生产环境配置
- 修改数据库连接配置
- 配置Redis集群
- 设置环境变量
- 配置反向代理

## 🤝 贡献指南

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 🔍 API测试

项目提供了完整的API测试页面，访问 `frontend/api-test.html` 可以测试所有服务接口。

### 测试功能
- 服务健康检查
- 用户认证测试
- 面试功能测试
- AI服务测试
- 管理功能测试

## 🐛 常见问题

### 1. 服务启动失败
- 检查端口是否被占用
- 确认数据库连接配置
- 查看服务注册中心状态

### 2. AI服务调用失败
- 检查AI API配置是否正确
- 确认API密钥有效性
- 查看网络连接状态

### 3. 前端编译错误
- 清理node_modules重新安装
- 检查Node.js版本是否符合要求
- 确认TypeScript配置正确

## ✨ 项目亮点

### 1. 100% 独立开发
涵盖完整系统生命周期，从需求分析、架构设计到开发部署全流程独立完成

### 2. 微服务架构实践
采用Spring Cloud微服务架构，实现服务解耦、独立部署、高可用性

### 3. 多AI模型集成
支持多种AI服务商，实现模型动态切换和智能评估算法

### 4. 全栈技术能力
- **后端**: 微服务架构设计 + Spring Boot开发 + 数据库优化
- **前端**: Vue 3 + TypeScript + 现代化UI设计
- **AI集成**: 多模型对接 + 智能评估算法 + 结构化数据处理

### 5. 企业级安全认证
- **JWT无状态认证**: 实现Token自动刷新机制，提升用户体验
- **RBAC权限模型**: 细粒度的角色权限控制，确保数据安全
- **多层安全防护**: API限流 + 密码加密 + 跨域防护的完整安全体系

## 📚 技术成果

- **代码量**: 前后端总计约15,000+行代码
- **数据库表**: 设计5个核心业务表，支持复杂业务场景
- **API接口**: 开发50+个RESTful API接口
- **前端页面**: 实现20+个功能页面，用户体验优秀
- **AI集成**: 对接4+种AI服务商，支持多模型切换
