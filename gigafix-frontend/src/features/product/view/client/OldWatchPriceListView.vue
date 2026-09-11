<script setup>
import priceListImage from "@/assets/jack/oldWatchPriceList.jpg";
</script>

<template>
  <main class="price-list-page">
    <figure class="price-list-card">
      <div class="price-list-scroll">
        <img
          class="price-list-image"
          :src="priceListImage"
          alt="Apple Watch 二手回收價參考對照表"
        />
      </div>
      <figcaption>實際收購價格將依裝置現場檢測結果為準。</figcaption>
    </figure>
  </main>
</template>

<style scoped>
.price-list-page {
  width: 100%;
  color: #1b1b1b;
  font-family: "Hanken Grotesk", "Noto Sans TC", sans-serif;
}

.price-list-card {
  margin: 0;
  overflow: hidden;
  border: 1px solid #eeeeee;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 24px 60px -36px rgba(0, 0, 0, 0.28);
}

.price-list-scroll {
  overflow-x: auto;
}

.price-list-image {
  display: block;
  width: 100%;
  height: auto;
}

figcaption {
  padding: 16px 24px;
  border-top: 1px solid #eeeeee;
  color: #635d5e;
  font-size: 14px;
  text-align: center;
}

@media (max-width: 767.98px) {
  .price-list-card {
    border-radius: 14px;
  }

  .price-list-image {
    width: 760px;
    max-width: none;
  }
}
</style>
