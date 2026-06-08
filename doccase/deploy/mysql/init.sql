-- DocCase Database Initialization

-- Nacos config database
CREATE DATABASE IF NOT EXISTS `nacos_config` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON `nacos_config`.* TO 'nacos'@'%' IDENTIFIED BY 'nacos123';

-- User service database
CREATE DATABASE IF NOT EXISTS `doccase_user` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Auth service database
CREATE DATABASE IF NOT EXISTS `doccase_auth` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Document service database
CREATE DATABASE IF NOT EXISTS `doccase_document` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Tag service database
CREATE DATABASE IF NOT EXISTS `doccase_tag` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- OCR service database
CREATE DATABASE IF NOT EXISTS `doccase_ocr` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

GRANT ALL PRIVILEGES ON `doccase_user`.* TO 'doccase'@'%';
GRANT ALL PRIVILEGES ON `doccase_auth`.* TO 'doccase'@'%';
GRANT ALL PRIVILEGES ON `doccase_document`.* TO 'doccase'@'%';
GRANT ALL PRIVILEGES ON `doccase_tag`.* TO 'doccase'@'%';
GRANT ALL PRIVILEGES ON `doccase_ocr`.* TO 'doccase'@'%';
FLUSH PRIVILEGES;

-- ============ USER SERVICE TABLES ============
USE `doccase_user`;

