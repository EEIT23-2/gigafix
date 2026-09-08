import axios from "axios";

// 商品 Controller 的共同路徑。
// Vite 會把 /api 開頭的請求代理到 http://localhost:8080。
const PRODUCT_URL = "/api/products";
const ADMIN_PRODUCT_URL = "/api/admin/products";

//回收單Controller的共同路徑
const RECYCLE_APPLICATION_URL = "/api/recycle-applications";
const ADMIN_RECYCLE_APPLICATION_URL = "/api/admin/recycle-applications";

/**
 * 查詢商品列表。
 *
 * params 可以包含：
 * category、saleStatus、search、modelName、color、storage、
 * orderBy、sort、minPrice、maxPrice、limit、offset。
 *
 * 對應：
 * GET /api/products
 */
export const getProducts = async (params = {}) => {
  const response = await axios.get(PRODUCT_URL, {
    params,
  });

  // 直接回傳 Spring Data Page<Product>。
  return response.data;
};

/**
 * 依照商品 ID 取得單一商品。
 *
 * 對應：
 * GET /api/products/{productId}
 */
export const getProduct = async (productId) => {
  const response = await axios.get(`${PRODUCT_URL}/${productId}`);

  return response.data;
};

/**
 * 新增商品。
 *
 * productRequest 是傳給後端 ProductRequest 的物件。
 *
 * 對應：
 * POST /api/products
 */
export const createProduct = async (productRequest) => {
  const response = await axios.post(ADMIN_PRODUCT_URL, productRequest);
  return response.data;
};

/**
 * 修改指定商品。
 *
 * 對應：
 * PUT /api/products/{productId}
 */
export const updateProduct = async (productId, productRequest) => {
  const response = await axios.put(
    `${ADMIN_PRODUCT_URL}/${productId}`,
    productRequest,
  );
  return response.data;
};

/**
 * 刪除指定商品。
 *
 * 後端成功時回傳 HTTP 204，因此沒有 response body。
 *
 * 對應：
 * DELETE /api/products/{productId}
 */
export const deleteProduct = async (productId) => {
  await axios.delete(`${ADMIN_PRODUCT_URL}/${productId}`);
};

/**
 * 刪除所有商品。
 *
 * 這是危險操作，View 呼叫前應顯示確認視窗。
 *
 * 對應：
 * DELETE /api/products
 */
export const deleteAllProducts = async () => {
  await axios.delete(ADMIN_PRODUCT_URL);
};

/**
 * 從後端設定的 JSON 資料來源匯入商品。
 *
 * 依照目前 Controller，這支 API 不需要 request body。
 *
 * 對應：
 * POST /api/products/import
 */
export const importProducts = async () => {
  const response = await axios.post(`${ADMIN_PRODUCT_URL}/import`);
  return response.data;
};

/**
 * 將資料庫商品匯出成 JSON 檔。
 *
 * responseType 必須設定為 blob，
 * 才能讓瀏覽器建立下載檔案。
 *
 * 對應：
 * GET /api/products/export
 */
export const exportProducts = async () => {
  const response = await axios.get(`${ADMIN_PRODUCT_URL}/export`, {
    responseType: "blob",
  });
  // 回傳 Blob 給 View 建立下載連結。
  return response.data;
};

/**
 * 將商品設為「已保留」。
 *
 * 對應：
 * PUT /api/products/{productId}/reserve
 */
export const reserveProduct = async (productId) => {
  const response = await axios.put(`${ADMIN_PRODUCT_URL}/${productId}/reserve`);
  return response.data;
};

/**
 * 解除商品保留，恢復可販售狀態。
 *
 * 對應：
 * PUT /api/products/{productId}/release
 */
export const releaseProduct = async (productId) => {
  const response = await axios.put(`${ADMIN_PRODUCT_URL}/${productId}/release`);
  return response.data;
};

/**
 * 將商品設為「已售出」。
 *
 * 對應：
 * PUT /api/products/{productId}/sell
 */
export const sellProduct = async (productId) => {
  const response = await axios.put(`${ADMIN_PRODUCT_URL}/${productId}/sell`);
  return response.data;
};
//以下是回收單需要的api

/**
 * 後台查詢回收申請列表
 *
 * params 可包含：
 * productName、appearance、productCategory、recycleStatus、
 * orderBy、sort、limit、offset
 *
 * GET /api/admin/recycle-applications
 */
export const getRecycleApplications = async (params = {}) => {
  const response = await axios.get(ADMIN_RECYCLE_APPLICATION_URL, {
    params,
  });

  return response.data;
};

/**
 * 後台依 applyId 查詢單筆回收申請
 *
 * GET /api/admin/recycle-applications/{applyId}
 */
export const getRecycleApplication = async (applyId) => {
  const response = await axios.get(
    `${ADMIN_RECYCLE_APPLICATION_URL}/${applyId}`,
  );

  return response.data;
};

/**
 * 使用者新增回收申請
 *
 * POST /api/recycle-applications
 */
export const createRecycleApplication = async (recycleRequest) => {
  const response = await axios.post(RECYCLE_APPLICATION_URL, recycleRequest);

  return response.data;
};

/**
 * 後台修改回收申請
 *
 * PUT /api/admin/recycle-applications/{applyId}
 */
export const updateRecycleApplication = async (applyId, recycleRequest) => {
  const response = await axios.put(
    `${ADMIN_RECYCLE_APPLICATION_URL}/${applyId}`,
    recycleRequest,
  );

  return response.data;
};

/**
 * 後台刪除單筆回收申請
 *
 * DELETE /api/admin/recycle-applications/{applyId}
 */
export const deleteRecycleApplication = async (applyId) => {
  await axios.delete(`${ADMIN_RECYCLE_APPLICATION_URL}/${applyId}`);
};

/**
 * 後台刪除全部回收申請
 *
 * DELETE /api/admin/recycle-applications
 */
export const deleteAllRecycleApplications = async () => {
  await axios.delete(ADMIN_RECYCLE_APPLICATION_URL);
};
