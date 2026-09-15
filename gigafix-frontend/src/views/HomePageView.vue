<script setup>
import repairHeroImage from "@/assets/home/repair-hero.png";

const services = [
  {
    title: "手機維修",
    description: "線上預約，快速掌握維修資訊",
    icon: "bi-tools",
    route: { name: "repair-appointment" },
    action: "預約維修",
  },
  {
    title: "手機回收",
    description: "填寫手機狀況，取得回收估價",
    icon: "bi-recycle",
    route: { name: "recycle-form" },
    action: "立即估價",
  },
  {
    title: "二手機商城",
    description: "嚴選檢測商品，放心挑選",
    icon: "bi-cart3",
    route: { name: "mall-list" },
    action: "前往商城",
  },
  {
    title: "GigaFix 討論區",
    description: "分享經驗，交流手機疑問",
    icon: "bi-chat-dots",
    route: { name: "forumList" },
    action: "瀏覽討論區",
  },
];

const advantages = [
  { title: "標準化檢測", description: "專業流程，檢測更精準", icon: "bi-search" },
  { title: "價格公開透明", description: "清楚報價，無隱藏費用", icon: "bi-coin" },
  { title: "會員進度查詢", description: "維修、回收進度即時掌握", icon: "bi-card-checklist" },
  { title: "維修紀錄保存", description: "完整紀錄，售後更有保障", icon: "bi-shield-check" },
];
</script>

<template>
  <div class="home-page">
    <section class="hero-section">
      <div class="hero-copy">
        <p class="hero-eyebrow">手機的每一段旅程，GigaFix 都在</p>
        <h1>維修、回收、選購，<br />一站完成</h1>
        <p class="hero-description">
          專業維修、透明估價、安心二手機與經驗交流，讓手機服務更簡單。
        </p>

        <!-- 首屏放兩個最高頻的入口，使用者不必往下滑就能預約或查價。 -->
        <div class="hero-actions">
          <RouterLink :to="{ name: 'repair-appointment' }" class="primary-action">
            立即預約維修
            <i class="bi bi-arrow-right" aria-hidden="true"></i>
          </RouterLink>
          <RouterLink :to="{ name: 'repair-price-reference' }" class="secondary-action">
            查看維修報價
          </RouterLink>
        </div>

        <ul class="hero-trust-list" aria-label="服務特色">
          <li><i class="bi bi-shield-check"></i>專業檢測</li>
          <li><i class="bi bi-receipt"></i>透明報價</li>
          <li><i class="bi bi-hand-thumbs-up"></i>售後保障</li>
        </ul>
      </div>

      <div class="hero-visual">
        <img :src="repairHeroImage" alt="GigaFix 維修人員正在檢測手機" />
        <div class="hero-note">
          <i class="bi bi-patch-check-fill" aria-hidden="true"></i>
          <span><strong>安心維修</strong>檢測後再確認報價</span>
        </div>
      </div>
    </section>

    <section class="services-section" aria-labelledby="services-title">
      <div class="section-heading">
        <p>OUR SERVICES</p>
        <h2 id="services-title">GigaFix 四大服務</h2>
        <span>從維修到交流，找到你需要的手機服務</span>
      </div>

      <!-- 四張卡片各自連到現有模組的具名路由，調整網址時較不易影響首頁。 -->
      <div class="service-grid">
        <article v-for="service in services" :key="service.title" class="service-card">
          <div class="service-icon" aria-hidden="true">
            <i class="bi" :class="service.icon"></i>
          </div>
          <h3>{{ service.title }}</h3>
          <p>{{ service.description }}</p>
          <RouterLink :to="service.route" class="service-link">
            {{ service.action }}
            <i class="bi bi-arrow-right" aria-hidden="true"></i>
          </RouterLink>
        </article>
      </div>
    </section>

    <section class="advantages-section" aria-label="GigaFix 服務優勢">
      <div v-for="advantage in advantages" :key="advantage.title" class="advantage-item">
        <i class="bi advantage-icon" :class="advantage.icon" aria-hidden="true"></i>
        <div>
          <h3>{{ advantage.title }}</h3>
          <p>{{ advantage.description }}</p>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.home-page {
  --brand-blue: #2b77c5;
  --brand-blue-dark: #1e3557;
  --brand-blue-soft: #edf5fc;
  --text-main: #1d324b;
  --text-muted: #66788b;
  overflow: hidden;
  color: var(--text-main);
  background: #ffffff;
}

