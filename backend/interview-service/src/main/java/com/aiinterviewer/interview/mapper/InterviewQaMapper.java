package com.aiinterviewer.interview.mapper;

import com.aiinterviewer.interview.entity.InterviewQa;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 面试问答记录Mapper接口
 */
@Mapper
public interface InterviewQaMapper extends BaseMapper<InterviewQa> {

    /**
     * 根据会话ID查询问答记录
     */
    @Select("SELECT * FROM interview_qa WHERE session_id = #{sessionId} AND (deleted = 0 OR deleted IS NULL) ORDER BY question_order ASC")
    List<InterviewQa> findBySessionId(@Param("sessionId") Long sessionId);

    /**
     * 根据会话ID和问题序号查询
     */
    @Select("SELECT * FROM interview_qa WHERE session_id = #{sessionId} AND question_order = #{questionOrder} AND (deleted = 0 OR deleted IS NULL)")
    InterviewQa findBySessionIdAndOrder(@Param("sessionId") Long sessionId, @Param("questionOrder") Integer questionOrder);

    /**
     * 查询会话的最新问题
     */
    @Select("SELECT * FROM interview_qa WHERE session_id = #{sessionId} AND (deleted = 0 OR deleted IS NULL) ORDER BY question_order DESC LIMIT 1")
    InterviewQa findLatestQuestion(@Param("sessionId") Long sessionId);

    /**
     * 统计会话已回答问题数
     */
    @Select("SELECT COUNT(*) FROM interview_qa WHERE session_id = #{sessionId} AND answer IS NOT NULL AND answer != '' AND (deleted = 0 OR deleted IS NULL)")
    int countAnsweredQuestions(@Param("sessionId") Long sessionId);

    /**
     * 统计会话总问题数
     */
    @Select("SELECT COUNT(*) FROM interview_qa WHERE session_id = #{sessionId} AND (deleted = 0 OR deleted IS NULL)")
    int countTotalQuestions(@Param("sessionId") Long sessionId);

    /**
     * 计算会话平均分数
     */
    @Select("SELECT AVG(score) FROM interview_qa WHERE session_id = #{sessionId} AND score IS NOT NULL AND (deleted = 0 OR deleted IS NULL)")
    Double calculateAverageScore(@Param("sessionId") Long sessionId);

    /**
     * 计算会话平均情感分数
     */
    @Select("SELECT AVG(emotion_score) FROM interview_qa WHERE session_id = #{sessionId} AND emotion_score IS NOT NULL AND (deleted = 0 OR deleted IS NULL)")
    Double calculateAverageEmotionScore(@Param("sessionId") Long sessionId);

    /**
     * 计算会话平均自信度分数
     */
    @Select("SELECT AVG(confidence_score) FROM interview_qa WHERE session_id = #{sessionId} AND confidence_score IS NOT NULL AND (deleted = 0 OR deleted IS NULL)")
    Double calculateAverageConfidenceScore(@Param("sessionId") Long sessionId);

    /**
     * 计算用户所有面试的平均分数
     */
    @Select("SELECT AVG(qa.score) FROM interview_qa qa " +
            "INNER JOIN interview_session s ON qa.session_id = s.id " +
            "WHERE s.user_id = #{userId} AND qa.score IS NOT NULL AND (qa.deleted = 0 OR qa.deleted IS NULL) AND (s.deleted = 0 OR s.deleted IS NULL)")
    Double calculateAverageScoreByUser(@Param("userId") Long userId);
    
    /**
     * 查询未回答的问题
     */
    List<InterviewQa> findUnansweredQuestions(@Param("sessionId") Long sessionId);
    
    /**
     * 批量插入问题
     */
    int batchInsertQuestions(@Param("questions") List<InterviewQa> questions);
}
