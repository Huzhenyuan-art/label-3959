import request from './index'

export const getMyFavorites = () => request.get('/favorites')
export const addFavorite = (data) => request.post('/favorites', data)
export const removeFavorite = (id) => request.delete(`/favorites/${id}`)
export const removeFavoriteByProductId = (productId) => request.delete(`/favorites/product/${productId}`)
export const checkFavorite = (productId) => request.get(`/favorites/check/${productId}`)
