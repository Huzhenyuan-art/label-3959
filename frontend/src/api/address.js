import request from './index'

export const getMyAddresses = () => request.get('/addresses')
export const getDefaultAddress = () => request.get('/addresses/default')
export const getAddressDetail = (id) => request.get(`/addresses/${id}`)
export const createAddress = (data) => request.post('/addresses', data)
export const updateAddress = (id, data) => request.put(`/addresses/${id}`, data)
export const deleteAddress = (id) => request.delete(`/addresses/${id}`)
export const setDefaultAddress = (id) => request.put(`/addresses/${id}/default`)
