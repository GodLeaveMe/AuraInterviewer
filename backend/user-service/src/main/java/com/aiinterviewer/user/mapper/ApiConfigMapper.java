package com.aiinterviewer.user.mapper;

import com.aiinterviewer.user.entity.ApiConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * API配置Mapper接口
 */
@Mapper
public interface ApiConfigMapper extends BaseMapper<ApiConfig> {
    
    /**
     * 获取用户的最大排序值
     */
    @Select("SELECT MAX(sort) FROM api_config WHERE user_id = #{userId} AND deleted = 0")
    Integer getMaxSortByUserId(@Param("userId") Long userId);
    
    /**
     * 获取用户的默认配置
     */
    @Select("SELECT * FROM api_config WHERE user_id = #{userId} AND is_default = 1 AND deleted = 0 LIMIT 1")
    ApiConfig getDefaultConfigByUserId(@Param("userId") Long userId);
    
    /**
     * 统计用户的配置数量
     */
    @Select("SELECT COUNT(*) FROM api_config WHERE user_id = #{userId} AND deleted = 0")
    Integer countByUserId(@Param("userId") Long userId);
    
    /**
     * 统计用户启用的配置数量
     */
    @Select("SELECT COUNT(*) FROM api_config WHERE user_id = #{userId} AND enabled = 1 AND deleted = 0")
    Integer countEnabledByUserId(@Param("userId") Long userId);
}
