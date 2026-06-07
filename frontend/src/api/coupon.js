import request from './index'

export const createCouponTemplate = (data) => {
  return request({
    url: '/api/coupons/templates',
    method: 'post',
    data
  })
}

export const getCouponTemplates = (params) => {
  return request({
    url: '/api/coupons/templates/page',
    method: 'get',
    params
  })
}

export const getAvailableTemplates = () => {
  return request({
    url: '/api/coupons/templates/available',
    method: 'get'
  })
}

export const updateTemplateStatus = (id, status) => {
  return request({
    url: `/api/coupons/templates/${id}/status`,
    method: 'put',
    params: { status }
  })
}

export const receiveCoupon = (templateId) => {
  return request({
    url: `/api/coupons/templates/${templateId}/receive`,
    method: 'post'
  })
}

export const getMyCoupons = (status) => {
  return request({
    url: '/api/coupons/my',
    method: 'get',
    params: { status }
  })
}

export const getMyCouponDetail = (id) => {
  return request({
    url: `/api/coupons/my/${id}`,
    method: 'get'
  })
}

export const getAvailableCouponsForOrder = (orderAmount) => {
  return request({
    url: '/api/coupons/my/available-for-order',
    method: 'get',
    params: { orderAmount }
  })
}

export const calculateDiscount = (userCouponId, orderAmount) => {
  return request({
    url: `/api/coupons/${userCouponId}/calculate`,
    method: 'post',
    params: { orderAmount }
  })
}
