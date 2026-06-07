import request from './index'

export const getUserPage = (params) => request.get('/users/page', { params })
export const getUserList = (params) => request.get('/users', { params })
export const getUserById = (id) => request.get(`/users/${id}`)
export const createUser = (data) => request.post('/users', data)
export const updateUser = (id, data) => request.put(`/users/${id}`, data)
export const deleteUser = (id) => request.delete(`/users/${id}`)
export const batchCreateUsers = (data) => request.post('/users/batch', data)
