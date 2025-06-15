// 音频录制工具类
export interface AudioRecorderOptions {
  sampleRate?: number
  channels?: number
  bitDepth?: number
  mimeType?: string
}

export interface RecordingResult {
  blob: Blob
  url: string
  duration: number
  size: number
}

export class AudioRecorder {
  private mediaRecorder: MediaRecorder | null = null
  private audioStream: MediaStream | null = null
  private audioChunks: Blob[] = []
  private startTime: number = 0
  private options: AudioRecorderOptions

  constructor(options: AudioRecorderOptions = {}) {
    this.options = {
      sampleRate: 44100,
      channels: 1,
      bitDepth: 16,
      mimeType: 'audio/webm;codecs=opus',
      ...options
    }
  }

  // 检查浏览器支持
  static isSupported(): boolean {
    return !!(navigator.mediaDevices &&
              typeof navigator.mediaDevices.getUserMedia === 'function' &&
              typeof window.MediaRecorder !== 'undefined')
  }

  // 请求麦克风权限
  async requestPermission(): Promise<boolean> {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      stream.getTracks().forEach(track => track.stop())
      return true
    } catch (error) {
      console.error('请求麦克风权限失败:', error)
      return false
    }
  }

  // 开始录音
  async startRecording(): Promise<void> {
    if (!AudioRecorder.isSupported()) {
      throw new Error('浏览器不支持音频录制')
    }

    try {
      // 获取音频流
      this.audioStream = await navigator.mediaDevices.getUserMedia({
        audio: {
          sampleRate: this.options.sampleRate,
          channelCount: this.options.channels,
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true
        }
      })

      // 创建MediaRecorder
      const mimeType = this.getSupportedMimeType()
      this.mediaRecorder = new MediaRecorder(this.audioStream, {
        mimeType
      })

      // 重置数据
      this.audioChunks = []
      this.startTime = Date.now()

      // 设置事件处理器
      this.mediaRecorder.ondataavailable = (event) => {
        if (event.data.size > 0) {
          this.audioChunks.push(event.data)
        }
      }

      // 开始录制
      this.mediaRecorder.start(100) // 每100ms收集一次数据
    } catch (error) {
      console.error('开始录音失败:', error)
      throw new Error('无法访问麦克风，请检查权限设置')
    }
  }

  // 停止录音
  async stopRecording(): Promise<RecordingResult> {
    return new Promise((resolve, reject) => {
      if (!this.mediaRecorder || this.mediaRecorder.state === 'inactive') {
        reject(new Error('录音未开始'))
        return
      }

      this.mediaRecorder.onstop = () => {
        try {
          const duration = (Date.now() - this.startTime) / 1000
          const blob = new Blob(this.audioChunks, { 
            type: this.mediaRecorder!.mimeType 
          })
          const url = URL.createObjectURL(blob)

          const result: RecordingResult = {
            blob,
            url,
            duration,
            size: blob.size
          }

          // 清理资源
          this.cleanup()
          resolve(result)
        } catch (error) {
          reject(error)
        }
      }

      this.mediaRecorder.onerror = (_event) => {
        reject(new Error('录音过程中发生错误'))
      }

      this.mediaRecorder.stop()
    })
  }

  // 暂停录音
  pauseRecording(): void {
    if (this.mediaRecorder && this.mediaRecorder.state === 'recording') {
      this.mediaRecorder.pause()
    }
  }

  // 恢复录音
  resumeRecording(): void {
    if (this.mediaRecorder && this.mediaRecorder.state === 'paused') {
      this.mediaRecorder.resume()
    }
  }

  // 取消录音
  cancelRecording(): void {
    if (this.mediaRecorder && this.mediaRecorder.state !== 'inactive') {
      this.mediaRecorder.stop()
    }
    this.cleanup()
  }

  // 获取录音状态
  getState(): string {
    return this.mediaRecorder ? this.mediaRecorder.state : 'inactive'
  }

  // 获取录音时长
  getDuration(): number {
    if (this.startTime === 0) return 0
    return (Date.now() - this.startTime) / 1000
  }

  // 获取音频流
  getAudioStream(): MediaStream | null {
    return this.audioStream
  }

  // 清理资源
  private cleanup(): void {
    if (this.audioStream) {
      this.audioStream.getTracks().forEach(track => track.stop())
      this.audioStream = null
    }
    this.mediaRecorder = null
    this.audioChunks = []
    this.startTime = 0
  }

  // 获取支持的MIME类型
  private getSupportedMimeType(): string {
    const types = [
      'audio/webm;codecs=opus',
      'audio/webm',
      'audio/mp4',
      'audio/wav'
    ]

    for (const type of types) {
      if (MediaRecorder.isTypeSupported(type)) {
        return type
      }
    }

    return 'audio/webm' // 默认类型
  }
}

// 音频播放器工具类
export class AudioPlayer {
  private audio: HTMLAudioElement
  private onEndedCallback?: () => void
  private onErrorCallback?: (error: Error) => void

  constructor() {
    this.audio = new Audio()
    this.setupEventListeners()
  }

  // 设置事件监听器
  private setupEventListeners(): void {
    this.audio.onended = () => {
      if (this.onEndedCallback) {
        this.onEndedCallback()
      }
    }

    this.audio.onerror = () => {
      if (this.onErrorCallback) {
        this.onErrorCallback(new Error('音频播放失败'))
      }
    }
  }

  // 播放音频
  async play(src: string): Promise<void> {
    this.audio.src = src
    try {
      await this.audio.play()
    } catch (error) {
      throw new Error('音频播放失败')
    }
  }

  // 暂停播放
  pause(): void {
    this.audio.pause()
  }

  // 停止播放
  stop(): void {
    this.audio.pause()
    this.audio.currentTime = 0
  }

  // 设置音量
  setVolume(volume: number): void {
    this.audio.volume = Math.max(0, Math.min(1, volume))
  }

  // 获取时长
  getDuration(): number {
    return this.audio.duration || 0
  }

  // 获取当前时间
  getCurrentTime(): number {
    return this.audio.currentTime
  }

  // 设置当前时间
  setCurrentTime(time: number): void {
    this.audio.currentTime = time
  }

  // 是否正在播放
  isPlaying(): boolean {
    return !this.audio.paused
  }

  // 设置播放结束回调
  onEnded(callback: () => void): void {
    this.onEndedCallback = callback
  }

  // 设置错误回调
  onError(callback: (error: Error) => void): void {
    this.onErrorCallback = callback
  }

  // 销毁播放器
  destroy(): void {
    this.stop()
    this.audio.src = ''
    this.onEndedCallback = undefined
    this.onErrorCallback = undefined
  }
}

// 音频工具函数
export const audioUtils = {
  // 格式化时长
  formatDuration(seconds: number): string {
    const minutes = Math.floor(seconds / 60)
    const remainingSeconds = Math.floor(seconds % 60)
    return `${minutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}`
  },

  // 格式化文件大小
  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 B'
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
  },

  // 检查音频格式支持
  isAudioFormatSupported(mimeType: string): boolean {
    const audio = document.createElement('audio')
    return audio.canPlayType(mimeType) !== ''
  }
}
