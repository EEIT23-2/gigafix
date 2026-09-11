// 維修報價參考表的靜態資料。
// 只服務 iPhone SE、11~17 共8個系列，價格參考 https://topwinfix.com.tw/service/detail/5 (2026/08更新)。
// 這份資料刻意不做成資料庫表格、不做後台管理頁，因為更新頻率低，維修模組時間有限，優先做核心維修流程；
// 之後要正式上線的話，比照 Stores/Technicians 的做法，抽成表格+後台管理頁讓店家自行維護。
// 這份資料同時被三個地方共用：報價參考頁面的表格、送修表單「故障狀況描述」的項目下拉選單、技師報價頁的項目下拉選單。

// 13個維修項目，順序固定，key給程式用、label給畫面顯示
export const REPAIR_ITEMS = [
  { key: "battery", label: "電池更換" },
  { key: "screen", label: "螢幕面板" },
  { key: "rearCamera", label: "後鏡頭" },
  { key: "frontCameraFaceId", label: "前鏡頭排線(含Face ID)" },
  { key: "chargingPort", label: "尾插排線(充電孔)" },
  { key: "speaker", label: "喇叭" },
  { key: "backCover", label: "後殼/背蓋" },
  { key: "waterDamage", label: "泡水清潔檢測" },
  { key: "inspectionFee", label: "檢測費" },
  { key: "iosReinstall", label: "iOS系統重灌" },
  { key: "dataTransfer", label: "資料轉移" },
  { key: "motherboard", label: "主機板維修" },
  { key: "dataRecovery", label: "救資料" },
];

// 8個系列，順序固定 SE→17
export const SERIES_LIST = [
  { id: "SE", label: "SE 系列" },
  { id: "11", label: "11 系列" },
  { id: "12", label: "12 系列" },
  { id: "13", label: "13 系列" },
  { id: "14", label: "14 系列" },
  { id: "15", label: "15 系列" },
  { id: "16", label: "16 系列" },
  { id: "17", label: "17 系列" },
];

