import request from './index'

export const getProductPage = (params) => request.get('/products/page', { params })
export const getProductList = () => request.get('/products')
export const getProductById = (id) => request.get(`/products/${id}`)
export const createProduct = (data) => request.post('/products', data)
export const updateProduct = (id, data) => request.put(`/products/${id}`, data)
export const deleteProduct = (id) => request.delete(`/products/${id}`)
export const getCategoryStats = () => request.get('/products/stats')
