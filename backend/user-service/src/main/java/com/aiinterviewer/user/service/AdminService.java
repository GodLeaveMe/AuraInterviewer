package com.aiinterviewer.user.service;

import com.aiinterviewer.user.dto.CreateUserRequest;
import com.aiinterviewer.user.dto.UserQueryRequest;
import com.aiinterviewer.user.entity.User;

import java.util.List;
import java.util.Map;

/**
 * 管理员服务接口
 */
public interface AdminService {
    
    /**
     * 获取用户列表（分页）
     */
    Map<String, Object> getUserList(UserQueryRequest request);
    
    /**
     * 获取用户详情
     */
    User getUserDetail(Long userId);
    
    /**
     * 创建用户
     */
    User createUser(User user, Long adminId);

    /**
     * 创建用户（使用请求对象）
     */
    User createUser(CreateUserRequest request, Long adminId);

    /**
     * 更新用户信息
     */
    User updateUser(User user, Long adminId);

    /**
     * 删除用户（逻辑删除）
     */
    boolean deleteUser(Long userId, Long adminId);

    /**
     * 恢复用户
     */
    boolean restoreUser(Long userId, Long adminId);
    
    /**
     * 切换用户状态
     */
    boolean toggleUserStatus(Long userId, Integer status, Long adminId);
    
    /**
     * 重置用户密码
     */
    void resetUserPassword(Long userId, String newPassword, Long adminId);
    
    /**
     * 获取系统统计数据
     */
    Map<String, Object> getSystemStatistics();
    
    /**
     * 批量删除用户
     */
    boolean batchDeleteUsers(Long[] userIds, Long adminId);
    
    /**
     * 批量更新用户状态
     */
    boolean batchUpdateUserStatus(Long[] userIds, Integer status, Long adminId);
    
    /**
     * 导出用户数据
     */
    byte[] exportUserData(UserQueryRequest request);

    /**
     * 获取用户注册趋势数据
     */
    List<Map<String, Object>> getUserTrendData();

    /**
     * 获取性别分布数据
     */
    List<Map<String, Object>> getGenderDistribution();

    /**
     * 获取活跃度数据
     */
    Map<String, Object> getActivityData();

    /**
     * 获取用户状态分布数据
     */
    List<Map<String, Object>> getUserStatusDistribution();

}
