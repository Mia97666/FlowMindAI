/**
 * 将前端画布中的流程图模型转换为 BPMN XML。
 *
 * 这里保持“轻量 BPMN”策略：前端负责输出可交换、可审计的 BPMN 基础结构，
 * 具体执行仍由后端工作流引擎读取 definitionJson 完成，BPMN XML 主要用于后续
 * 对接外部建模器、归档与审计。
 */
export function buildBpmnXml(graph, options = {}) {
  const processId = normalizeBpmnId(options.processCode || 'FLOWMIND_PROCESS')
  const processName = xmlEscape(options.processName || options.processCode || 'FlowMind Process')
  const nodes = graph.nodes || []
  const edges = graph.edges || []
  const nodeXml = nodes.map((node) => buildNodeXml(node, edges)).join('\n')
  const edgeXml = edges.map((edge) => buildEdgeXml(edge)).join('\n')
  const shapeXml = nodes.map((node) => buildShapeXml(node)).join('\n')
  const edgeDiXml = edges.map((edge) => buildEdgeDiXml(edge, nodes)).join('\n')

  return [
    '<?xml version="1.0" encoding="UTF-8"?>',
    '<bpmn:definitions xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"',
    '  xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"',
    '  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"',
    '  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"',
    '  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"',
    '  xmlns:flowmind="https://flowmind.ai/schema/bpmn"',
    `  id="${processId}_definitions" targetNamespace="https://flowmind.ai/bpmn/${processId}">`,
    `  <bpmn:process id="${processId}" name="${processName}" isExecutable="true">`,
    nodeXml,
    edgeXml,
    '  </bpmn:process>',
    `  <bpmndi:BPMNDiagram id="${processId}_diagram">`,
    `    <bpmndi:BPMNPlane id="${processId}_plane" bpmnElement="${processId}">`,
    shapeXml,
    edgeDiXml,
    '    </bpmndi:BPMNPlane>',
    '  </bpmndi:BPMNDiagram>',
    '</bpmn:definitions>',
  ].join('\n')
}

function buildNodeXml(node, edges) {
  const tagName = bpmnTagName(node.nodeType)
  const incoming = edges
    .filter((edge) => edge.target === node.id)
    .map((edge) => `      <bpmn:incoming>${xmlEscape(edge.id)}</bpmn:incoming>`)
    .join('\n')
  const outgoing = edges
    .filter((edge) => edge.source === node.id)
    .map((edge) => `      <bpmn:outgoing>${xmlEscape(edge.id)}</bpmn:outgoing>`)
    .join('\n')
  const configJson = xmlEscape(JSON.stringify(node.config || {}))
  const extensionElements = [
    '      <bpmn:extensionElements>',
    `        <flowmind:node type="${xmlEscape(node.nodeType)}" config="${configJson}" />`,
    '      </bpmn:extensionElements>',
  ].join('\n')

  return [
    `    <${tagName} id="${xmlEscape(node.id)}" name="${xmlEscape(node.name || node.id)}">`,
    extensionElements,
    incoming,
    outgoing,
    `    </${tagName}>`,
  ].filter(Boolean).join('\n')
}

function buildEdgeXml(edge) {
  const condition = edge.condition && !['default', 'else', '默认'].includes(String(edge.condition).trim())
    ? `\n      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">${xmlEscape(edge.condition)}</bpmn:conditionExpression>\n    `
    : ''
  return `    <bpmn:sequenceFlow id="${xmlEscape(edge.id)}" name="${xmlEscape(edge.label || edge.condition || '')}" sourceRef="${xmlEscape(edge.source)}" targetRef="${xmlEscape(edge.target)}">${condition}</bpmn:sequenceFlow>`
}

function buildShapeXml(node) {
  const position = node.position || { x: 80, y: 80 }
  return [
    `      <bpmndi:BPMNShape id="${xmlEscape(node.id)}_di" bpmnElement="${xmlEscape(node.id)}">`,
    `        <dc:Bounds x="${Number(position.x || 0)}" y="${Number(position.y || 0)}" width="180" height="72" />`,
    '      </bpmndi:BPMNShape>',
  ].join('\n')
}

