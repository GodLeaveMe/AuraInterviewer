import request from './request'

export const fileApi = {
  // 上传头像
  uploadAvatar(file: File) {
    const formData = new FormData()
    formData.append('file', file)

    return request.post('/user/file/upload/avatar', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },

  // 上传文档
  uploadDocument(file: File) {
    const formData = new FormData()
    formData.append('file', file)

    return request.post('/user-service/file/upload/document', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },

  // 删除文件
  deleteFile(fileUrl: string) {
    return request.delete('/user-service/file/delete', {
      params: { fileUrl }
    })
  }
}
