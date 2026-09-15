<script setup>
const beliefs = [
  {
    title: "清楚透明",
    description: "重要的價格、狀態與流程，都應該讓使用者容易理解。",
  },
  {
    title: "認真負責",
    description: "從提出需求到完成服務，重視每一個會影響選擇的細節。",
  },
  {
    title: "延續價值",
    description: "讓能修好的手機繼續使用，也讓閒置設備獲得合適的去處。",
  },
];

const teachers = [
  { subject: "Java", name: "Vincent 蔡昇達老師" },
  { subject: "Vue.js", name: "Ben 陳冠竹老師" },
  { subject: "前後端串接、AI", name: "洪子敬老師" },
  { subject: "SQL", name: "郭惠民老師" },
  { subject: "JavaScript、Hibernate", name: "Gary 李建輝老師" },
  { subject: "職涯講座", name: "林昭吟老師" },
  { subject: "Spring、RESTful & Ajax", name: "陳奕兆老師" },
  { subject: "HTML、Git、Docker", name: "王裕繽老師" },
  { subject: "教務", name: "林小姐" },
];
</script>

<template>
  <div class="about-page">
    <section class="about-hero" aria-labelledby="about-title">
      <div class="about-container hero-content">
        <div class="hero-copy">
          <p class="section-label">ABOUT GIGAFIX</p>
          <h1 id="about-title">關於 GigaFix</h1>
          <p class="hero-lead">把複雜的手機服務，整理成讓人放心的選擇。</p>
          <p class="hero-description">
            我們從維修與二手機使用經驗出發，希望讓每一個需要修理、汰換或重新選購手機的人，
            都能更清楚地理解流程，也更安心地做決定。
          </p>
        </div>

        <div class="hero-wordmark" aria-hidden="true">
          <span>GIGAFIX</span>
          <p>更清楚的資訊<br />更安心的選擇</p>
        </div>
      </div>
    </section>

    <section class="story-section" aria-labelledby="story-title">
      <div class="about-container numbered-section">
        <div class="section-number" aria-hidden="true">01</div>
        <div class="section-divider" aria-hidden="true"></div>
        <div class="section-content">
          <p class="section-label">OUR STORY</p>
          <h2 id="story-title">從一次次手機問題，看見更好的可能</h2>
          <p>
            手機已經成為生活的一部分，故障或汰換時，卻常伴隨價格不清楚、流程難理解，
            以及舊機不知道該如何處理的困擾。GigaFix 因此誕生。我們把課堂所學化為實際專題，
            重新整理維修、回收與二手機選購的過程，希望資訊能說得明白，服務也能更貼近真正的需要。
          </p>
          <p>
            這個網站記錄的不只是功能開發，也包含我們對使用經驗、服務流程與資源再利用的思考。
          </p>
        </div>
      </div>
    </section>

    <section class="belief-section" aria-labelledby="belief-title">
      <div class="about-container">
        <div class="numbered-section belief-heading">
          <div class="section-number" aria-hidden="true">02</div>
          <div class="section-divider" aria-hidden="true"></div>
          <div class="section-content">
            <p class="section-label">OUR BELIEF</p>
            <h2 id="belief-title">清楚、負責，讓價值繼續</h2>
          </div>
        </div>

        <div class="belief-list">
          <article v-for="belief in beliefs" :key="belief.title" class="belief-item">
            <h3>{{ belief.title }}</h3>
            <p>{{ belief.description }}</p>
          </article>
        </div>

        <p class="service-summary">
          GigaFix 串連維修、回收、商城與交流，陪使用者找到適合自己的下一步。
        </p>
      </div>
    </section>

    <section class="thanks-section" aria-labelledby="thanks-title">
      <div class="thanks-decoration thanks-decoration-left" aria-hidden="true"></div>
      <div class="thanks-decoration thanks-decoration-right" aria-hidden="true"></div>

      <div class="about-container thanks-content">
        <header class="thanks-heading">
          <p class="section-label">SPECIAL THANKS</p>
          <h2 id="thanks-title">特別感謝以下的指導老師</h2>
          <p>謝謝每一位老師在學習與專題製作過程中的指導與陪伴。</p>
        </header>

        <div class="teacher-list">
          <article v-for="teacher in teachers" :key="teacher.subject" class="teacher-item">
            <p>{{ teacher.subject }}</p>
            <h3>{{ teacher.name }}</h3>
          </article>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.about-page {
  --brand-blue: #2b77c5;
  --brand-navy: #17375e;
  --text-main: #183653;
  --text-muted: #566f88;
  overflow: hidden;
  color: var(--text-main);
  background: #ffffff;
}

