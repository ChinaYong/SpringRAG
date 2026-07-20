SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id` INT AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(20) NOT NULL UNIQUE COMMENT '用户登录账号',
    `password` VARCHAR(255) NOT NULL COMMENT '密码的哈希值',
    `create_time` DATETIME COMMENT '创建时间',
    `update_time` DATETIME COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

DROP TABLE IF EXISTS `agent`;
CREATE TABLE `agent` (
    `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '智能体ID',
    `user_id` INT NOT NULL COMMENT '所属用户ID',
    `name` VARCHAR(20) NOT NULL COMMENT '智能体名称',
    `description` TEXT COMMENT '智能体描述',
    `system_prompt` TEXT COMMENT '系统提示词',
    `model_id` INT COMMENT '默认模型ID',
    `model_parameters` JSON COMMENT 'LLM模型参数',
    `file_ids` JSON COMMENT '绑定的文件ID集合',
    `create_time` DATETIME COMMENT '创建时间',
    `update_time` DATETIME COMMENT '更新时间',
    UNIQUE KEY `uk_user_agent_name` (`user_id`, `name`) -- 保证同一个用户下的智能体不重名
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='智能体表';

DROP TABLE IF EXISTS `session`;
CREATE TABLE `session` (
    `id` INT AUTO_INCREMENT COMMENT '会话ID',
    `user_id` INT NOT NULL COMMENT '会话所属用户ID',
    `agent_id` INT NOT NULL COMMENT '会话所属智能体ID',
    `name` VARCHAR(20) NOT NULL COMMENT '会话名称',
    `create_time` DATETIME COMMENT '创建时间',
    `update_time` DATETIME COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会话管理表';

DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message` (
    `session_id` INT NOT NULL COMMENT '会话ID',
    `role` ENUM('user', 'assistant') NOT NULL COMMENT '消息角色',
    `content` TEXT NOT NULL COMMENT '对话内容',
    `tokens` INT COMMENT 'Token数量',
    `order` INT NOT NULL COMMENT '对话序号',
    `file_ids` JSON COMMENT '用户上传的附件',
    `create_time` DATETIME COMMENT '创建时间',
    `update_time` DATETIME COMMENT '更新时间',
    PRIMARY KEY (`session_id`, `order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='对话消息表';

DROP TABLE IF EXISTS `user_file_info`;
DROP TABLE IF EXISTS `file_storage`;
-- 清理旧重构遗留的单表（重构前用单表 file_info 存储文件）
DROP TABLE IF EXISTS `file_info`;

CREATE TABLE `file_storage` (
    `id` INT AUTO_INCREMENT COMMENT '文件存储ID',
    `sha256` VARCHAR(64) NOT NULL COMMENT '文件 SHA-256',
    `storage_path` VARCHAR(255) NOT NULL COMMENT '基于 SHA-256 的存储路径',
    `size` BIGINT NOT NULL COMMENT '文件大小（字节）',
    `extension` VARCHAR(10) NOT NULL COMMENT '文件真实扩展名',
    `ref_count` INT NOT NULL DEFAULT 1 COMMENT '文件引用计数（同一文件被多次引用时累加）',
    `vectorized_status` TINYINT NOT NULL DEFAULT 0 COMMENT '向量化状态（0:待解析 1:解析中 2:已完成 3:失败）',
    `create_time` DATETIME COMMENT '创建时间',
    `update_time` DATETIME COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sha256` (`sha256`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件存储表';

CREATE TABLE `user_file_info` (
    `id` INT AUTO_INCREMENT COMMENT '文件信息ID',
    `user_id` INT NOT NULL COMMENT '文件所属用户ID',
    `name` VARCHAR(255) NOT NULL COMMENT '文件原始名称',
    `file_id` INT NOT NULL COMMENT '文件存储表中的ID（参照 file_storage.id）',
    `create_time` DATETIME COMMENT '创建时间',
    `update_time` DATETIME COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_file_id` (`file_id`),
    Unique Key 'uk_user_file' (user_id, file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户文件信息表';

DROP TABLE IF EXISTS `model_provider`;
CREATE TABLE `model_provider` (
    `id` INT AUTO_INCREMENT COMMENT '模型提供商ID',
    `user_id` INT NOT NULL COMMENT '模型提供商所属用户ID',
    `name` VARCHAR(20) NOT NULL COMMENT '模型提供商名称',
    `api_key` VARCHAR(255) NOT NULL COMMENT '模型提供商API密钥',
    `api_base_url` VARCHAR(255) NOT NULL COMMENT '模型提供商API基础URL',
    `model_list` JSON NOT NULL COMMENT '模型列表',
    `create_time` DATETIME COMMENT '创建时间',
    `update_time` DATETIME COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='模型提供商表';

SET FOREIGN_KEY_CHECKS = 1;