// 每個系列底下的機型 + 各機型對應13項目的價格(數字)；價格是 null 代表該站尚未開放報價(顯示「詢價」)
export const PRICE_TABLE = {
  SE: {
    models: ["SE1", "SE2", "SE3"],
    prices: {
      battery: [790, 890, 890],
      screen: [1000, 2000, 2000],
      rearCamera: [700, 2000, 2000],
      frontCameraFaceId: [700, 1000, 1000],
      chargingPort: [700, 1000, 1000],
      speaker: [700, 1000, 1000],
      backCover: [null, 2000, 2000],
      waterDamage: [800, 800, 800],
      inspectionFee: [300, 300, 300],
      iosReinstall: [500, 500, 500],
      dataTransfer: [1000, 1000, 1000],
      motherboard: [2500, 2500, 2500],
      dataRecovery: [2500, 2500, 2500],
    },
  },
  11: {
    models: ["11", "11 Pro", "11 Pro Max"],
    prices: {
      battery: [1390, 1390, 1390],
      screen: [3000, 4500, 5000],
      rearCamera: [2000, 3000, 3000],
      frontCameraFaceId: [2600, 2600, 2600],
      chargingPort: [1600, 2200, 2200],
      speaker: [1600, 1600, 1600],
      backCover: [3000, 3000, 3000],
      waterDamage: [800, 800, 800],
      inspectionFee: [300, 300, 300],
      iosReinstall: [500, 500, 500],
      dataTransfer: [1000, 1000, 1000],
      motherboard: [4500, 4500, 4500],
      dataRecovery: [3500, 3500, 3500],
    },
  },
  12: {
    models: ["12 mini", "12", "12 Pro", "12 Pro Max"],
    prices: {
      battery: [1490, 1490, 1490, 1490],
      screen: [5500, 5500, 5500, 6000],
      rearCamera: [2500, 2500, 4500, 4500],
      frontCameraFaceId: [3600, 3600, 3600, 3600],
      chargingPort: [2600, 2600, 2600, 2600],
      speaker: [2600, 2600, 2600, 2600],
      backCover: [3500, 3500, 3500, 3500],
      waterDamage: [800, 800, 800, 800],
      inspectionFee: [300, 300, 300, 300],
      iosReinstall: [500, 500, 500, 500],
      dataTransfer: [1000, 1000, 1000, 1000],
      motherboard: [5000, 5000, 5000, 5000],
      dataRecovery: [4500, 4500, 4500, 4500],
    },
  },
  13: {
    models: ["13 mini", "13", "13 Pro", "13 Pro Max"],
    prices: {
      battery: [1590, 1590, 1590, 1590],
      screen: [5500, 5500, 6000, 6500],
      rearCamera: [2500, 2500, 4500, 4500],
      frontCameraFaceId: [3600, 3600, 3600, 3600],
      chargingPort: [2600, 2600, 2600, 2600],
      speaker: [2600, 2600, 2600, 2600],
      backCover: [4000, 4000, 4000, 4000],
      waterDamage: [800, 800, 800, 800],
      inspectionFee: [300, 300, 300, 300],
      iosReinstall: [500, 500, 500, 500],
      dataTransfer: [1000, 1000, 1000, 1000],
      motherboard: [6000, 6000, 6000, 6000],
      dataRecovery: [5000, 5000, 5000, 5000],
    },
  },
  14: {
    models: ["14", "14 Plus", "14 Pro", "14 Pro Max"],
    prices: {
      battery: [1790, 1790, 1790, 1790],
      screen: [6500, 7500, 8500, 9500],
      rearCamera: [3500, 3500, 5500, 5500],
      frontCameraFaceId: [4600, 4600, 4600, 4600],
      chargingPort: [3600, 3600, 3600, 3600],
      speaker: [3600, 3600, 3600, 3600],
      backCover: [3500, 3500, 5000, 5000],
      waterDamage: [800, 800, 800, 800],
      inspectionFee: [300, 300, 300, 300],
      iosReinstall: [500, 500, 500, 500],
      dataTransfer: [1000, 1000, 1000, 1000],
      motherboard: [7000, 7000, 7000, 7000],
      dataRecovery: [6000, 6000, 6000, 6000],
    },
  },
  15: {
    models: ["15", "15 Plus", "15 Pro", "15 Pro Max"],
    prices: {
      battery: [1790, 1790, 1790, 1790],
      screen: [6500, 7500, 8500, 9500],
      rearCamera: [3500, 3500, 5500, 5500],
      frontCameraFaceId: [4600, 4600, 4600, 4600],
      chargingPort: [3600, 3600, 3600, 3600],
      speaker: [3600, 3600, 3600, 3600],
      backCover: [3500, 3500, 3500, 3500],
      waterDamage: [800, 800, 800, 800],
      inspectionFee: [300, 300, 300, 300],
      iosReinstall: [500, 500, 500, 500],
      dataTransfer: [1000, 1000, 1000, 1000],
      motherboard: [8000, 8000, 8000, 8000],
      dataRecovery: [7000, 7000, 7000, 7000],
    },
  },
  16: {
    models: ["16", "16 Plus", "16 Pro", "16 Pro Max", "16e"],
    prices: {
      battery: [1990, 1990, 1990, 1990, 1990],
      screen: [7000, 8000, 9000, 10000, 7000],
      rearCamera: [4500, 4500, 5500, 5500, 4500],
      frontCameraFaceId: [4600, 4600, 4600, 4600, 4600],
      chargingPort: [3600, 3600, 3600, 3600, 3600],
      speaker: [3600, 3600, 3600, 3600, 3600],
      backCover: [4500, 4500, 4500, 4500, 4500],
      waterDamage: [800, 800, 800, 800, 800],
      inspectionFee: [300, 300, 300, 300, 300],
      iosReinstall: [500, 500, 500, 500, 500],
      dataTransfer: [1000, 1000, 1000, 1000, 1000],
      motherboard: [8000, 8000, 8000, 8000, 8000],
      dataRecovery: [7000, 7000, 7000, 7000, 7000],
    },
  },
  17: {
    models: ["17", "Air", "17 Pro", "17 Pro Max"],
    prices: {
      battery: [null, null, null, null],
      screen: [7000, null, 9000, 10000],
      rearCamera: [4500, 4500, 5500, 5500],
      frontCameraFaceId: [4600, 4600, 4600, 4600],
      chargingPort: [3600, 3600, 3600, 3600],
      speaker: [3600, 3600, 3600, 3600],
      backCover: [4500, 4500, 4500, 4500],
      waterDamage: [800, 800, 800, 800],
      inspectionFee: [300, 300, 300, 300],
      iosReinstall: [500, 500, 500, 500],
      dataTransfer: [1000, 1000, 1000, 1000],
      motherboard: [9000, 9000, 9000, 9000],
      dataRecovery: [8000, 8000, 8000, 8000],
    },
  },
};

// 取某系列底下的機型清單
export function getModelsForSeries(seriesId) {
  return PRICE_TABLE[seriesId]?.models ?? [];
}

// 取某系列+某機型(用機型在該系列的index)+某項目的價格，查不到或該項目尚未開放就回傳null
export function getItemPrice(seriesId, modelIndex, itemKey) {
  return PRICE_TABLE[seriesId]?.prices?.[itemKey]?.[modelIndex] ?? null;
}

// 加總選中項目的價格；遇到null(尚未開放)的項目會跳過不計入，並用hasUnavailable告知呼叫端有項目沒被加進去
export function sumItemsPrice(seriesId, modelIndex, itemKeys) {
  let total = 0;
  let hasUnavailable = false;
  for (const key of itemKeys) {
    const price = getItemPrice(seriesId, modelIndex, key);
    if (price == null) {
      hasUnavailable = true;
    } else {
      total += price;
    }
  }
  return { total, hasUnavailable };
}

// 價格轉千分位字串，null顯示「詢價」
export function formatPrice(price) {
  return price == null ? "詢價" : `$${price.toLocaleString()}`;
}