.about-container {
  width: min(100% - 48px, 1420px);
  margin: 0 auto;
}

.about-hero {
  position: relative;
  background:
    radial-gradient(circle at 96% -18%, rgba(43, 119, 197, 0.12) 0 18%, transparent 18.2%),
    linear-gradient(105deg, #ffffff 0%, #f5faff 58%, #eaf5ff 100%);
}

.hero-content {
  display: grid;
  min-height: 420px;
  grid-template-columns: minmax(0, 1fr) minmax(420px, 0.9fr);
  align-items: center;
  gap: 70px;
  padding-top: 72px;
  padding-bottom: 72px;
}

.section-label {
  margin: 0 0 10px;
  color: var(--brand-blue);
  font-size: 13px;
  font-weight: 750;
  letter-spacing: 0.13em;
}

.hero-copy h1 {
  margin: 0;
  color: #102f50;
  font-size: clamp(48px, 5vw, 74px);
  font-weight: 800;
  line-height: 1.08;
  letter-spacing: -0.035em;
}

.hero-lead {
  margin: 20px 0 0;
  color: var(--brand-navy);
  font-size: clamp(21px, 2vw, 29px);
  font-weight: 700;
  line-height: 1.45;
}

.hero-description {
  max-width: 690px;
  margin: 18px 0 0;
  color: var(--text-muted);
  font-size: 17px;
  line-height: 1.9;
}

.hero-wordmark {
  position: relative;
  min-height: 210px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.hero-wordmark span {
  color: rgba(43, 119, 197, 0.09);
  font-size: clamp(88px, 10vw, 172px);
  font-weight: 900;
  line-height: 1;
  letter-spacing: -0.075em;
  white-space: nowrap;
}

.hero-wordmark p {
  position: absolute;
  right: 10px;
  bottom: 0;
  margin: 0;
  padding-bottom: 13px;
  border-bottom: 3px solid var(--brand-blue);
  color: #315779;
  font-size: 16px;
  line-height: 1.55;
  letter-spacing: 0.1em;
}

.story-section {
  padding: 72px 0 78px;
}

.numbered-section {
  display: grid;
  grid-template-columns: 110px 1px minmax(0, 1fr);
  align-items: start;
  gap: 36px;
}

.section-number {
  color: #c8e4fa;
  font-size: 68px;
  font-weight: 800;
  line-height: 1;
}

.section-divider {
  width: 1px;
  height: 64px;
  background: #8fc7ee;
}

.section-content h2 {
  margin: 0 0 22px;
  color: #102f50;
  font-size: clamp(30px, 3vw, 43px);
  font-weight: 800;
  line-height: 1.3;
  letter-spacing: -0.02em;
}

.section-content > p:not(.section-label) {
  max-width: 1080px;
  margin: 0 0 16px;
  color: var(--text-muted);
  font-size: 17px;
  line-height: 1.9;
}

.belief-section {
  padding: 62px 0 38px;
  background: linear-gradient(110deg, #f3faff 0%, #eaf6ff 52%, #f7fbff 100%);
}

.belief-heading {
  margin-bottom: 28px;
}

.belief-heading .section-content h2 {
  margin-bottom: 0;
}

.belief-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-top: 18px;
}

.belief-item {
  padding: 12px 56px 22px;
  text-align: center;
  border-right: 1px solid #a9d2ef;
}

.belief-item:last-child {
  border-right: 0;
}

.belief-item h3 {
  margin: 0 0 12px;
  color: #153b62;
  font-size: 22px;
  font-weight: 800;
}

.belief-item p {
  margin: 0;
  color: var(--text-muted);
  font-size: 16px;
  line-height: 1.75;
}

.service-summary {
  margin: 26px 0 0;
  padding: 22px 20px 2px;
  border-top: 1px solid #a9d2ef;
  color: #315979;
  font-size: 16px;
  text-align: center;
}

.thanks-section {
  position: relative;
  padding: 78px 0 88px;
  background: linear-gradient(180deg, #f8fcff 0%, #edf8ff 100%);
}

.thanks-decoration {
  position: absolute;
  width: 260px;
  height: 260px;
  border-radius: 50%;
  background: rgba(43, 119, 197, 0.055);
  pointer-events: none;
}

.thanks-decoration-left {
  top: -155px;
  left: -120px;
}

.thanks-decoration-right {
  top: -165px;
  right: -100px;
}

.thanks-content {
  position: relative;
  z-index: 1;
}

.thanks-heading {
  margin-bottom: 48px;
  text-align: center;
}

.thanks-heading h2 {
  margin: 0;
  color: #102f50;
  font-size: clamp(36px, 4vw, 58px);
  font-weight: 800;
  letter-spacing: -0.035em;
}

.thanks-heading > p:last-child {
  margin: 15px 0 0;
  color: var(--text-muted);
  font-size: 17px;
}

.teacher-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  column-gap: 48px;
}

.teacher-item {
  min-height: 112px;
  padding: 21px 10px 24px;
  border-bottom: 1px solid #acd2ed;
}

.teacher-item:nth-last-child(-n + 3) {
  border-bottom: 0;
}

.teacher-item p {
  margin: 0 0 8px;
  color: var(--brand-blue);
  font-size: 14px;
  font-weight: 650;
}

.teacher-item h3 {
  margin: 0;
  color: #14375a;
  font-size: clamp(19px, 1.7vw, 25px);
  font-weight: 800;
  line-height: 1.4;
}

@media (max-width: 1050px) {
  .hero-content {
    grid-template-columns: minmax(0, 1fr) 360px;
    gap: 28px;
  }

  .numbered-section {
    grid-template-columns: 82px 1px minmax(0, 1fr);
    gap: 26px;
  }

  .belief-item {
    padding-right: 28px;
    padding-left: 28px;
  }
}

@media (max-width: 760px) {
  .about-container {
    width: min(100% - 36px, 1420px);
  }

  .hero-content {
    min-height: auto;
    grid-template-columns: 1fr;
    padding-top: 56px;
    padding-bottom: 48px;
  }

  .hero-wordmark {
    min-height: 110px;
    justify-content: flex-start;
    overflow: hidden;
  }

  .hero-wordmark p {
    right: 0;
  }

  .story-section {
    padding: 56px 0;
  }

  .numbered-section {
    grid-template-columns: 70px minmax(0, 1fr);
    gap: 18px;
  }

  .section-divider {
    display: none;
  }

  .section-number {
    font-size: 52px;
  }

  .belief-list,
  .teacher-list {
    grid-template-columns: 1fr;
  }

  .belief-item {
    padding: 24px 10px;
    border-right: 0;
    border-bottom: 1px solid #a9d2ef;
  }

  .belief-item:last-child {
    border-bottom: 0;
  }

  .thanks-section {
    padding: 62px 0;
  }

  .thanks-heading {
    margin-bottom: 28px;
  }

  .teacher-item,
  .teacher-item:nth-last-child(-n + 3) {
    min-height: auto;
    padding: 20px 4px;
    border-bottom: 1px solid #acd2ed;
  }

  .teacher-item:last-child {
    border-bottom: 0;
  }
}

@media (max-width: 480px) {
  .hero-copy h1 {
    font-size: 44px;
  }

  .hero-wordmark span {
    font-size: 82px;
  }

  .numbered-section {
    grid-template-columns: 1fr;
  }

  .section-number {
    margin-bottom: -6px;
    font-size: 42px;
  }
}
</style>
