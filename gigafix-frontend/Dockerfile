#----編譯(npm build)
#使用node -v確認版本號為24.19.0
FROM node:24.19.0 AS build
WORKDIR /app

#從根目錄複製兩個檔案(在最後的路徑前串接就好，最後一個參數才是要複製到的地方)到/app (./指的是WORKDIR，也就是/app)
COPY package.json package-lock.json ./
#npm ci依據package-lock.json下載(嚴格的版本號)，而且會把整個node_modules刪掉重建，速度較快因為是照著檔案下載，如果是npm i就會自己調整要下載哪個
RUN npm ci
#複製完dependencies後複製靜態檔案到/app(.表示當前目錄，也就是/app)
#如果是COPY <明確目標檔案>的話，後面的image目標檔案就算是在根目錄，也會使用 ./ 而不是 . ，主要是撰寫風格還有可讀性問題(就算效果一樣)
COPY . .
#使用node指令將專案內的vue檔案打包成靜態資源(html,css,script)
RUN npm run build

#----將靜態資源放到web server中實現前後端分離
#使用web server的nginx做前端的資源管理,Nginx版本號跟vue使用的node版本號之間沒有相容性問題，因為vue打包好後都是靜態資源檔案
#使用alpine版本體積會比一般版本小4倍，因為是demo所以先用這個(我不需要編譯原生模組只是要使用nginx的web功能所以沒差)
FROM nginx:1.30-alpine
#--from會讓COPY的檔案來源從本機電腦變成名叫build的容器(上面宣告的node容器就被取名叫build)
#去到build裡面會有剛剛執行npm run build後所產生的dist資料夾，裡面會有專案的靜態資源
#NginX的靜態資源要放在/usr/share/nginx/html才會被讀取
COPY --from=build /app/dist /usr/share/nginx/html
#把根目錄的default.conf複製到/etc/nginx/conf.d/default.conf，這樣自己定義的NginX設定檔才會生效
COPY default.conf /etc/nginx/conf.d/default.conf