function buildEdgeDiXml(edge, nodes) {
  const source = nodes.find((node) => node.id === edge.source)
  const target = nodes.find((node) => node.id === edge.target)
  const sourcePosition = source?.position || { x: 80, y: 80 }
  const targetPosition = target?.position || { x: 260, y: 80 }

  return [
    `      <bpmndi:BPMNEdge id="${xmlEscape(edge.id)}_di" bpmnElement="${xmlEscape(edge.id)}">`,
    `        <di:waypoint x="${Number(sourcePosition.x || 0) + 180}" y="${Number(sourcePosition.y || 0) + 36}" />`,
    `        <di:waypoint x="${Number(targetPosition.x || 0)}" y="${Number(targetPosition.y || 0) + 36}" />`,
    '      </bpmndi:BPMNEdge>',
  ].join('\n')
}

function bpmnTagName(nodeType) {
  const map = {
    START: 'bpmn:startEvent',
    FORM_TASK: 'bpmn:userTask',
    APPROVAL: 'bpmn:userTask',
    AI_RISK_CHECK: 'bpmn:serviceTask',
    AI_APPROVAL: 'bpmn:serviceTask',
    CONDITION: 'bpmn:exclusiveGateway',
    NOTIFY: 'bpmn:serviceTask',
    END: 'bpmn:endEvent',
  }
  return map[nodeType] || 'bpmn:task'
}

function normalizeBpmnId(value) {
  const text = String(value || 'FLOWMIND_PROCESS').replace(/[^A-Za-z0-9_]/g, '_')
  return /^[A-Za-z_]/.test(text) ? text : `FLOW_${text}`
}

function xmlEscape(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&apos;')
}

/**
 * 将 BPMN 2.0 XML 解析为前端画布节点/连线模型。
 * 支持基础元素：startEvent、endEvent、userTask、serviceTask、exclusiveGateway、sequenceFlow。
 */
export function parseBpmnXml(xml) {
  const parser = new DOMParser()
  const doc = parser.parseFromString(xml, 'text/xml')
  const errorNode = doc.querySelector('parsererror')
  if (errorNode) {
    throw new Error(`XML 解析失败：${errorNode.textContent}`)
  }

  const process = doc.querySelector('process')
  if (!process) {
    throw new Error('未找到 <process> 元素，请确认是有效的 BPMN 2.0 XML')
  }

  const ns = 'http://www.omg.org/spec/BPMN/20100524/MODEL'
  const tagMap = [
    { tag: 'startEvent', type: 'START' },
    { tag: 'endEvent', type: 'END' },
    { tag: 'userTask', type: 'APPROVAL' },
    { tag: 'serviceTask', type: 'AI_RISK_CHECK' },
    { tag: 'exclusiveGateway', type: 'CONDITION' },
  ]

  const nodeIdSet = new Set()
  const nodes = []
  let yOffset = 0

  for (const { tag, type } of tagMap) {
    const elements = [
      ...Array.from(process.getElementsByTagNameNS(ns, tag)),
      ...Array.from(process.getElementsByTagName(tag)),
    ]
    for (const el of elements) {
      const id = el.getAttribute('id')
      const name = el.getAttribute('name') || id
      if (!id || nodeIdSet.has(id)) continue
      nodeIdSet.add(id)
      const extEl = el.querySelector('flowmind\\:node, node')
      let config = {}
      if (extEl) {
        try { config = JSON.parse(extEl.getAttribute('config') || '{}') } catch { /* ignore */ }
      }
      nodes.push({
        id, name, nodeType: type, config,
        position: { x: 200, y: 100 + yOffset },
      })
      yOffset += 120
    }
  }

  const edges = []
  const flowElements = [
    ...Array.from(process.getElementsByTagNameNS(ns, 'sequenceFlow')),
    ...Array.from(process.getElementsByTagName('sequenceFlow')),
  ]

  for (const el of flowElements) {
    const id = el.getAttribute('id')
    const sourceRef = el.getAttribute('sourceRef')
    const targetRef = el.getAttribute('targetRef')
    const label = el.getAttribute('name') || ''
    if (!id || !sourceRef || !targetRef) continue

    const condEl = el.getElementsByTagNameNS(ns, 'conditionExpression')[0]
      || el.getElementsByTagName('conditionExpression')[0]
    const condition = condEl?.textContent?.trim() || ''

    edges.push({
      id, source: sourceRef, target: targetRef, label,
      condition: condition || label || '',
    })
  }

  return { nodes, edges }
}
