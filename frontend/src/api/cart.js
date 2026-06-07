import request from './index'

export const getMyCart = () => request.get('/carts')
export const addToCart = (data) => request.post('/carts', data)
export const updateCartQuantity = (id, data) => request.put(`/carts/${id}/quantity`, data)
export const removeFromCart = (id) => request.delete(`/carts/${id}`)
export const batchRemoveCart = (data) => request.delete('/carts/batch', { data })
export const checkoutCart = (data) => request.post('/carts/checkout', data)
