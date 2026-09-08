import "./assets/main.css";

import { createApp } from "vue";
import { createPinia } from "pinia";
import vue3GoogleLogin from "vue3-google-login";

import App from "./App.vue";
import router from "./router";

//jack載入bootstrap全站css
import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap/dist/js/bootstrap.bundle.min.js";
//yoyo也要載入bootstrap的icon
import "bootstrap-icons/font/bootstrap-icons.css";

const app = createApp(App);

app.use(createPinia());
app.use(router);
//全域註冊Google第三方登入用的<GoogleLogin>元件，順便帶入client id
app.use(vue3GoogleLogin, {
  clientId: import.meta.env.VITE_GOOGLE_CLIENT_ID,
});

app.mount("#app");
