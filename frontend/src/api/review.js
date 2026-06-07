import request from './index'

export const submitReview = (data) => request.post('/reviews', data)
export const getReviewPage = (params) => request.get('/reviews/page', { params })
export const getReviewStats = (productId) => request.get(`/reviews/stats/${productId}`)
export const getPendingReviews = () => request.get('/reviews/pending')
