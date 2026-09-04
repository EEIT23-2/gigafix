export function formatDateTime(ldtString) {
  if (!ldtString) return ''

  // 拆出日期跟時間兩部分
  const [datePart, timePart] = ldtString.split('T')
  if (!datePart || !timePart) return ldtString // 格式不符預期時,原樣顯示,不讓畫面壞掉

  const [year, month, day] = datePart.split('-')

  // 時間部分可能帶奈秒(.8387595),只取到秒
  const [hour, minute, secondWithNano] = timePart.split(':')
  const second = secondWithNano.split('.')[0]

  return `${year}/${month}/${day} ${hour}:${minute}:${second}`
}