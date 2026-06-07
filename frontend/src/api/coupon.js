import request from './index'

export const createCouponTemplate = (data) => {
  return request({
    url: '/coupons/templates',
    method: 'post',
    data
  })
}

export const getCouponTemplates = (params) => {
  return request({
    url: '/coupons/templates/page',
    method: 'get',
    params
  })
}

export const getAvailableTemplates = () => {
  return request({
    url: '/coupons/templates/available',
    method: 'get'
  })
}

export const updateTemplateStatus = (id, status) => {
  return request({
    url: `/coupons/templates/${id}/status`,
    method: 'put',
    params: { status }
  })
}

export const receiveCoupon = (templateId) => {
  return request({
    url: `/coupons/templates/${templateId}/receive`,
    method: 'post'
  })
}

export const getMyCoupons = (status) => {
  return request({
    url: '/coupons/my',
    method: 'get',
    params: { status }
  })
}

export const getMyCouponDetail = (id) => {
  return request({
    url: `/coupons/my/${id}`,
    method: 'get'
  })
}

export const getAvailableCouponsForOrder = (orderAmount) => {
  return request({
    url: '/coupons/my/available-for-order',
    method: 'get',
    params: { orderAmount }
  })
}

export const calculateDiscount = (userCouponId, orderAmount) => {
  return request({
    url: `/coupons/${userCouponId}/calculate`,
    method: 'post',
    params: { orderAmount }
  })
}
