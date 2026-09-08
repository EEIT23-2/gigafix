# ---- Stage 1: 編譯 ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# 先只複製mvnw與pom.xml，讓相依套件下載可以被 Docker layer cache 起來
# 之後只要pom.xml 沒變，重複 build 就不用重新下載整個 Maven repo(如果先複製src的話，只要src有變動就要整包重新下載)

#複製Maven Wrapper到/app，Maven Wrapper是去maven下載dependencies的腳本
#因為mvn本身的版本號是依照每個主機電腦環境會有不同版本，可能下載dependencies執行上每台電腦會有落差(但使用image不該有落差)
#但mvnw會依據.mvn下載對應版本的mvn，同樣版本的mvn跑一樣的.mvn下載的dependencies一定一樣
COPY mvnw ./
#.mvn 資料夾內有 mvnw 需要參照的設定(包含要下載哪個版本的Maven和dependencies
COPY .mvn .mvn
#下載pom.xml，因為無論是mvn或是mvnw都是依照pom.xml去執行
COPY pom.xml ./
#chmod +x mvnw會給予mvnw下載的執行權限，用安靜跟批次模式將所有dependencies下載到/.m2
#使用mvn下指令會使用到每台電腦預設的mvn，要用./mvnw才會使用剛剛上面下載的固定版本mvn去複製dependencies
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline

# 複製原始碼並打包（先跳過測試，測試請在CI或本機另外跑），可以寫成 COPY src/ ./src/
COPY src src
# 沿用前面下載的mvn -q -B去執行刪除target目錄，然後執行phase到打包maven檔案(-DskipTests選項跳過測試)
RUN ./mvnw -q -B clean package -DskipTests

# ---- Stage 2: 執行期 ----
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
#從剛剛創建的build容器中複製/app/target/下面所有.jar到runtime的app.jar
COPY --from=build /app/target/*.jar app.jar
#容器預期開放8080 port
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]