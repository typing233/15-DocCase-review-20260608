export interface AutoTagRule {
  id: number
  tenantId: string
  name: string
  description: string | null
  priority: number
  isEnabled: boolean
  rolloutPercentage: number
  version: number
  conditionTree: string
  actions: string
  triggerEvent: string
  executionCount: number
  errorCount: number
  lastExecutedAt: string | null
  createdBy: number
  createdAt: string
  updatedAt: string
}

export interface ConditionNode {
  operator?: string
  field?: string
  fieldOperator?: string
  value?: any
  conditions?: ConditionNode[]
}

export interface RuleAction {
  type: 'ADD_TAG' | 'REMOVE_TAG'
  tagId: number
}

export interface RuleDryRunResult {
  ruleId: number
  ruleName: string
  totalDocuments: number
  matchedDocuments: number
  matches: {
    documentId: number
    documentTitle: string
    matched: boolean
    actionsToExecute: string[]
  }[]
}
