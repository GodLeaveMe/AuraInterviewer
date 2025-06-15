package com.aiinterviewer.interview.mapper;

import com.aiinterviewer.interview.entity.InterviewSession;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 面试会话Mapper接口
 */
@Mapper
public interface InterviewSessionMapper extends BaseMapper<InterviewSession> {

    /**
     * 根据用户ID查询面试会话列表
     */
    @Select("SELECT * FROM interview_session WHERE user_id = #{userId} AND (deleted = 0 OR deleted IS NULL) ORDER BY create_time DESC")
    List<InterviewSession> findByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID和状态查询面试会话
     */
    @Select("SELECT * FROM interview_session WHERE user_id = #{userId} AND status = #{status} AND (deleted = 0 OR deleted IS NULL) ORDER BY create_time DESC")
    List<InterviewSession> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Integer status);

    /**
     * 查询用户的进行中面试
     */
    @Select("SELECT * FROM interview_session WHERE user_id = #{userId} AND status IN (0, 1) AND (deleted = 0 OR deleted IS NULL) ORDER BY create_time DESC LIMIT 1")
    InterviewSession findActiveSessionByUserId(@Param("userId") Long userId);

    /**
     * 更新面试状态
     */
    @Update("UPDATE interview_session SET status = #{status}, update_time = NOW() WHERE id = #{sessionId}")
    int updateStatus(@Param("sessionId") Long sessionId, @Param("status") Integer status);

    /**
     * 更新面试分数
     */
    @Update("UPDATE interview_session SET score = #{score}, update_time = NOW() WHERE id = #{sessionId}")
    int updateScore(@Param("sessionId") Long sessionId, @Param("score") Double score);

    /**
     * 统计用户面试次数
     */
    @Select("SELECT COUNT(*) FROM interview_session WHERE user_id = #{userId} AND (deleted = 0 OR deleted IS NULL)")
    int countByUserId(@Param("userId") Long userId);
    
    /**
     * 查询最近的面试记录
     */
    @Select("SELECT * FROM interview_session WHERE user_id = #{userId} AND (deleted = 0 OR deleted IS NULL) ORDER BY create_time DESC LIMIT #{limit}")
    List<InterviewSession> findRecentSessions(@Param("userId") Long userId, @Param("limit") Integer limit);
}
