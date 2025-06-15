// 模拟面试API，避免503错误
export const interviewMockApi = {
  // 模板管理
  getTemplateList(category?: string, difficulty?: number) {
    return Promise.resolve({
      code: 200,
      data: [
        {
          id: 1,
          name: '前端开发面试模板',
          category: 'frontend',
          difficulty: 'medium',
          description: '适用于前端开发岗位的面试模板，包含HTML、CSS、JavaScript、Vue等技术栈问题',
          questionCount: 10,
          duration: 60,
          createTime: new Date().toISOString(),
          updateTime: new Date().toISOString(),
          isPublic: 1,
          usageCount: 100,
          userId: 1
        },
        {
          id: 2,
          name: 'Java后端面试模板',
          category: 'backend',
          difficulty: 'hard',
          description: '适用于Java后端开发岗位的面试模板，包含Spring、MyBatis、数据库等问题',
          questionCount: 15,
          duration: 90,
          createTime: new Date().toISOString(),
          updateTime: new Date().toISOString(),
          isPublic: 1,
          usageCount: 80,
          userId: 1
        },
        {
          id: 3,
          name: 'Python开发面试模板',
          category: 'backend',
          difficulty: 'medium',
          description: '适用于Python开发岗位的面试模板，包含Django、Flask、数据分析等问题',
          questionCount: 12,
          duration: 75,
          createTime: new Date().toISOString(),
          updateTime: new Date().toISOString(),
          isPublic: 1,
          usageCount: 60,
          userId: 1
        }
      ],
      message: '获取成功'
    })
  },

  getTemplateDetail(templateId: number) {
    return Promise.resolve({
      code: 200,
      data: {
        id: templateId,
        name: '前端开发面试模板',
        category: 'frontend',
        difficulty: 'medium',
        description: '适用于前端开发岗位的面试模板',
        questionCount: 10,
        duration: 60,
        createTime: new Date().toISOString(),
        updateTime: new Date().toISOString(),
        isPublic: 1,
        usageCount: 100,
        userId: 1
      },
      message: '获取成功'
    })
  },

  createTemplate(data: any) {
    return Promise.resolve({
      code: 200,
      data: {
        id: Math.floor(Math.random() * 1000) + 1,
        ...data,
        createTime: new Date().toISOString(),
        updateTime: new Date().toISOString(),
        usageCount: 0
      },
      message: '创建成功'
    })
  },

  updateTemplate(templateId: number, data: any) {
    return Promise.resolve({
      code: 200,
      data: { id: templateId, ...data, updateTime: new Date().toISOString() },
      message: '更新成功'
    })
  },

  deleteTemplate(templateId: number) {
    return Promise.resolve({
      code: 200,
      data: true,
      message: '删除成功'
    })
  },

  // 面试会话管理
  createSession(data: any) {
    return Promise.resolve({
      code: 200,
      data: {
        id: Math.floor(Math.random() * 1000) + 1,
        userId: 1,
        templateId: data.templateId,
        title: '面试会话',
        status: 0, // 0-未开始 1-进行中 2-已完成 3-已取消
        score: null,
        duration: 0,
        startTime: null,
        endTime: null,
        questionCount: 10,
        answeredCount: 0,
        voiceEnabled: data.voiceEnabled || 0,
        createTime: new Date().toISOString(),
        updateTime: new Date().toISOString()
      },
      message: '创建成功'
    })
  },

  startInterview(sessionId: number) {
    return Promise.resolve({
      code: 200,
      data: {
        id: sessionId,
        status: 1,
        startTime: new Date().toISOString(),
        updateTime: new Date().toISOString()
      },
      message: '面试开始'
    })
  },

  finishInterview(sessionId: number) {
    return Promise.resolve({
      code: 200,
      data: {
        id: sessionId,
        status: 2,
        endTime: new Date().toISOString(),
        score: 85,
        duration: 2700,
        updateTime: new Date().toISOString()
      },
      message: '面试结束'
    })
  },

  cancelInterview(sessionId: number) {
    return Promise.resolve({
      code: 200,
      data: {
        id: sessionId,
        status: 3,
        updateTime: new Date().toISOString()
      },
      message: '面试取消'
    })
  },

  getSessionDetail(sessionId: number) {
    return Promise.resolve({
      code: 200,
      data: {
        id: sessionId,
        userId: 1,
        templateId: 1,
        title: '前端开发面试',
        status: 1,
        score: null,
        duration: 1800,
        startTime: new Date(Date.now() - 1800000).toISOString(),
        endTime: null,
        questionCount: 10,
        answeredCount: 5,
        voiceEnabled: 1,
        createTime: new Date(Date.now() - 1800000).toISOString(),
        updateTime: new Date().toISOString()
      },
      message: '获取成功'
    })
  },

  getCurrentSession() {
    return Promise.resolve({
      code: 200,
      data: null, // 当前没有进行中的面试
      message: '获取成功'
    })
  },

  // 问答管理
  getNextQuestion(sessionId: number) {
    const questions = [
      '请介绍一下你的前端开发经验',
      '什么是闭包？请举例说明',
      'Vue.js的生命周期有哪些？',
      '如何优化网页性能？',
      '什么是Promise？如何使用？'
    ]
    
    const randomIndex = Math.floor(Math.random() * questions.length)
    
    return Promise.resolve({
      code: 200,
      data: {
        id: Math.floor(Math.random() * 1000) + 1,
        sessionId,
        questionOrder: randomIndex + 1,
        question: questions[randomIndex],
        category: 'technical',
        difficulty: 'medium',
        timeLimit: 300,
        createTime: new Date().toISOString()
      },
      message: '获取成功'
    })
  },

  submitAnswer(data: any) {
    return Promise.resolve({
      code: 200,
      data: {
        id: Math.floor(Math.random() * 1000) + 1,
        sessionId: data.sessionId,
        questionOrder: data.questionOrder,
        question: '示例问题',
        answer: data.answer,
        answerType: data.answerType || 'text',
        voiceUrl: data.voiceUrl,
        voiceDuration: data.voiceDuration,
        thinkingTime: data.thinkingTime,
        score: Math.floor(Math.random() * 40) + 60, // 60-100分
        feedback: '回答不错，继续保持',
        answerTime: new Date().toISOString(),
        createTime: new Date().toISOString()
      },
      message: '提交成功'
    })
  },

  getInterviewQaList(sessionId: number) {
    return Promise.resolve({
      code: 200,
      data: [
        {
          id: 1,
          sessionId,
          questionOrder: 1,
          question: '请介绍一下你的前端开发经验',
          answer: '我有3年的前端开发经验，主要使用Vue.js和React...',
          answerType: 'text',
          score: 85,
          feedback: '回答详细，经验丰富',
          answerTime: new Date(Date.now() - 3600000).toISOString(),
          createTime: new Date(Date.now() - 3600000).toISOString()
        }
      ],
      message: '获取成功'
    })
  },

  // 面试历史
  getUserInterviewHistory(page = 1, size = 10) {
    return Promise.resolve({
      code: 200,
      data: [
        {
          id: 1,
          userId: 1,
          templateId: 1,
          title: '前端开发面试',
          status: 2,
          score: 85,
          duration: 2700,
          startTime: new Date(Date.now() - 86400000).toISOString(),
          endTime: new Date(Date.now() - 83700000).toISOString(),
          questionCount: 10,
          answeredCount: 10,
          voiceEnabled: 1,
          createTime: new Date(Date.now() - 86400000).toISOString(),
          updateTime: new Date(Date.now() - 83700000).toISOString()
        },
        {
          id: 2,
          userId: 1,
          templateId: 2,
          title: 'Java后端面试',
          status: 2,
          score: 78,
          duration: 3600,
          startTime: new Date(Date.now() - 172800000).toISOString(),
          endTime: new Date(Date.now() - 169200000).toISOString(),
          questionCount: 15,
          answeredCount: 15,
          voiceEnabled: 0,
          createTime: new Date(Date.now() - 172800000).toISOString(),
          updateTime: new Date(Date.now() - 169200000).toISOString()
        }
      ],
      message: '获取成功'
    })
  },

  // 面试报告
  generateInterviewReport(sessionId: number) {
    return Promise.resolve({
      code: 200,
      data: `
# 面试报告

## 基本信息
- 面试ID: ${sessionId}
- 面试时间: ${new Date().toLocaleString()}
- 总分: 85分
- 用时: 45分钟

## 表现分析
您在本次面试中表现良好，技术基础扎实，回答问题思路清晰。

## 改进建议
1. 可以加强算法和数据结构的学习
2. 建议多关注最新的前端技术趋势
3. 提升项目实战经验

## 详细评分
- 技术基础: 85分
- 项目经验: 80分
- 沟通表达: 90分
- 综合素质: 85分
      `,
      message: '生成成功'
    })
  },

  deleteInterviewSession(sessionId: number) {
    return Promise.resolve({
      code: 200,
      data: true,
      message: '删除成功'
    })
  },

  restartInterview(sessionId: number) {
    return Promise.resolve({
      code: 200,
      data: {
        id: sessionId,
        status: 0,
        score: null,
        duration: 0,
        startTime: null,
        endTime: null,
        answeredCount: 0,
        updateTime: new Date().toISOString()
      },
      message: '重置成功'
    })
  }
}
