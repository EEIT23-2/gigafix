// demo/測試用：前台表單「一鍵輸入資料」要填進去的示範內容。
// 每種表單固定一組，按幾次結果都一樣，方便反覆展示與測試。
//
// 內文 HTML 只能用「TipTap schema ∩ 前台 DOMPurify 白名單 ∩ 後端 jsoup」三方都認得的標籤，
// 少一方認得就會在畫面上或存檔時被吃掉。安全集合：
//   p, h4, hr, strong, em, s, u, ul, ol, li, a[href 絕對網址],
//   span[style 只有 color / background-color / font-size 8~32px]
// 另外標籤之間不要換行——文章內容的容器是 white-space: pre-wrap，換行會變成看得見的空行。

export const DEMO_ARTICLE = {
  title: 'iPhone 充電變慢的三個常見原因',
  content:
    '<p>最近手機充電越來越慢，查了一輪之後發現原因通常不在手機本身，整理成三點給大家參考。</p>' +
    '<h4>一、充電孔卡棉絮</h4>' +
    '<p>長期放口袋最容易累積，用手電筒照一下就看得出來。清的時候<strong>務必先關機</strong>，用木製或塑膠牙籤輕輕勾，不要拿金屬針刮接點。</p>' +
    '<h4>二、線材或充電頭老化</h4>' +
    '<p>最快的判斷方式是交叉測試：換一條線、換一個頭、換一個插座，<em>一次只換一個變因</em>。接頭附近外皮裂開的線就別再用了。</p>' +
    '<h4>三、電池健康度下降</h4>' +
    '<p>健康度掉到 80% 以下時，充電速度與續航都會明顯變差，這時候換電池比換手機划算得多。</p>' +
    '<hr>' +
    '<p><span style="color: #c0392b">提醒：邊充邊玩遊戲造成的高溫，是電池老化最主要的兇手。</span></p>',
  coverImage: 'https://images.unsplash.com/photo-1695482482959-937db8e81cef?w=1200&auto=format&fit=crop&q=70',
}

export const DEMO_FLOOR =
  '<p>補充一個我自己的經驗：充電孔清乾淨之後，充電速度就回來了，完全不用送修。</p>' +
  '<p>另外<strong>換線之前先借別條線測試</strong>，可以少花很多冤枉錢。</p>'

export const DEMO_COMMENT =
  '感謝分享！我原本以為是手機壞了，照著第一點清了充電孔之後真的正常了，省下一筆維修費。'
