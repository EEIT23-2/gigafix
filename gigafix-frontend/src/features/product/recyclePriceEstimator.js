/**
 * 回收自動估價規則。
 * 每個型號先依參考價目表定義價格與容量／尺寸範圍，再以規格 70%、外觀 30%
 * 換算區間內的估價，最後四捨五入到百元。此檔案只負責計算，不呼叫 API。
 */

// iPhone 的 Pro 與非 Pro 使用不同參考區間，讓型號字串中的 Pro 直接反映價差。
const IPHONE_PRICES = {
  17: {
    pro: { minCapacity: 256, maxCapacity: 2048, minPrice: 28500, maxPrice: 45500 },
    standard: { minCapacity: 256, maxCapacity: 512, minPrice: 20000, maxPrice: 26000 },
  },
  16: {
    pro: { minCapacity: 128, maxCapacity: 1024, minPrice: 19000, maxPrice: 30500 },
    standard: { minCapacity: 128, maxCapacity: 512, minPrice: 10000, maxPrice: 18500 },
  },
  15: {
    pro: { minCapacity: 128, maxCapacity: 1024, minPrice: 14000, maxPrice: 21500 },
    standard: { minCapacity: 128, maxCapacity: 512, minPrice: 11000, maxPrice: 12500 },
  },
  14: {
    pro: { minCapacity: 128, maxCapacity: 1024, minPrice: 9000, maxPrice: 15000 },
    standard: { minCapacity: 128, maxCapacity: 512, minPrice: 6500, maxPrice: 9500 },
  },
  13: {
    pro: { minCapacity: 128, maxCapacity: 1024, minPrice: 6000, maxPrice: 11000 },
    standard: { minCapacity: 128, maxCapacity: 512, minPrice: 4000, maxPrice: 6500 },
  },
  12: {
    pro: { minCapacity: 128, maxCapacity: 512, minPrice: 3500, maxPrice: 6500 },
    standard: { minCapacity: 64, maxCapacity: 256, minPrice: 2000, maxPrice: 3500 },
  },
};

// iPad 以晶片、產品線或世代判斷價目表列；使用函式可容納多種使用者輸入方式。
const IPAD_PRICES = [
  {
    label: "iPad Pro 最新世代",
    matches: (text) => /\bpro\b/i.test(text) && /(m4|m2|最新)/i.test(text),
    minCapacity: 256,
    maxCapacity: 2048,
    minPrice: 20000,
    maxPrice: 45000,
  },
  {
    label: "iPad Pro 前代世代",
    matches: (text) => /\bpro\b/i.test(text) && /(a12z|前代)/i.test(text),
    minCapacity: 128,
    maxCapacity: 1024,
    minPrice: 12000,
    maxPrice: 28000,
  },
  {
    label: "iPad Air 最新世代",
    matches: (text) => /\bair\b/i.test(text) && /(m2|最新)/i.test(text),
    minCapacity: 128,
    maxCapacity: 512,
    minPrice: 9000,
    maxPrice: 23000,
  },
  {
    label: "iPad Air 前代世代",
    matches: (text) => /\bair\b/i.test(text) && /(air\s*[34]\b|前代)/i.test(text),
    minCapacity: 64,
    maxCapacity: 256,
    minPrice: 4500,
    maxPrice: 9000,
  },
  {
    label: "iPad mini 最新世代",
    matches: (text) => /\bmini\s*[67]\b|\bmini\b.*最新/i.test(text),
    minCapacity: 128,
    maxCapacity: 512,
    minPrice: 7500,
    maxPrice: 15000,
  },
  {
    label: "iPad mini 前代世代",
    matches: (text) => /\bmini\s*[45]\b|\bmini\b.*前代/i.test(text),
    minCapacity: 64,
    maxCapacity: 256,
    minPrice: 2500,
    maxPrice: 5000,
  },
  {
    label: "iPad 一般標準版（第 9／10 代）",
    matches: (text) => !/(pro|air|mini)/i.test(text) && /(第?\s*(?:9|10)\s*代|\b(?:9th|10th)\b)/i.test(text),
    minCapacity: 64,
    maxCapacity: 256,
    minPrice: 4000,
    maxPrice: 11000,
  },
  {
    label: "iPad 舊款標準版（第 7／8 代）",
    matches: (text) => !/(pro|air|mini)/i.test(text) && /(第?\s*[78]\s*代|\b(?:7th|8th)\b)/i.test(text),
    minCapacity: 32,
    maxCapacity: 128,
    minPrice: 1500,
    maxPrice: 4000,
  },
];