CREATE TABLE `dc_user` (
    `id` BIGINT NOT NULL COMMENT 'Snowflake ID',
    `username` VARCHAR(64) NOT NULL,
    `email` VARCHAR(128) NOT NULL,
    `password_hash` VARCHAR(256) NOT NULL,
    `avatar_url` VARCHAR(512) DEFAULT NULL,
    `phone` VARCHAR(20) DEFAULT NULL,
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0=disabled, 1=active, 2=locked',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    `deleted_at` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `dc_role` (
    `id` BIGINT NOT NULL,
    `name` VARCHAR(64) NOT NULL,
    `code` VARCHAR(64) NOT NULL COMMENT 'ADMIN, EDITOR, VIEWER',
    `description` VARCHAR(256) DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    `deleted_at` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `dc_user_role` (
    `id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `role_id` BIGINT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert default roles
INSERT INTO `dc_role` (`id`, `name`, `code`, `description`) VALUES
(1, '系统管理员', 'ADMIN', '系统管理员，拥有所有权限'),
(2, '编辑者', 'EDITOR', '可以创建和编辑文档'),
(3, '查看者', 'VIEWER', '只能查看文档');

-- ============ AUTH SERVICE TABLES ============
USE `doccase_auth`;

CREATE TABLE `dc_oauth_binding` (
    `id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `provider` VARCHAR(32) NOT NULL COMMENT 'github, google, wechat',
    `provider_user_id` VARCHAR(128) NOT NULL,
    `access_token` VARCHAR(512) DEFAULT NULL,
    `refresh_token` VARCHAR(512) DEFAULT NULL,
    `token_expires_at` DATETIME DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_provider_user` (`provider`, `provider_user_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `dc_mfa_secret` (
    `id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `secret_key` VARCHAR(128) NOT NULL COMMENT 'TOTP secret (encrypted)',
    `is_enabled` TINYINT NOT NULL DEFAULT 0,
    `backup_codes` JSON DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `dc_audit_log` (
    `id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `action` VARCHAR(64) NOT NULL COMMENT 'LOGIN, LOGOUT, CREATE_DOC, DELETE_DOC, etc.',
    `resource_type` VARCHAR(64) DEFAULT NULL,
    `resource_id` BIGINT DEFAULT NULL,
    `detail` JSON DEFAULT NULL,
    `ip_address` VARCHAR(45) DEFAULT NULL,
    `user_agent` VARCHAR(512) DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_action` (`action`),
    KEY `idx_resource` (`resource_type`, `resource_id`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `dc_abac_policy` (
    `id` BIGINT NOT NULL,
    `name` VARCHAR(128) NOT NULL,
    `description` VARCHAR(512) DEFAULT NULL,
    `effect` VARCHAR(8) NOT NULL COMMENT 'ALLOW or DENY',
    `subject_condition` JSON NOT NULL,
    `resource_condition` JSON NOT NULL,
    `action_condition` JSON NOT NULL,
    `environment_condition` JSON DEFAULT NULL,
    `priority` INT NOT NULL DEFAULT 0,
    `is_enabled` TINYINT NOT NULL DEFAULT 1,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_enabled_priority` (`is_enabled`, `priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============ DOCUMENT SERVICE TABLES ============
USE `doccase_document`;

CREATE TABLE `dc_document` (
    `id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `title` VARCHAR(256) NOT NULL,
    `description` TEXT DEFAULT NULL,
    `file_name` VARCHAR(256) NOT NULL,
    `file_size` BIGINT NOT NULL,
    `file_type` VARCHAR(32) NOT NULL,
    `mime_type` VARCHAR(128) NOT NULL,
    `file_hash` VARCHAR(128) NOT NULL COMMENT 'SHA-256',
    `storage_type` VARCHAR(16) NOT NULL DEFAULT 'minio',
    `storage_path` VARCHAR(512) NOT NULL,
    `thumbnail_path` VARCHAR(512) DEFAULT NULL,
    `current_version` INT NOT NULL DEFAULT 1,
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0=processing, 1=active, 2=archived',
    `tag_ids` JSON DEFAULT NULL,
    `metadata` JSON DEFAULT NULL,
    `ocr_status` TINYINT NOT NULL DEFAULT 0 COMMENT '0=none, 1=pending, 2=completed, 3=failed',
    `ocr_text` MEDIUMTEXT DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    `deleted_at` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_file_hash` (`file_hash`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_file_type` (`file_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `dc_document_version` (
    `id` BIGINT NOT NULL,
    `document_id` BIGINT NOT NULL,
    `version_number` INT NOT NULL,
    `file_hash` VARCHAR(128) NOT NULL,
    `storage_path` VARCHAR(512) NOT NULL,
    `file_size` BIGINT NOT NULL,
    `change_note` VARCHAR(512) DEFAULT NULL,
    `created_by` BIGINT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_doc_version` (`document_id`, `version_number`),
    KEY `idx_document_id` (`document_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `dc_chunk_upload` (
    `id` BIGINT NOT NULL,
    `upload_id` VARCHAR(64) NOT NULL,
    `user_id` BIGINT NOT NULL,
    `file_name` VARCHAR(256) NOT NULL,
    `file_hash` VARCHAR(128) DEFAULT NULL,
    `total_size` BIGINT NOT NULL,
    `chunk_size` INT NOT NULL,
    `total_chunks` INT NOT NULL,
    `uploaded_chunks` JSON NOT NULL DEFAULT ('[]'),
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0=uploading, 1=merging, 2=completed, 3=expired',
    `expire_at` DATETIME NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_upload_id` (`upload_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status_expire` (`status`, `expire_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============ TAG SERVICE TABLES ============
USE `doccase_tag`;

CREATE TABLE `dc_tag` (
    `id` BIGINT NOT NULL,
    `name` VARCHAR(128) NOT NULL,
    `parent_id` BIGINT DEFAULT NULL,
    `path` VARCHAR(1024) NOT NULL COMMENT 'Materialized path: /1/5/12/',
    `level` INT NOT NULL DEFAULT 0,
    `sort_order` INT NOT NULL DEFAULT 0,
    `color` VARCHAR(16) DEFAULT NULL,
    `icon` VARCHAR(64) DEFAULT NULL,
    `document_count` INT NOT NULL DEFAULT 0,
    `created_by` BIGINT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    `deleted_at` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_path` (`path`(255)),
    KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `dc_document_tag` (
    `id` BIGINT NOT NULL,
    `document_id` BIGINT NOT NULL,
    `tag_id` BIGINT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_doc_tag` (`document_id`, `tag_id`),
    KEY `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============ OCR SERVICE TABLES ============
USE `doccase_ocr`;

CREATE TABLE `dc_ocr_task` (
    `id` BIGINT NOT NULL,
    `document_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `engine` VARCHAR(32) DEFAULT NULL COMMENT 'tesseract, paddleocr, auto',
    `source_path` VARCHAR(512) NOT NULL,
    `file_type` VARCHAR(16) NOT NULL,
    `language` VARCHAR(32) DEFAULT 'eng',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0=pending,1=preprocessing,2=recognizing,3=completed,4=failed',
    `retry_count` INT NOT NULL DEFAULT 0,
    `max_retries` INT NOT NULL DEFAULT 3,
    `error_message` TEXT DEFAULT NULL,
    `started_at` DATETIME DEFAULT NULL,
    `completed_at` DATETIME DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_document_id` (`document_id`),
    KEY `idx_status` (`status`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `dc_ocr_result` (
    `id` BIGINT NOT NULL,
    `task_id` BIGINT NOT NULL,
    `document_id` BIGINT NOT NULL,
    `engine_used` VARCHAR(32) NOT NULL,
    `full_text` MEDIUMTEXT DEFAULT NULL,
    `confidence` DECIMAL(5,4) DEFAULT NULL,
    `page_results` JSON DEFAULT NULL,
    `structured_data` JSON DEFAULT NULL,
    `processing_time_ms` INT DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_id` (`task_id`),
    KEY `idx_document_id` (`document_id`),
    KEY `idx_confidence` (`confidence`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
