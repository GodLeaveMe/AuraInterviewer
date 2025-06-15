package com.aiinterviewer.user.mapper;

import com.aiinterviewer.user.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户Mapper接口
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    
    /**
     * 根据用户名查询用户
     */
    User findByUsername(@Param("username") String username);
    
    /**
     * 根据邮箱查询用户
     */
    User findByEmail(@Param("email") String email);
    
    /**
     * 根据用户名或邮箱查询用户
     */
    User findByUsernameOrEmail(@Param("username") String username, @Param("email") String email);
    
    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(@Param("username") String username);
    
    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(@Param("email") String email);
    
    /**
     * 更新最后登录信息
     */
    int updateLastLoginInfo(@Param("userId") Long userId, @Param("loginTime") String loginTime, @Param("loginIp") String loginIp);
}
