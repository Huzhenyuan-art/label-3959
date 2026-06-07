import request, { createCrudApi, createApiCall } from './index'

const basePath = '/products'
const crudApi = createCrudApi(basePath)

export const {
  getPage: getProductPage,
  getList: getProductList,
  getById: getProductById,
  create: createProduct,
  update: updateProduct,
  remove: deleteProduct
} = crudApi

export const getCategoryStats = createApiCall(params => request.get(`${basePath}/stats`, { params }))
