-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS ai_interviewer CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE ai_interviewer;

-- 创建用户表
CREATE TABLE IF NOT EXISTS `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `email` varchar(100) NOT NULL COMMENT '邮箱',
  `password` varchar(255) NOT NULL COMMENT '密码（加密后）',
  `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
  `real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `profession` varchar(100) DEFAULT NULL COMMENT '职业',
  `experience_years` int DEFAULT 0 COMMENT '工作经验年数',
  `bio` text COMMENT '个人简介',
  `gender` tinyint DEFAULT 0 COMMENT '性别：0-未知，1-男，2-女',
  `birthday` date COMMENT '生日',
  `role` tinyint DEFAULT 0 COMMENT '角色：0-普通用户，1-管理员',
  `last_login_time` datetime COMMENT '最后登录时间',
  `last_login_ip` varchar(50) COMMENT '最后登录IP',
  `create_by` bigint COMMENT '创建人',
  `update_by` bigint COMMENT '更新人',
  `version` int DEFAULT 1 COMMENT '版本号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除：0-未删除，1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`),
  KEY `idx_phone` (`phone`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 创建面试记录表
CREATE TABLE IF NOT EXISTS `interview_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '面试记录ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `title` varchar(200) NOT NULL COMMENT '面试标题',
  `type` varchar(50) NOT NULL COMMENT '面试类型',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING-待开始，IN_PROGRESS-进行中，COMPLETED-已完成，CANCELLED-已取消',
  `score` decimal(5,2) DEFAULT NULL COMMENT '面试得分',
  `duration` int DEFAULT NULL COMMENT '面试时长（秒）',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `feedback` text COMMENT '面试反馈',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除：0-未删除，1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `fk_interview_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试记录表';

-- 创建面试问题表
CREATE TABLE IF NOT EXISTS `interview_question` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '问题ID',
  `record_id` bigint NOT NULL COMMENT '面试记录ID',
  `question` text NOT NULL COMMENT '问题内容',
  `answer` text COMMENT '用户回答',
  `score` decimal(5,2) DEFAULT NULL COMMENT '问题得分',
  `feedback` text COMMENT '问题反馈',
  `order_num` int NOT NULL COMMENT '问题顺序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_record_id` (`record_id`),
  KEY `idx_order_num` (`order_num`),
  CONSTRAINT `fk_question_record` FOREIGN KEY (`record_id`) REFERENCES `interview_record` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试问题表';
