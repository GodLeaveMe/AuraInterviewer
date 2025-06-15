-- 为api_config表添加model_type字段
ALTER TABLE api_config ADD COLUMN model_type VARCHAR(20) NOT NULL DEFAULT 'chat' COMMENT '模型类型：chat-文本对话，tts-语音合成，reasoning-深度思考，stt-语音识别';

-- 创建索引以提高查询性能
CREATE INDEX idx_api_config_model_type ON api_config(model_type);
CREATE INDEX idx_api_config_type_active ON api_config(model_type, is_active);

-- 更新现有记录的model_type为chat（如果还没有设置的话）
UPDATE api_config SET model_type = 'chat' WHERE model_type IS NULL OR model_type = '';
