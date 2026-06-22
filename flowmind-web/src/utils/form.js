export function parseFormSchema(schemaJson) {
  try {
    const parsed = JSON.parse(schemaJson || '{}')
    return (parsed.fields || []).map((field) => ({
      ...field,
      id: field.id || `field_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
    }))
  } catch (error) {
    return []
  }
}

export function parseControlOptions(field) {
  try {
    return JSON.parse(field.optionsJson || '[]')
  } catch (error) {
    return []
  }
}

export function parseJson(value, fallback) {
  if (!value) return fallback
  try {
    return JSON.parse(value)
  } catch {
    return fallback
  }
}

export function initBusinessData(fields, source = {}) {
  const result = {}
  fields.forEach((field) => {
    const key = field.fieldKey || field.key
    const type = field.componentType || field.type
    if (Object.prototype.hasOwnProperty.call(source, key)) {
      result[key] = source[key]
    } else {
      result[key] = type === 'BOOLEAN' ? false : ''
    }
  })
  return result
}

export function normalizeBusinessData(data, fields) {
  const result = {}
  Object.entries(data).forEach(([key, value]) => {
    const field = fields.find((f) => f.fieldKey === key)
    const type = field?.componentType || field?.type
    result[key] = ['NUMBER', 'AMOUNT'].includes(type) && value !== '' ? Number(value) : value
  })
  return result
}