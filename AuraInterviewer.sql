/*
 Navicat Premium Dump SQL

 Source Server         : Hellomykimi
 Source Server Type    : MySQL
 Source Server Version : 80032 (8.0.32)
 Source Host           : localhost:3306
 Source Schema         : ai_interviewer

 Target Server Type    : MySQL
 Target Server Version : 80032 (8.0.32)
 File Encoding         : 65001

 Date: 11/06/2025 03:18:40
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ai_config
-- ----------------------------
DROP TABLE IF EXISTS `ai_config`;
CREATE TABLE `ai_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置名称',
  `provider` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '服务提供商：openai、volcengine、baidu等',
  `model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模型名称',
  `api_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'API密钥',
  `api_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'API地址',
  `max_tokens` int NULL DEFAULT 2048 COMMENT '最大token数',
  `temperature` decimal(3, 2) NULL DEFAULT 0.70 COMMENT '温度参数',
  `top_p` decimal(3, 2) NULL DEFAULT 1.00 COMMENT 'top_p参数',
  `frequency_penalty` decimal(3, 2) NULL DEFAULT 0.00 COMMENT '频率惩罚',
  `presence_penalty` decimal(3, 2) NULL DEFAULT 0.00 COMMENT '存在惩罚',
  `system_prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '系统提示词',
  `is_active` tinyint NULL DEFAULT 1 COMMENT '是否启用：0-禁用，1-启用',
  `is_public` tinyint(1) NULL DEFAULT 0 COMMENT '是否为公共配置：0-私有，1-公共（管理员创建的供所有人使用）',
  `priority` int NULL DEFAULT 0 COMMENT '优先级',
  `rate_limit` int NULL DEFAULT 60 COMMENT '速率限制（每分钟请求数）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除',
  `model_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'chat' COMMENT '模型类型：chat-文本对话，reasoning-深度思考',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_provider`(`provider` ASC) USING BTREE,
  INDEX `idx_is_active`(`is_active` ASC) USING BTREE,
  INDEX `idx_priority`(`priority` ASC) USING BTREE,
  INDEX `idx_ai_config_model_type`(`model_type` ASC) USING BTREE,
  INDEX `idx_ai_config_type_active`(`model_type` ASC, `is_active` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 100000002 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'AI配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_config
-- ----------------------------
INSERT INTO `ai_config` VALUES (100000001, '硅基流动', 'siliconflow', 'deepseek-ai/DeepSeek-V3-0324', 'sk-Du2IKlFldswsDedo5ZgNRWqHd53dftEH8kRcPdBMboGUC9CB', 'https://www.dpapi.top/v1/chat/completions', 4096, 0.70, 1.00, 0.00, 0.00, '', 1, 1, 1, 60, '2025-06-10 19:01:41', '2025-06-11 02:48:36', 100000000, 100000000, 0, 'chat');

-- ----------------------------
-- Table structure for api_config
-- ----------------------------
DROP TABLE IF EXISTS `api_config`;
CREATE TABLE `api_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '创建用户ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置名称',
  `api_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'API类型：openai、claude、gemini、qwen等',
  `api_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'API密钥',
  `base_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'API基础URL',
  `model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '模型名称',
  `max_tokens` int NULL DEFAULT 2048 COMMENT '最大Token数',
  `temperature` decimal(3, 2) NULL DEFAULT 0.70 COMMENT '温度参数',
  `system_prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '系统提示词',
  `interviewer_role` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '面试官角色描述',
  `interview_style` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '面试风格',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '配置描述',
  `enabled` tinyint NULL DEFAULT 1 COMMENT '是否启用：0-禁用，1-启用',
  `is_default` tinyint NULL DEFAULT 0 COMMENT '是否为默认配置：0-否，1-是',
  `sort` int NULL DEFAULT 0 COMMENT '排序',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除',
  `model_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'chat' COMMENT '模型类型：chat-文本对话，reasoning-深度思考',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_api_type`(`api_type` ASC) USING BTREE,
  INDEX `idx_enabled`(`enabled` ASC) USING BTREE,
  INDEX `idx_is_default`(`is_default` ASC) USING BTREE,
  INDEX `idx_api_config_model_type`(`model_type` ASC) USING BTREE,
  INDEX `idx_api_config_type_active`(`model_type` ASC, `enabled` ASC) USING BTREE,
  CONSTRAINT `api_config_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 100000002 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户API配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of api_config
-- ----------------------------
INSERT INTO `api_config` VALUES (100000001, 100000000, '硅基流动', 'openai', 'sk-ugbzeyebqajgimvymudgigklhdqljexumayiuxvpjwgmkmeh', 'https://api.siliconflow.cn/v1/chat/completions', 'deepseek-ai/DeepSeek-V3', 2048, 0.70, '66', '66', '严格型', '66', 1, 1, 1, '2025-06-08 16:47:35', '2025-06-11 02:59:43', 0, 'chat');

-- ----------------------------
-- Table structure for interview_qa
-- ----------------------------
DROP TABLE IF EXISTS `interview_qa`;
CREATE TABLE `interview_qa`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `session_id` bigint NOT NULL COMMENT '会话ID',
  `question_order` int NOT NULL COMMENT '问题序号',
  `question` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '问题内容',
  `answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '回答内容',
  `answer_type` tinyint NULL DEFAULT 1 COMMENT '回答类型：1-文本',
  `answer_time` datetime NULL DEFAULT NULL COMMENT '回答时间',
  `thinking_time` int NULL DEFAULT NULL COMMENT '思考时间（秒）',
  `score` decimal(5, 2) NULL DEFAULT NULL COMMENT '评分(0-100)',
  `ai_feedback` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'AI反馈',
  `ai_thinking` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'AI思考过程',
  `keywords` json NULL COMMENT '关键词',
  `emotion_score` decimal(5, 2) NULL DEFAULT NULL COMMENT '情感分数(0-100)',
  `confidence_score` decimal(5, 2) NULL DEFAULT NULL COMMENT '自信度分数(0-100)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除',
  `version` int NULL DEFAULT 1 COMMENT '版本号',
  `question_data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'Question structured data in JSON format',
  `answer_data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'Answer structured data in JSON format',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_session_id`(`session_id` ASC) USING BTREE,
  INDEX `idx_question_order`(`question_order` ASC) USING BTREE,
  INDEX `idx_answer_time`(`answer_time` ASC) USING BTREE,
  INDEX `idx_interview_qa_session_order`(`session_id` ASC, `question_order` ASC) USING BTREE,
  INDEX `idx_interview_qa_answer_type`(`answer_type` ASC) USING BTREE,
  CONSTRAINT `interview_qa_ibfk_1` FOREIGN KEY (`session_id`) REFERENCES `interview_session` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1932106999905448026 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '面试问答记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of interview_qa
-- ----------------------------

-- ----------------------------
-- Table structure for interview_session
-- ----------------------------
DROP TABLE IF EXISTS `interview_session`;
CREATE TABLE `interview_session`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `template_id` bigint NULL DEFAULT NULL COMMENT '模板ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '面试标题',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0-未开始，1-进行中，2-已完成，3-已取消，4-已暂停',
  `start_time` datetime NULL DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '结束时间',
  `duration` int NULL DEFAULT 0 COMMENT '实际时长（秒）',
  `question_count` int NULL DEFAULT 0 COMMENT '问题总数',
  `answered_count` int NULL DEFAULT 0 COMMENT '已回答数',
  `score` decimal(5, 2) NULL DEFAULT NULL COMMENT '总分',
  `ai_model` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '使用的AI模型',
  `interview_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'text' COMMENT '面试模式：text-文本',
  `job_position` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '职位信息',
  `settings` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '面试设置（JSON格式）',
  `summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '面试总结',
  `feedback` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '反馈建议',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除',
  `version` int NULL DEFAULT 1 COMMENT '版本号',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_template_id`(`template_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_start_time`(`start_time` ASC) USING BTREE,
  CONSTRAINT `interview_session_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `interview_session_ibfk_2` FOREIGN KEY (`template_id`) REFERENCES `interview_template` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1932106934562385969 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '面试会话表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of interview_session
-- ----------------------------

-- ----------------------------
-- Table structure for interview_template
-- ----------------------------
DROP TABLE IF EXISTS `interview_template`;
CREATE TABLE `interview_template`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '模板描述',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类：技术面试、行为面试、综合面试等',
  `difficulty` tinyint NULL DEFAULT 1 COMMENT '难度：1-初级，2-中级，3-高级',
  `duration` int NULL DEFAULT 30 COMMENT '预计时长（分钟）',
  `question_count` int NULL DEFAULT 10 COMMENT '问题数量',
  `tags` json NULL COMMENT '标签',
  `config` json NULL COMMENT '配置信息',
  `is_public` tinyint NULL DEFAULT 1 COMMENT '是否公开：0-私有，1-公开',
  `usage_count` int NULL DEFAULT 0 COMMENT '使用次数',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_category`(`category` ASC) USING BTREE,
  INDEX `idx_difficulty`(`difficulty` ASC) USING BTREE,
  INDEX `idx_is_public`(`is_public` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 100000005 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '面试模板表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of interview_template
-- ----------------------------
INSERT INTO `interview_template` VALUES (100000001, '666', '6', '行为面试', 3, 30, 2, '[\"Python\", \"JavaScript\", \"Java\", \"Vue\"]', '{}', 1, 0, '2025-06-08 18:27:06', '2025-06-10 05:20:14', 100000000, 100000000, 0);
INSERT INTO `interview_template` VALUES (100000002, '55555', '555', '行为面试', 3, 30, 2, '[\"Python\", \"JavaScript\", \"Java\", \"Vue\"]', '{}', 1, 0, '2025-06-09 01:00:01', '2025-06-10 05:20:13', 100000000, 100000000, 0);
INSERT INTO `interview_template` VALUES (100000003, '7777777', '7', '行为面试', 3, 10, 2, '[\"Python\", \"JavaScript\", \"Java\", \"Vue\"]', '{}', 1, 0, '2025-06-09 03:04:20', '2025-06-10 05:20:13', 100000000, 100000000, 0);
INSERT INTO `interview_template` VALUES (100000004, '88888', '8', '专业面试', 2, 10, 2, '[\"Python\", \"JavaScript\", \"Java\", \"Vue\", \"React\", \"Spring Boot\"]', '{}', 1, 0, '2025-06-10 00:51:13', '2025-06-10 06:05:43', 100000000, 100000000, 0);

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '邮箱',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '昵称',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '真实姓名',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '头像URL',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '手机号',
  `gender` tinyint NULL DEFAULT 0 COMMENT '性别：0-未知，1-男，2-女',
  `birthday` date NULL DEFAULT NULL COMMENT '生日',
  `profession` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '职业',
  `experience_years` int NULL DEFAULT 0 COMMENT '工作经验年数',
  `bio` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '个人简介',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
  `role` tinyint NULL DEFAULT 0 COMMENT '角色：0-普通用户，1-管理员',
  `last_login_time` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '最后登录IP',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除',
  `version` int NULL DEFAULT 1 COMMENT '版本号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE,
  UNIQUE INDEX `uk_email`(`email` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 100000005 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES (100000000, 'kimi', 'admin@aiinterviewer.com', '6eb43a42e5afc791f5e931b7618a3e37', '管理员', '管理员', '', '12345612345', 0, '2025-06-08', '', 10, '我是管理员', 1, 1, '2025-06-11 02:05:49', '0:0:0:0:0:0:0:1', '2025-06-08 07:09:04', '2025-06-11 02:05:49', NULL, 100000000, 0, 1);
INSERT INTO `user` VALUES (100000001, 'user', 'user@user.com', '6eb43a42e5afc791f5e931b7618a3e37', 'user', 'user', NULL, '', 0, NULL, NULL, 5, '', 1, 0, '2025-06-09 01:01:14', '0:0:0:0:0:0:0:1', '2025-06-08 16:36:45', '2025-06-10 04:49:00', 100000000, 100000000, 0, NULL);
INSERT INTO `user` VALUES (100000002, 'user1', 'user1@user.com', '6eb43a42e5afc791f5e931b7618a3e37', 'user1', 'user1', NULL, '', 0, NULL, NULL, 0, '', 1, 0, NULL, NULL, '2025-06-08 16:43:32', '2025-06-10 04:49:00', 100000000, 100000000, 0, NULL);
INSERT INTO `user` VALUES (100000003, 'user2', 'user2@user.com', '6eb43a42e5afc791f5e931b7618a3e37', 'user2', 'user2', NULL, '', 1, NULL, NULL, 0, NULL, 1, 0, NULL, NULL, '2025-06-08 16:56:16', '2025-06-10 04:49:00', NULL, 100000000, 0, NULL);
INSERT INTO `user` VALUES (100000004, 'user3', 'user3@user.com', '6eb43a42e5afc791f5e931b7618a3e37', 'user3', 'user3', NULL, '', 2, '2025-06-10', '', 2, '', 1, 0, NULL, NULL, '2025-06-08 17:34:47', '2025-06-10 18:24:11', NULL, 100000000, 0, NULL);

SET FOREIGN_KEY_CHECKS = 1;
