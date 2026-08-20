import axios from "axios";

// 商品 Controller 的共同路徑。
// Vite 會把 /api 開頭的請求代理到 http://localhost:8080。
const PRODUCT_URL = "/api/products";

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
  const response = await axios.post(PRODUCT_URL, productRequest);

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
    `${PRODUCT_URL}/${productId}`,
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
  await axios.delete(`${PRODUCT_URL}/${productId}`);
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
  await axios.delete(PRODUCT_URL);
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
  const response = await axios.post(`${PRODUCT_URL}/import`);

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
  const response = await axios.get(`${PRODUCT_URL}/export`, {
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
  const response = await axios.put(`${PRODUCT_URL}/${productId}/reserve`);

  return response.data;
};

/**
 * 解除商品保留，恢復可販售狀態。
 *
 * 對應：
 * PUT /api/products/{productId}/release
 */
export const releaseProduct = async (productId) => {
  const response = await axios.put(`${PRODUCT_URL}/${productId}/release`);

  return response.data;
};

/**
 * 將商品設為「已售出」。
 *
 * 對應：
 * PUT /api/products/{productId}/sell
 */
export const sellProduct = async (productId) => {
  const response = await axios.put(`${PRODUCT_URL}/${productId}/sell`);

  return response.data;
};
