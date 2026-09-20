// 後台維修單的「異動紅點」：只在「客戶造成的異動」亮起。
// 做法：記錄每張單「上次看過的更新時間」，repairUpdatedTime 比它新、而且單目前的狀態是客戶操作才會造成的，就亮紅點。
// 記錄存在瀏覽器 localStorage，只有這台電腦的這個瀏覽器看得到，不用改後端/資料表。
// 時間只拿後端回傳的 repairUpdatedTime 互相比較，不跟瀏覽器現在的時間比，避免伺服器跟瀏覽器時區不同造成誤判。
const STORAGE_KEY = "gigafix.admin.repairSeen";

function load() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY)) ?? {};
  } catch {
    return {};
  }
}

function save(data) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
  } catch {
    // 瀏覽器擋 localStorage(隱私模式等)就算了，只是紅點不會被記住
  }
}

function toTime(value) {
  return value ? new Date(value).getTime() : 0;
}

// 單目前的狀態，是不是「只有客戶操作才會變成這樣」。後端沒有記錄是誰改的，只能用狀態推測：
// - 待估價+沒有技師：客戶新預約(只有客戶會建單)
// - 已取消：後端只有客戶能取消
// - 維修中+客戶同意 / 報價後不維修+客戶拒絕：客戶回應報價
// - 維修完成或尚未取件+已有取件方式：客戶選了取件與付款方式(線上付款完成也在這個狀態)
// 技師造成的狀態(已報價、結案、未送檢、還沒選取件的維修完成…)一律不亮
function isCustomerCaused(r) {
  switch (r.repairStatus) {
    case "PENDING_QUOTE":
      return !r.technicianId;
    case "CANCELLED":
      return true;
    case "IN_REPAIR":
      return r.approvalStatus === "APPROVED";
    case "QUOTE_REJECTED":
      return r.approvalStatus === "REJECTED";
    case "REPAIR_COMPLETED":
    case "AWAITING_PICKUP":
      return !!r.pickupType;
    default:
      return false;
  }
}

// 第一次使用時，把「目前列表裡最新的更新時間」當作基準線，之前的舊單都視為已看過，
// 不然一開始整個列表會全部亮紅點。基準線之後才新增/異動的單才會亮
export function initSeenBaseline(repairs) {
  const data = load();
  if (data.baseline || repairs.length === 0) return;
  data.baseline = repairs.reduce(
    (latest, r) => (toTime(r.repairUpdatedTime) > toTime(latest) ? r.repairUpdatedTime : latest),
    repairs[0].repairUpdatedTime,
  );
  save(data);
}

// 這張單要不要亮紅點：客戶造成的狀態，而且比上次看過的更新
export function hasUnseenChange(repair) {
  if (!isCustomerCaused(repair)) return false;
  const data = load();
  if (!data.baseline) return false; // 還沒建立基準線(第一次載入前)，不亮
  const seenAt = data.seen?.[repair.id] ?? data.baseline;
  return toTime(repair.repairUpdatedTime) > toTime(seenAt);
}

// 打開詳情頁(含在詳情頁操作完重新載入)時呼叫，把這張單標為已看過
export function markRepairSeen(repair) {
  if (!repair?.id || !repair.repairUpdatedTime) return;
  const data = load();
  data.seen = { ...data.seen, [repair.id]: repair.repairUpdatedTime };
  save(data);
}
