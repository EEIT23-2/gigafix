import "./assets/main.css";

import { createApp } from "vue";
import { createPinia } from "pinia";
import vue3GoogleLogin from "vue3-google-login";

import App from "./App.vue";
import router from "./router";
import { useFetchAdminInfoStore } from "./stores/admin";

//jack載入bootstrap全站css
import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap/dist/js/bootstrap.bundle.min.js";
//yoyo也要載入bootstrap的icon
import "bootstrap-icons/font/bootstrap-icons.css";

const app = createApp(App);

app.use(createPinia());

//要裝在createPinia()之後，才能拿到store實例；裝在router之前/之後都可以，攔截器本身在request發生時才會執行
useFetchAdminInfoStore().installSessionExpiredInterceptor(router);
app.use(router);
//全域註冊Google第三方登入用的<GoogleLogin>元件，順便帶入client id
//Client ID不是機密(本來就會出現在登入請求網址上)，直接寫死不用靠環境變數
app.use(vue3GoogleLogin, {
  clientId: "264337878838-2ucbbheve8obmf0qoihqbl776alqmijb.apps.googleusercontent.com",
});

app.mount("#app");