.hero-section {
  display: grid;
  grid-template-columns: minmax(0, 0.93fr) minmax(520px, 1.07fr);
  min-height: 520px;
  background: linear-gradient(90deg, #f5f9fd 0%, #f5f9fd 43%, #ffffff 67%);
}

.hero-copy {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 64px clamp(32px, 5.5vw, 104px);
  position: relative;
  z-index: 2;
}

.hero-eyebrow,
.section-heading > p {
  margin: 0 0 14px;
  color: var(--brand-blue);
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.hero-copy h1 {
  margin: 0;
  color: #122d49;
  font-size: clamp(42px, 4.2vw, 68px);
  line-height: 1.18;
  font-weight: 800;
  letter-spacing: -0.035em;
}

.hero-description {
  max-width: 610px;
  margin: 24px 0 0;
  color: #5b6f82;
  font-size: 19px;
  line-height: 1.8;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 32px;
}

.primary-action,
.secondary-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 176px;
  min-height: 52px;
  padding: 12px 24px;
  border: 2px solid var(--brand-blue);
  border-radius: 6px;
  font-size: 17px;
  font-weight: 700;
  text-decoration: none;
  transition: transform 0.2s ease, box-shadow 0.2s ease, background-color 0.2s ease;
}

.primary-action { gap: 12px; color: #ffffff; background: var(--brand-blue); }
.secondary-action { color: var(--brand-blue); background: #ffffff; }

.primary-action:hover,
.secondary-action:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 24px rgba(43, 119, 197, 0.17);
}

.secondary-action:hover { color: #ffffff; background: var(--brand-blue); }

.hero-trust-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0;
  padding: 0;
  margin: 38px 0 0;
  list-style: none;
}

.hero-trust-list li {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 22px;
  color: #344b62;
  font-size: 15px;
  font-weight: 600;
  border-right: 1px solid #cfdae4;
}

.hero-trust-list li:first-child { padding-left: 0; }
.hero-trust-list li:last-child { border-right: 0; }
.hero-trust-list i { color: var(--brand-blue); font-size: 23px; }

.hero-visual {
  min-height: 520px;
  position: relative;
  overflow: hidden;
}

.hero-visual::before {
  position: absolute;
  inset: 0 auto 0 0;
  z-index: 1;
  width: 20%;
  content: "";
  background: linear-gradient(90deg, #f5f9fd 0%, rgba(245, 249, 253, 0) 100%);
}

.hero-visual img {
  width: 100%;
  height: 100%;
  min-height: 520px;
  display: block;
  object-fit: cover;
  object-position: 55% center;
}

.hero-note {
  position: absolute;
  right: clamp(22px, 3vw, 52px);
  bottom: 30px;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 13px 18px;
  border: 1px solid rgba(255, 255, 255, 0.88);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 12px 28px rgba(30, 53, 87, 0.16);
  backdrop-filter: blur(8px);
}

.hero-note i { color: var(--brand-blue); font-size: 26px; }
.hero-note span { display: flex; flex-direction: column; color: #68798a; font-size: 13px; }
.hero-note strong { margin-bottom: 1px; color: var(--text-main); font-size: 15px; }

.services-section {
  max-width: 1500px;
  margin: 0 auto;
  padding: 68px clamp(24px, 4vw, 64px) 76px;
}

.section-heading { margin-bottom: 38px; text-align: center; }
.section-heading > p { margin-bottom: 7px; font-size: 13px; letter-spacing: 0.18em; }
.section-heading h2 { margin: 0; color: #16314d; font-size: 36px; font-weight: 800; }
.section-heading span { display: block; margin-top: 10px; color: var(--text-muted); font-size: 17px; }

.service-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 22px;
}

.service-card {
  display: flex;
  min-height: 260px;
  flex-direction: column;
  align-items: flex-start;
  padding: 30px;
  border: 1px solid #dde6ee;
  border-radius: 10px;
  background: #ffffff;
  box-shadow: 0 8px 24px rgba(30, 53, 87, 0.06);
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.service-card:hover {
  transform: translateY(-5px);
  border-color: #a9c9e9;
  box-shadow: 0 16px 34px rgba(30, 53, 87, 0.1);
}

.service-icon {
  display: inline-flex;
  width: 58px;
  height: 58px;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: var(--brand-blue);
  background: var(--brand-blue-soft);
  font-size: 27px;
}

.service-card h3 { margin: 22px 0 8px; color: #172f48; font-size: 22px; font-weight: 750; }
.service-card p { margin: 0 0 24px; color: var(--text-muted); font-size: 15px; line-height: 1.65; }

.service-link {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin-top: auto;
  color: var(--brand-blue);
  font-size: 16px;
  font-weight: 700;
  text-decoration: none;
}

.service-link i { transition: transform 0.2s ease; }
.service-link:hover i { transform: translateX(4px); }

.advantages-section {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0;
  padding: 38px max(5vw, calc((100vw - 1420px) / 2));
  border-top: 1px solid #dde6ee;
  background: #f7fafc;
}

.advantage-item {
  display: flex;
  align-items: center;
  gap: 18px;
  min-width: 0;
  padding: 6px 34px;
  border-right: 1px solid #d5e0e9;
}

.advantage-item:last-child { border-right: 0; }
.advantage-icon { flex: 0 0 auto; color: var(--brand-blue); font-size: 34px; }
.advantage-item h3 { margin: 0 0 5px; color: #203951; font-size: 17px; font-weight: 750; }
.advantage-item p { margin: 0; color: #788898; font-size: 13px; line-height: 1.5; }

@media (max-width: 1180px) {
  .hero-section { grid-template-columns: 1fr 1fr; }
  .hero-copy { padding: 54px 36px; }
  .service-grid, .advantages-section { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .advantage-item:nth-child(2) { border-right: 0; }
  .advantage-item:nth-child(-n + 2) { padding-bottom: 25px; margin-bottom: 25px; border-bottom: 1px solid #d5e0e9; }
}

@media (max-width: 820px) {
  .hero-section { grid-template-columns: 1fr; }
  .hero-copy { padding: 54px 24px 44px; }
  .hero-visual, .hero-visual img { min-height: 390px; }
  .hero-visual::before {
    inset: 0 0 auto;
    width: 100%;
    height: 22%;
    background: linear-gradient(180deg, #f5f9fd 0%, rgba(245, 249, 253, 0) 100%);
  }
  .services-section { padding-top: 54px; }
}

@media (max-width: 620px) {
  .hero-copy h1 { font-size: 40px; }
  .hero-description { font-size: 17px; }
  .hero-actions, .primary-action, .secondary-action { width: 100%; }
  .hero-trust-list { row-gap: 16px; }
  .hero-trust-list li { width: 50%; padding: 0; border-right: 0; }
  .service-grid, .advantages-section { grid-template-columns: 1fr; }
  .service-card { min-height: 240px; }
  .advantages-section { padding: 30px 24px; }
  .advantage-item, .advantage-item:nth-child(-n + 2) {
    padding: 22px 0;
    margin: 0;
    border-right: 0;
    border-bottom: 1px solid #d5e0e9;
  }
  .advantage-item:last-child { border-bottom: 0; }
}
</style>
