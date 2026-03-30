type NotifyType = 'success' | 'error' | 'warning'

export const notify = async (type: NotifyType, text: string) => {
  const { message } = await import('antd')
  message[type](text)
}
