package com.aiinterviewer.user.service.impl;

import com.aiinterviewer.user.dto.CreateUserRequest;
import com.aiinterviewer.user.dto.UserQueryRequest;
import com.aiinterviewer.user.entity.User;
import com.aiinterviewer.user.mapper.UserMapper;
import com.aiinterviewer.user.service.AdminService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 管理员服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserMapper userMapper;
    private final RestTemplate restTemplate;
    
    @Override
    public Map<String, Object> getUserList(UserQueryRequest request) {
        Page<User> page = new Page<>(request.getPage(), request.getSize());
        
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getDeleted, 0);
        
        // 关键词搜索
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.and(w -> w.like(User::getUsername, request.getKeyword())
                           .or().like(User::getEmail, request.getKeyword())
                           .or().like(User::getNickname, request.getKeyword()));
        }
        
        // 状态筛选
        if (request.getStatus() != null) {
            wrapper.eq(User::getStatus, request.getStatus());
        }
        
        // 性别筛选
        if (request.getGender() != null) {
            wrapper.eq(User::getGender, request.getGender());
        }
        
        // 职业筛选
        if (StringUtils.hasText(request.getProfession())) {
            wrapper.like(User::getProfession, request.getProfession());
        }
        
        // 工作经验筛选
        if (request.getMinExperienceYears() != null) {
            wrapper.ge(User::getExperienceYears, request.getMinExperienceYears());
        }
        if (request.getMaxExperienceYears() != null) {
            wrapper.le(User::getExperienceYears, request.getMaxExperienceYears());
        }
        
        // 时间范围筛选
        if (StringUtils.hasText(request.getStartDate())) {
            wrapper.ge(User::getCreateTime, request.getStartDate() + " 00:00:00");
        }
        if (StringUtils.hasText(request.getEndDate())) {
            wrapper.le(User::getCreateTime, request.getEndDate() + " 23:59:59");
        }
        
        // 排序
        if ("asc".equalsIgnoreCase(request.getSortOrder())) {
            switch (request.getSortField()) {
                case "username":
                    wrapper.orderByAsc(User::getUsername);
                    break;
                case "email":
                    wrapper.orderByAsc(User::getEmail);
                    break;
                case "createTime":
                    wrapper.orderByAsc(User::getCreateTime);
                    break;
                case "lastLoginTime":
                    wrapper.orderByAsc(User::getLastLoginTime);
                    break;
                default:
                    wrapper.orderByAsc(User::getCreateTime);
            }
        } else {
            switch (request.getSortField()) {
                case "username":
                    wrapper.orderByDesc(User::getUsername);
                    break;
                case "email":
                    wrapper.orderByDesc(User::getEmail);
                    break;
                case "createTime":
                    wrapper.orderByDesc(User::getCreateTime);
                    break;
                case "lastLoginTime":
                    wrapper.orderByDesc(User::getLastLoginTime);
                    break;
                default:
                    wrapper.orderByDesc(User::getCreateTime);
            }
        }
        
        IPage<User> result = userMapper.selectPage(page, wrapper);
        
        Map<String, Object> response = new HashMap<>();
        response.put("records", result.getRecords());
        response.put("total", result.getTotal());
        response.put("current", result.getCurrent());
        response.put("size", result.getSize());
        response.put("pages", result.getPages());
        
        return response;
    }
    
    @Override
    public User getUserDetail(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() == 1) {
            throw new RuntimeException("用户不存在");
        }
        return user;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public User createUser(User user, Long adminId) {
        // 检查用户名是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, user.getUsername())
               .eq(User::getDeleted, 0);
        if (userMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("用户名已存在");
        }
        
        // 检查邮箱是否已存在
        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, user.getEmail())
               .eq(User::getDeleted, 0);
        if (userMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("邮箱已存在");
        }
        
        // 设置默认值
        user.setPassword(md5Encode(user.getPassword() != null ? user.getPassword() : "123456"));
        user.setStatus(user.getStatus() != null ? user.getStatus() : 1);
        user.setRole(user.getRole() != null ? user.getRole() : 0);
        user.setGender(user.getGender() != null ? user.getGender() : 0);
        user.setExperienceYears(user.getExperienceYears() != null ? user.getExperienceYears() : 0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setCreateBy(adminId);
        user.setUpdateBy(adminId);
        
        userMapper.insert(user);
        log.info("管理员{}创建用户成功: {}", adminId, user.getUsername());
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User createUser(CreateUserRequest request, Long adminId) {
        // 检查用户名是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername())
               .eq(User::getDeleted, 0);
        if (userMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("用户名已存在");
        }

        // 检查邮箱是否已存在
        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, request.getEmail())
               .eq(User::getDeleted, 0);
        if (userMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("邮箱已存在");
        }

        // 创建用户对象
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(md5Encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname() : request.getUsername());
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setGender(request.getGender() != null ? request.getGender() : 0);
        user.setRole(request.getRole() != null ? request.getRole() : 0);
        user.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        user.setExperienceYears(request.getExperienceYears() != null ? request.getExperienceYears() : 0);
        user.setBio(request.getBio());
        user.setDeleted(0);

        // 设置时间字段
        LocalDateTime now = LocalDateTime.now();
        user.setCreateTime(now);
        user.setUpdateTime(now);
        user.setCreateBy(adminId);
        user.setUpdateBy(adminId);

        userMapper.insert(user);
        log.info("管理员{}通过请求对象创建用户成功: {}", adminId, user.getUsername());
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User updateUser(User user, Long adminId) {
        User existing = getUserDetail(user.getId());
        
        // 如果更新用户名，检查是否重复
        if (!existing.getUsername().equals(user.getUsername())) {
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getUsername, user.getUsername())
                   .ne(User::getId, user.getId())
                   .eq(User::getDeleted, 0);
            if (userMapper.selectCount(wrapper) > 0) {
                throw new RuntimeException("用户名已存在");
            }
        }
        
        // 如果更新邮箱，检查是否重复
        if (!existing.getEmail().equals(user.getEmail())) {
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getEmail, user.getEmail())
                   .ne(User::getId, user.getId())
                   .eq(User::getDeleted, 0);
            if (userMapper.selectCount(wrapper) > 0) {
                throw new RuntimeException("邮箱已存在");
            }
        }
        
        // 使用UpdateWrapper避免乐观锁问题
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, user.getId())
                     .set(User::getUsername, user.getUsername())
                     .set(User::getEmail, user.getEmail())
                     .set(User::getNickname, user.getNickname())
                     .set(User::getRealName, user.getRealName())
                     .set(User::getPhone, user.getPhone())
                     .set(User::getGender, user.getGender())
                     .set(User::getBirthday, user.getBirthday())
                     .set(User::getProfession, user.getProfession())
                     .set(User::getExperienceYears, user.getExperienceYears())
                     .set(User::getBio, user.getBio())
                     .set(User::getStatus, user.getStatus())
                     .set(User::getRole, user.getRole())
                     .set(User::getUpdateTime, LocalDateTime.now())
                     .set(User::getUpdateBy, adminId);

        int result = userMapper.update(null, updateWrapper);
        if (result > 0) {
            log.info("管理员{}更新用户成功: {}", adminId, user.getUsername());
            // 返回更新后的用户信息
            return getUserDetail(user.getId());
        } else {
            throw new RuntimeException("更新用户失败");
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUser(Long userId, Long adminId) {
        User user = getUserDetail(userId);

        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(User::getId, userId)
               .set(User::getDeleted, 1)
               .set(User::getStatus, 0) // 同时设置状态为禁用
               .set(User::getUpdateTime, LocalDateTime.now())
               .set(User::getUpdateBy, adminId);

        boolean result = userMapper.update(null, wrapper) > 0;
        if (result) {
            log.info("管理员{}删除用户成功: {}", adminId, user.getUsername());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean restoreUser(Long userId, Long adminId) {
        // 查找已删除的用户
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getId, userId)
                   .eq(User::getDeleted, 1);
        User user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            throw new RuntimeException("用户不存在或未被删除");
        }

        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(User::getId, userId)
               .set(User::getDeleted, 0)
               .set(User::getStatus, 1) // 恢复时设置为启用状态
               .set(User::getUpdateTime, LocalDateTime.now())
               .set(User::getUpdateBy, adminId);

        boolean result = userMapper.update(null, wrapper) > 0;
        if (result) {
            log.info("管理员{}恢复用户成功: {}", adminId, user.getUsername());
        }
        return result;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleUserStatus(Long userId, Integer status, Long adminId) {
        User user = getUserDetail(userId);

        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(User::getId, userId)
               .set(User::getStatus, status)
               .set(User::getUpdateTime, LocalDateTime.now())
               .set(User::getUpdateBy, adminId);

        boolean result = userMapper.update(null, wrapper) > 0;
        if (result) {
            log.info("管理员{}更新用户{}状态为: {}", adminId, user.getUsername(), status);
        }
        return result;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetUserPassword(Long userId, String newPassword, Long adminId) {
        User user = getUserDetail(userId);
        
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(User::getId, userId)
               .set(User::getPassword, md5Encode(newPassword))
               .set(User::getUpdateTime, LocalDateTime.now())
               .set(User::getUpdateBy, adminId);
        
        userMapper.update(null, wrapper);
        log.info("管理员{}重置用户{}密码成功", adminId, user.getUsername());
    }
    
    @Override
    public Map<String, Object> getSystemStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        // 用户统计
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getDeleted, 0);
        Long totalUsers = userMapper.selectCount(wrapper);
        
        wrapper.eq(User::getStatus, 1);
        Long activeUsers = userMapper.selectCount(wrapper);
        
        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getDeleted, 0)
               .eq(User::getStatus, 0);
        Long inactiveUsers = userMapper.selectCount(wrapper);
        
        // 今日新增用户
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getDeleted, 0)
               .ge(User::getCreateTime, today + " 00:00:00")
               .le(User::getCreateTime, today + " 23:59:59");
        Long todayNewUsers = userMapper.selectCount(wrapper);
        
        statistics.put("totalUsers", totalUsers);
        statistics.put("activeUsers", activeUsers);
        statistics.put("inactiveUsers", inactiveUsers);
        statistics.put("todayNewUsers", todayNewUsers);
        
        // 面试相关统计
        try {
            Map<String, Object> interviewStats = getInterviewStatistics();
            statistics.putAll(interviewStats);
        } catch (Exception e) {
            log.warn("获取面试统计失败，使用默认值", e);
            statistics.put("totalInterviews", 0L);
            statistics.put("todayInterviews", 0L);
            statistics.put("completedInterviews", 0L);
        }
        
        return statistics;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchDeleteUsers(Long[] userIds, Long adminId) {
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(User::getId, Arrays.asList(userIds))
               .set(User::getDeleted, 1)
               .set(User::getUpdateTime, LocalDateTime.now())
               .set(User::getUpdateBy, adminId);
        
        int count = userMapper.update(null, wrapper);
        log.info("管理员{}批量删除{}个用户", adminId, count);
        return count > 0;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdateUserStatus(Long[] userIds, Integer status, Long adminId) {
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(User::getId, Arrays.asList(userIds))
               .set(User::getStatus, status)
               .set(User::getUpdateTime, LocalDateTime.now())
               .set(User::getUpdateBy, adminId);
        
        int count = userMapper.update(null, wrapper);
        log.info("管理员{}批量更新{}个用户状态为{}", adminId, count, status);
        return count > 0;
    }
    
    @Override
    public byte[] exportUserData(UserQueryRequest request) {
        try {
            log.info("开始导出用户数据，查询条件: {}", request);

            // 构建查询条件
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getDeleted, 0);

            // 关键词搜索
            if (StringUtils.hasText(request.getKeyword())) {
                wrapper.and(w -> w.like(User::getUsername, request.getKeyword())
                               .or().like(User::getEmail, request.getKeyword())
                               .or().like(User::getNickname, request.getKeyword()));
            }

            // 状态筛选
            if (request.getStatus() != null) {
                wrapper.eq(User::getStatus, request.getStatus());
            }

            // 性别筛选
            if (request.getGender() != null) {
                wrapper.eq(User::getGender, request.getGender());
            }

            // 职业筛选
            if (StringUtils.hasText(request.getProfession())) {
                wrapper.like(User::getProfession, request.getProfession());
            }

            // 工作经验筛选
            if (request.getMinExperienceYears() != null) {
                wrapper.ge(User::getExperienceYears, request.getMinExperienceYears());
            }
            if (request.getMaxExperienceYears() != null) {
                wrapper.le(User::getExperienceYears, request.getMaxExperienceYears());
            }

            // 时间范围筛选
            if (StringUtils.hasText(request.getStartDate())) {
                wrapper.ge(User::getCreateTime, request.getStartDate() + " 00:00:00");
            }
            if (StringUtils.hasText(request.getEndDate())) {
                wrapper.le(User::getCreateTime, request.getEndDate() + " 23:59:59");
            }

            // 排序
            wrapper.orderByDesc(User::getCreateTime);

            // 查询用户数据
            List<User> users = userMapper.selectList(wrapper);

            // 生成CSV内容
            StringBuilder csvContent = new StringBuilder();

            // CSV头部
            csvContent.append("用户ID,用户名,邮箱,昵称,真实姓名,手机号,性别,角色,状态,职业,工作经验,个人简介,创建时间,最后登录时间,最后登录IP\n");

            // 数据行
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (User user : users) {
                csvContent.append(user.getId()).append(",")
                         .append(escapeCSV(user.getUsername())).append(",")
                         .append(escapeCSV(user.getEmail())).append(",")
                         .append(escapeCSV(user.getNickname())).append(",")
                         .append(escapeCSV(user.getRealName())).append(",")
                         .append(escapeCSV(user.getPhone())).append(",")
                         .append(getGenderText(user.getGender())).append(",")
                         .append(getRoleText(user.getRole())).append(",")
                         .append(getStatusText(user.getStatus())).append(",")
                         .append(escapeCSV(user.getProfession())).append(",")
                         .append(user.getExperienceYears() != null ? user.getExperienceYears() : 0).append(",")
                         .append(escapeCSV(user.getBio())).append(",")
                         .append(user.getCreateTime() != null ? user.getCreateTime().format(formatter) : "").append(",")
                         .append(user.getLastLoginTime() != null ? user.getLastLoginTime().format(formatter) : "").append(",")
                         .append(escapeCSV(user.getLastLoginIp())).append("\n");
            }

            log.info("用户数据导出完成，共导出{}条记录", users.size());
            return csvContent.toString().getBytes("UTF-8");

        } catch (Exception e) {
            log.error("导出用户数据失败", e);
            throw new RuntimeException("导出用户数据失败: " + e.getMessage());
        }
    }

    /**
     * CSV字段转义
     */
    private String escapeCSV(String value) {
        if (value == null) {
            return "";
        }
        // 如果包含逗号、引号或换行符，需要用引号包围并转义内部引号
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * 获取性别文本
     */
    private String getGenderText(Integer gender) {
        if (gender == null) return "未知";
        switch (gender) {
            case 1: return "男";
            case 2: return "女";
            default: return "未知";
        }
    }

    /**
     * 获取角色文本
     */
    private String getRoleText(Integer role) {
        if (role == null) return "普通用户";
        switch (role) {
            case 1: return "管理员";
            default: return "普通用户";
        }
    }

    /**
     * 获取状态文本
     */
    private String getStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 1: return "启用";
            case 0: return "禁用";
            default: return "未知";
        }
    }
    

    
    /**
     * 获取面试统计数据
     */
    private Map<String, Object> getInterviewStatistics() {
        Map<String, Object> stats = new HashMap<>();

        try {
            // 调用面试服务获取统计数据
            String url = "http://interview-service/interview/statistics";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.get("data") != null) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                stats.put("totalInterviews", data.getOrDefault("totalInterviews", 0L));
                stats.put("todayInterviews", data.getOrDefault("todayInterviews", 0L));
                stats.put("completedInterviews", data.getOrDefault("completedInterviews", 0L));
                stats.put("averageScore", data.getOrDefault("averageScore", 0.0));
            } else {
                // 默认值
                stats.put("totalInterviews", 0L);
                stats.put("todayInterviews", 0L);
                stats.put("completedInterviews", 0L);
                stats.put("averageScore", 0.0);
            }
        } catch (Exception e) {
            log.warn("调用面试服务获取统计失败: {}", e.getMessage());
            // 返回默认值
            stats.put("totalInterviews", 0L);
            stats.put("todayInterviews", 0L);
            stats.put("completedInterviews", 0L);
            stats.put("averageScore", 0.0);
        }

        return stats;
    }

    @Override
    public List<Map<String, Object>> getUserTrendData() {
        List<Map<String, Object>> trendData = new ArrayList<>();

        try {
            // 获取最近30天的用户注册数据
            for (int i = 29; i >= 0; i--) {
                LocalDateTime date = LocalDateTime.now().minusDays(i);
                String dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

                LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(User::getDeleted, 0)
                       .ge(User::getCreateTime, dateStr + " 00:00:00")
                       .le(User::getCreateTime, dateStr + " 23:59:59");

                Long count = userMapper.selectCount(wrapper);

                Map<String, Object> dayData = new HashMap<>();
                dayData.put("date", dateStr);
                dayData.put("count", count);
                trendData.add(dayData);
            }
        } catch (Exception e) {
            log.error("获取用户趋势数据失败", e);
        }

        return trendData;
    }

    @Override
    public List<Map<String, Object>> getGenderDistribution() {
        List<Map<String, Object>> genderData = new ArrayList<>();

        try {
            // 统计各性别用户数量
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getDeleted, 0);

            // 男性用户
            wrapper.eq(User::getGender, 1);
            Long maleCount = userMapper.selectCount(wrapper);

            // 女性用户
            wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getDeleted, 0).eq(User::getGender, 2);
            Long femaleCount = userMapper.selectCount(wrapper);

            // 未知性别用户
            wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getDeleted, 0).and(w -> w.eq(User::getGender, 0).or().isNull(User::getGender));
            Long unknownCount = userMapper.selectCount(wrapper);

            if (maleCount > 0) {
                Map<String, Object> maleData = new HashMap<>();
                maleData.put("gender", 1);
                maleData.put("count", maleCount);
                genderData.add(maleData);
            }

            if (femaleCount > 0) {
                Map<String, Object> femaleData = new HashMap<>();
                femaleData.put("gender", 2);
                femaleData.put("count", femaleCount);
                genderData.add(femaleData);
            }

            if (unknownCount > 0) {
                Map<String, Object> unknownData = new HashMap<>();
                unknownData.put("gender", 0);
                unknownData.put("count", unknownCount);
                genderData.add(unknownData);
            }
        } catch (Exception e) {
            log.error("获取性别分布数据失败", e);
        }

        return genderData;
    }

    @Override
    public Map<String, Object> getActivityData() {
        Map<String, Object> activityData = new HashMap<>();

        try {
            // 获取最近7天的登录数据
            List<Integer> weeklyData = new ArrayList<>();

            for (int i = 6; i >= 0; i--) {
                LocalDateTime date = LocalDateTime.now().minusDays(i);
                String dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

                LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(User::getDeleted, 0)
                       .eq(User::getStatus, 1)
                       .ge(User::getLastLoginTime, dateStr + " 00:00:00")
                       .le(User::getLastLoginTime, dateStr + " 23:59:59");

                Long count = userMapper.selectCount(wrapper);
                weeklyData.add(count.intValue());
            }

            activityData.put("weeklyData", weeklyData);
        } catch (Exception e) {
            log.error("获取活跃度数据失败", e);
            activityData.put("weeklyData", Arrays.asList(0, 0, 0, 0, 0, 0, 0));
        }

        return activityData;
    }

    @Override
    public List<Map<String, Object>> getUserStatusDistribution() {
        List<Map<String, Object>> statusData = new ArrayList<>();

        try {
            // 统计各状态用户数量
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getDeleted, 0);

            // 正常用户
            wrapper.eq(User::getStatus, 1);
            Long activeCount = userMapper.selectCount(wrapper);

            // 禁用用户
            wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getDeleted, 0).eq(User::getStatus, 0);
            Long inactiveCount = userMapper.selectCount(wrapper);

            if (activeCount > 0) {
                Map<String, Object> activeData = new HashMap<>();
                activeData.put("status", 1);
                activeData.put("statusName", "正常");
                activeData.put("count", activeCount);
                statusData.add(activeData);
            }

            if (inactiveCount > 0) {
                Map<String, Object> inactiveData = new HashMap<>();
                inactiveData.put("status", 0);
                inactiveData.put("statusName", "禁用");
                inactiveData.put("count", inactiveCount);
                statusData.add(inactiveData);
            }
        } catch (Exception e) {
            log.error("获取用户状态分布数据失败", e);
        }

        return statusData;
    }

    /**
     * MD5加密
     */
    private String md5Encode(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("密码加密失败", e);
        }
    }
}