// Apple Watch 先判斷系列，再用合法尺寸在該系列價格區間中換算。
const WATCH_PRICES = [
  { label: "Apple Watch Ultra", pattern: /\bultra\b/i, sizes: [49], minPrice: 7000, maxPrice: 9000 },
  { label: "Apple Watch Series 9", pattern: /(?:series\s*)?9\b/i, sizes: [41, 45], minPrice: 4000, maxPrice: 7500 },
  { label: "Apple Watch Series 8", pattern: /(?:series\s*)?8\b/i, sizes: [41, 45], minPrice: 2000, maxPrice: 4000 },
  { label: "Apple Watch Series 7", pattern: /(?:series\s*)?7\b/i, sizes: [41, 45], minPrice: 1500, maxPrice: 3000 },
  { label: "Apple Watch SE 第 2 代", pattern: /\bse\b.*(?:第?\s*2\s*代|2nd)/i, sizes: [40, 44], minPrice: 1200, maxPrice: 2200 },
  { label: "Apple Watch SE 第 1 代", pattern: /\bse\b.*(?:第?\s*1\s*代|1st)/i, sizes: [40, 44], minPrice: 800, maxPrice: 1500 },
];

function parseCapacity(text) {
  // 同時接受 256GB、256G、1TB、1T，並統一換算成 GB。
  const match = text.match(/(\d+(?:\.\d+)?)\s*(tb|t|gb|g)\b/i);
  if (!match) return null;

  const capacity = Number(match[1]);
  return /^t/i.test(match[2]) ? capacity * 1024 : capacity;
}

function parseCondition(appearance) {
  // 支援「9成新」、「九成新」及「80%」等常見填寫方式。
  const chineseGrades = { 十: 10, 九: 9, 八: 8, 七: 7, 六: 6, 五: 5 };
  const chineseMatch = appearance.match(/([十九八七六五])\s*成新/);
  const numericMatch = appearance.match(/(10|[5-9](?:\.\d+)?)\s*成新/i);
  const percentMatch = appearance.match(/(100|9\d|8\d|7\d|6\d|5\d)\s*%/);

  let grade = null;
  if (numericMatch) grade = Number(numericMatch[1]);
  else if (chineseMatch) grade = chineseGrades[chineseMatch[1]];
  else if (percentMatch) grade = Number(percentMatch[1]) / 10;

  if (grade == null) {
    throw new Error("無法辨識外觀成新程度，請在商品外觀填寫例如「9成新」或「80%」。");
  }

  return {
    grade,
    score: Math.min(1, Math.max(0, (grade - 5) / 5)),
    label: `${grade} 成新`,
  };
}

function interpolatePrice(rule, specificationScore, conditionScore) {
  // 規格影響 70%、外觀影響 30%；兩者分數皆限制在 0～1，所以價格不會超出參考區間。
  const score = specificationScore * 0.7 + conditionScore * 0.3;
  const rawPrice = rule.minPrice + (rule.maxPrice - rule.minPrice) * score;
  return Math.round(rawPrice / 100) * 100;
}

function capacityScore(capacity, rule) {
  // 將容量在該型號允許範圍內的位置正規化為 0～1。
  if (capacity < rule.minCapacity || capacity > rule.maxCapacity) {
    throw new Error(
      `容量 ${capacity}GB 超出參考範圍 ${rule.minCapacity}GB～${rule.maxCapacity}GB。`,
    );
  }

  if (rule.minCapacity === rule.maxCapacity) return 0.5;
  return (capacity - rule.minCapacity) / (rule.maxCapacity - rule.minCapacity);
}

