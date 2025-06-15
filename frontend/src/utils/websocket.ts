// WebSocket连接管理
export interface WebSocketMessage {
  type: string
  data?: any
  status?: string
  error?: string
  timestamp?: number
}

export class WebSocketManager {
  private ws: WebSocket | null = null
  private url: string
  private reconnectAttempts = 0
  private maxReconnectAttempts = 5
  private reconnectInterval = 3000
  private heartbeatInterval: NodeJS.Timeout | null = null
  private messageHandlers: Map<string, Function[]> = new Map()
  private isConnecting = false

  constructor(url: string) {
    this.url = url
  }

  // 连接WebSocket
  connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      if (this.isConnecting || (this.ws && this.ws.readyState === WebSocket.CONNECTING)) {
        return
      }

      this.isConnecting = true

      try {
        this.ws = new WebSocket(this.url)

        this.ws.onopen = () => {
          console.log('WebSocket连接已建立')
          this.isConnecting = false
          this.reconnectAttempts = 0
          this.startHeartbeat()
          resolve()
        }

        this.ws.onmessage = (event) => {
          try {
            const message: WebSocketMessage = JSON.parse(event.data)
            this.handleMessage(message)
          } catch (error) {
            console.error('解析WebSocket消息失败:', error)
          }
        }

        this.ws.onclose = (event) => {
          console.log('WebSocket连接已关闭:', event.code, event.reason)
          this.isConnecting = false
          this.stopHeartbeat()
          
          if (!event.wasClean && this.reconnectAttempts < this.maxReconnectAttempts) {
            this.reconnect()
          }
        }

        this.ws.onerror = (error) => {
          console.error('WebSocket连接错误:', error)
          this.isConnecting = false
          reject(error)
        }
      } catch (error) {
        this.isConnecting = false
        reject(error)
      }
    })
  }

  // 断开连接
  disconnect() {
    this.stopHeartbeat()
    if (this.ws) {
      this.ws.close(1000, '主动断开连接')
      this.ws = null
    }
  }

  // 发送消息
  send(message: WebSocketMessage): boolean {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      try {
        this.ws.send(JSON.stringify({
          ...message,
          timestamp: Date.now()
        }))
        return true
      } catch (error) {
        console.error('发送WebSocket消息失败:', error)
        return false
      }
    }
    console.warn('WebSocket未连接，无法发送消息')
    return false
  }

  // 注册消息处理器
  on(type: string, handler: Function) {
    if (!this.messageHandlers.has(type)) {
      this.messageHandlers.set(type, [])
    }
    this.messageHandlers.get(type)!.push(handler)
  }

  // 移除消息处理器
  off(type: string, handler?: Function) {
    if (!this.messageHandlers.has(type)) {
      return
    }

    if (handler) {
      const handlers = this.messageHandlers.get(type)!
      const index = handlers.indexOf(handler)
      if (index > -1) {
        handlers.splice(index, 1)
      }
    } else {
      this.messageHandlers.delete(type)
    }
  }

  // 处理接收到的消息
  private handleMessage(message: WebSocketMessage) {
    const handlers = this.messageHandlers.get(message.type)
    if (handlers) {
      handlers.forEach(handler => {
        try {
          handler(message)
        } catch (error) {
          console.error('处理WebSocket消息失败:', error)
        }
      })
    }

    // 处理通用消息
    const allHandlers = this.messageHandlers.get('*')
    if (allHandlers) {
      allHandlers.forEach(handler => {
        try {
          handler(message)
        } catch (error) {
          console.error('处理WebSocket消息失败:', error)
        }
      })
    }
  }

  // 重连
  private reconnect() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('WebSocket重连次数已达上限')
      return
    }

    this.reconnectAttempts++
    console.log(`WebSocket重连中... (${this.reconnectAttempts}/${this.maxReconnectAttempts})`)

    setTimeout(() => {
      this.connect().catch(error => {
        console.error('WebSocket重连失败:', error)
      })
    }, this.reconnectInterval)
  }

  // 开始心跳
  private startHeartbeat() {
    this.heartbeatInterval = setInterval(() => {
      this.send({ type: 'ping' })
    }, 30000) // 30秒心跳
  }

  // 停止心跳
  private stopHeartbeat() {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval)
      this.heartbeatInterval = null
    }
  }

  // 获取连接状态
  get readyState(): number {
    return this.ws ? this.ws.readyState : WebSocket.CLOSED
  }

  // 是否已连接
  get isConnected(): boolean {
    return this.ws ? this.ws.readyState === WebSocket.OPEN : false
  }
}

// 面试WebSocket管理器
export class InterviewWebSocketManager extends WebSocketManager {
  private sessionId: string

  constructor(sessionId: string) {
    const token = localStorage.getItem('token')
    const wsUrl = `${import.meta.env.VITE_WS_URL || 'ws://localhost:8082'}/ws/interview/${sessionId}?token=${token}`
    super(wsUrl)
    this.sessionId = sessionId
  }

  // 加入面试
  joinInterview() {
    return this.send({
      type: 'join_interview',
      data: { sessionId: this.sessionId }
    })
  }

  // 提交回答
  submitAnswer(answer: string, questionOrder: number) {
    return this.send({
      type: 'submit_answer',
      data: { answer, questionOrder, sessionId: this.sessionId }
    })
  }

  // 获取下一个问题
  getNextQuestion() {
    return this.send({
      type: 'get_next_question',
      data: { sessionId: this.sessionId }
    })
  }

  // 发送输入状态
  sendTyping(isTyping: boolean) {
    return this.send({
      type: 'typing',
      data: { isTyping, sessionId: this.sessionId }
    })
  }
}

// 创建面试WebSocket连接
export function createInterviewWebSocket(sessionId: string): InterviewWebSocketManager {
  return new InterviewWebSocketManager(sessionId)
}
