import axios from "axios";

const REPAIRS_URL = "/api/repairs";
const TECHNICIANS_URL = "/api/repairtechnicians";
const STORES_URL = "/api/stores";

// ========== 維修單 ==========

// 客戶預約維修單（需登入，memberId 後端會自己從登入資訊拿，這裡不用傳）
export const createAppointment = async (appointmentRequest) => {
  const response = await axios.post(`${REPAIRS_URL}/appointment`, appointmentRequest);
  return response.data;
};

// 查某分店、某一天已經被預約的時段，讓預約頁面把這些時段設為不可選
export const getBookedSlots = async (storeId, date) => {
  const response = await axios.get(`${REPAIRS_URL}/booked-slots`, {
    params: { storeId, date },
  });
  return response.data;
};

// 查詢維修單，params 可以是 { id, memberId, memberName, technicianId, technicianName, status }
// 每個欄位都可以不填，不填就是查全部
export const searchRepairs = async (params = {}) => {
  const response = await axios.get(REPAIRS_URL, { params });
  return response.data;
};

// 依 id 查單一維修單
export const getRepair = async (repairId) => {
  const response = await axios.get(`${REPAIRS_URL}/${repairId}`);
  return response.data;
};

// 會員中心「維修進度」用：查登入會員自己的所有維修單（不用傳memberId，後端從登入資訊拿）
export const getMyRepairs = async () => {
  const response = await axios.get(`${REPAIRS_URL}/me`);
  return response.data;
};

// 客戶回應報價（同意／拒絕），不用傳memberId，後端從登入資訊拿
export const respondToQuote = async (repairId, approve) => {
  const response = await axios.patch(
    `${REPAIRS_URL}/${repairId}/approval`,
    null,
    { params: { approve } },
  );
  return response.data;
};

// 技師認領維修單
export const assignRepair = async (repairId, technicianId) => {
  const response = await axios.post(`${REPAIRS_URL}/${repairId}/assign`, null, {
    params: { technicianId },
  });
  return response.data;
};

// 技師填寫／修改檢測報價（部分更新，還沒送出正式報價前都可以改）
export const updateQuote = async (repairId, quotationRequest) => {
  const response = await axios.patch(
    `${REPAIRS_URL}/${repairId}/quote`,
    quotationRequest,
  );
  return response.data;
};

// 技師正式送出報價
export const submitQuote = async (repairId, technicianId) => {
  const response = await axios.patch(
    `${REPAIRS_URL}/${repairId}/quote/submit`,
    null,
    { params: { technicianId } },
  );
  return response.data;
};

// 技師在維修中補充／更新檢測結果
export const updateInspectionResult = async (
  repairId,
  inspectionResultRequest,
) => {
  const response = await axios.patch(
    `${REPAIRS_URL}/${repairId}/inspection-note`,
    inspectionResultRequest,
  );
  return response.data;
};

// 技師維修完成
export const completeRepair = async (repairId, completeRepairRequest) => {
  const response = await axios.patch(
    `${REPAIRS_URL}/${repairId}/complete`,
    completeRepairRequest,
  );
  return response.data;
};

// 已通知客戶取件
export const markNotified = async (repairId, technicianId) => {
  const response = await axios.patch(
    `${REPAIRS_URL}/${repairId}/notify`,
    null,
    { params: { technicianId } },
  );
  return response.data;
};

// 門市取貨付款、結案
export const closeRepair = async (repairId, technicianId) => {
  const response = await axios.patch(`${REPAIRS_URL}/${repairId}/close`, null, {
    params: { technicianId },
  });
  return response.data;
};

// 報價不維修：技師填最終金額(檢測費)送出，狀態推進到尚未取件；finalCost 不填就是0元
export const notifyRejected = async (repairId, technicianId, finalCost) => {
  const response = await axios.patch(
    `${REPAIRS_URL}/${repairId}/notify-rejected`,
    null,
    { params: { technicianId, finalCost } },
  );
  return response.data;
};

// 客戶預約後未送修
export const markUndelivered = async (repairId, technicianId) => {
  const response = await axios.patch(
    `${REPAIRS_URL}/${repairId}/undelivered`,
    null,
    { params: { technicianId } },
  );
  return response.data;
};

// 技師手動更新付款狀態
export const updatePayStatus = async (repairId, payStatus) => {
  const response = await axios.patch(
    `${REPAIRS_URL}/${repairId}/pay-status`,
    null,
    { params: { payStatus } },
  );
  return response.data;
};

// 客戶選取件方式＋付款方式（選寄件要附收件人姓名/電話/地址），只能送出一次，不用傳memberId，後端從登入資訊拿
export const submitPickupPayment = async (repairId, pickupPaymentRequest) => {
  const response = await axios.patch(
    `${REPAIRS_URL}/${repairId}/pickup-payment`,
    pickupPaymentRequest,
  );
  return response.data;
};

// 技師編輯收件人資訊：結案前都可以改，僅限客戶選寄件的單
export const updateRecipient = async (repairId, recipientRequest) => {
  const response = await axios.patch(
    `${REPAIRS_URL}/${repairId}/recipient`,
    recipientRequest,
  );
  return response.data;
};

// ========== 技師 ==========

// 查詢技師，storeId 有填就查該分店的技師，不填就查全部
export const getTechnicians = async (storeId) => {
  const response = await axios.get(TECHNICIANS_URL, {
    params: storeId ? { storeId } : {},
  });
  return response.data;
};

export const createTechnician = async (technicianRequest) => {
  const response = await axios.post(TECHNICIANS_URL, technicianRequest);
  return response.data;
};

export const updateTechnician = async (technicianId, technicianRequest) => {
  const response = await axios.put(
    `${TECHNICIANS_URL}/${technicianId}`,
    technicianRequest,
  );
  return response.data;
};

export const deleteTechnician = async (technicianId) => {
  await axios.delete(`${TECHNICIANS_URL}/${technicianId}`);
};

// ========== 分店 ==========

export const getStores = async () => {
  const response = await axios.get(STORES_URL);
  return response.data;
};

export const createStore = async (storeRequest) => {
  const response = await axios.post(STORES_URL, storeRequest);
  return response.data;
};

export const updateStore = async (storeId, storeRequest) => {
  const response = await axios.put(`${STORES_URL}/${storeId}`, storeRequest);
  return response.data;
};

export const deleteStore = async (storeId) => {
  await axios.delete(`${STORES_URL}/${storeId}`);
};