function estimateIPhone(text, condition) {
  // category 已確認為 IPHONE，因此也接受省略「iPhone」的輸入，例如「17 Pro 512GB」。
  const modelMatch = text.match(/(?:iphone\s*)?(17|16|15|14|13|12)\b/i);
  if (!modelMatch) {
    throw new Error("無法辨識 iPhone 型號，目前支援 iPhone 12～17。");
  }

  const model = Number(modelMatch[1]);
  const variant = /\bpro(?:\s*max)?\b/i.test(text) ? "pro" : "standard";
  const rule = IPHONE_PRICES[model][variant];
  const capacity = parseCapacity(text);
  if (capacity == null) {
    throw new Error("無法辨識容量，請在商品名稱或描述填寫例如「256GB」。");
  }

  return {
    rule,
    modelLabel: `iPhone ${model}${variant === "pro" ? " Pro 系列" : " 非 Pro 系列"}`,
    specificationLabel: `${capacity}GB`,
    specificationScore: capacityScore(capacity, rule),
    condition,
  };
}

function estimateIPad(text, condition) {
  // category 已確認為 IPAD，因此「Pro M4 256GB」也能辨識。
  const rule = IPAD_PRICES.find((item) => item.matches(text));
  if (!rule) {
    throw new Error("無法辨識 iPad 型號，請填寫 Pro／Air／mini 型號或標準版世代。");
  }

  const capacity = parseCapacity(text);
  if (capacity == null) {
    throw new Error("無法辨識容量，請在商品名稱或描述填寫例如「256GB」。");
  }

  return {
    rule,
    modelLabel: rule.label,
    specificationLabel: `${capacity}GB`,
    specificationScore: capacityScore(capacity, rule),
    condition,
  };
}

function estimateWatch(text, condition) {
  // Watch 的尺寸必須符合該系列價目表，避免把 49mm 套用到一般 Series。
  const rule = WATCH_PRICES.find((item) => item.pattern.test(text));
  if (!rule) {
    throw new Error("無法辨識 Apple Watch 型號，目前支援 Ultra、Series 7～9、SE 第 1／2 代。");
  }

  const sizeMatch = text.match(/\b(40|41|44|45|49)\s*mm\b/i);
  if (!sizeMatch) {
    throw new Error("無法辨識手錶尺寸，請在商品名稱或描述填寫例如「45mm」。");
  }

  const size = Number(sizeMatch[1]);
  const sizeIndex = rule.sizes.indexOf(size);
  if (sizeIndex === -1) {
    throw new Error(`${rule.label} 不支援價目表中的 ${size}mm 尺寸。`);
  }

  const specificationScore =
    rule.sizes.length === 1 ? 0.5 : sizeIndex / (rule.sizes.length - 1);

  return {
    rule,
    modelLabel: rule.label,
    specificationLabel: `${size}mm`,
    specificationScore,
    condition,
  };
}

export function estimateRecyclePrice(application) {
  // 型號、容量或尺寸可寫在商品名稱或描述；成新程度則以外觀欄位為準。
  const text = `${application.productName ?? ""} ${application.description ?? ""}`.trim();
  const appearance = application.appearance ?? "";
  const condition = parseCondition(appearance);

  let estimate;
  if (application.category === "IPHONE") estimate = estimateIPhone(text, condition);
  else if (application.category === "IPAD") estimate = estimateIPad(text, condition);
  else if (application.category === "WATCH") estimate = estimateWatch(text, condition);
  else throw new Error("此商品分類目前不支援自動估價。");

  return {
    price: interpolatePrice(
      estimate.rule,
      estimate.specificationScore,
      condition.score,
    ),
    modelLabel: estimate.modelLabel,
    specificationLabel: estimate.specificationLabel,
    conditionLabel: condition.label,
    minPrice: estimate.rule.minPrice,
    maxPrice: estimate.rule.maxPrice,
  };
}
