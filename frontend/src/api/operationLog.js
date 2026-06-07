import request from './index'

export const getOperationLogPage = (params) => request.get('/operation-logs/page', { params })
export const getOperationLogById = (id) => request.get(`/operation-logs/${id}`)
export const getOperationTypes = () => request.get('/operation-logs/operation-types')
export const getOperationCategories = () => request.get('/operation-logs/operation-categories')
