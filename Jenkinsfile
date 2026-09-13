pipeline {
    agent any

    environment {
        DOCKERHUB_NAMESPACE   = 'yoyopoke'
        BACKEND_IMAGE         = "${DOCKERHUB_NAMESPACE}/gigafix-backend"
        FRONTEND_IMAGE        = "${DOCKERHUB_NAMESPACE}/gigafix-frontend"
        //BUILD_NUMBER(該資料build過幾次)是 Jenkins 系統本身自動維護的計數器， Jenkins 幫算好、自動塞進 env 這個全域變數裡
        IMAGE_TAG              = "${env.BUILD_NUMBER}"
        DOCKERHUB_CREDENTIALS  = credentials('gigafix') //會從jenkins裡面抓這個id的帳密出來

        //以下是部署後端到 Azure Container Apps 用的設定，前端交給 Vercel 自己接 GitHub 部署，這裡不用管
        AZURE_CREDENTIALS      = credentials('azure-sp') //Jenkins 裡設定的 Azure Service Principal，username=clientId、password=clientSecret
        AZURE_TENANT_ID        = credentials('azure-tenant-id') //Secret Text 類型的 credential，存 Azure AD 的 tenant id
        AZURE_SUBSCRIPTION_ID  = credentials('azure-subscription-id') //Secret Text 類型的 credential，存要部署的 Azure 訂閱 id

        AZURE_RESOURCE_GROUP   = 'Gigafix' //後端 Container App 所在的 resource group 名稱
        AZURE_CONTAINERAPP_NAME = 'gigafix' //後端 Container App 實際的名稱
    }

    stages { //stage的外層容器,整個 pipeline只會出現一次
        //stages內的stage會依照順序一個接一個跑
        stage('Checkout') { //stage是steps的外層容器，裡面會有許多步驟，()內是這個stage的名稱
            steps {
                checkout scm //把你的 git repo 程式碼抓下來、放進這次 build 的工作目錄(git clone / git pull)
            }
        }

        stage('Build backend image') { //build 後端的image
            steps {
                dir('gigafix-backend') { //設定工作目錄在後端的跟資料夾
                    //sh用來在pipeline裡面執行shell指令(平時在cmd執行的docker指令放在這裡)，後面的指令要用"包起來才可以使用${}加入變數
                    sh "docker build -t ${BACKEND_IMAGE}:${IMAGE_TAG} -t ${BACKEND_IMAGE}:latest ." //build後端程式碼的image
                }
            }
        }

        stage('Build frontend image') { //前端正式環境是 Vercel 直接從 GitHub build/deploy，這裡 build 的 image 只給本機 docker compose 測試用
            steps {
                dir('gigafix-frontend') {
                    sh "docker build -t ${FRONTEND_IMAGE}:${IMAGE_TAG} -t ${FRONTEND_IMAGE}:latest ." //build後端程式碼的image
                }
            }
        }

        stage('Push images') {
            steps {
                //透過''' '''可以在範圍內輸入多行指令，以下指令是直接用擁有jenkins權限的帳號變數登入
                //echo ... | ... 會把前面要印出來的東西標準輸出給後面的指令(不印出來)，後面的指令是登入，並透過--password-stdin接前面的輸出內容
                sh '''
                    echo "$DOCKERHUB_CREDENTIALS_PSW" | docker login -u "$DOCKERHUB_CREDENTIALS_USR" --password-stdin
                '''
                //我所有image推到docker hub上
                sh "docker push ${BACKEND_IMAGE}:${IMAGE_TAG}"
                sh "docker push ${BACKEND_IMAGE}:latest"
                sh "docker push ${FRONTEND_IMAGE}:${IMAGE_TAG}"
                sh "docker push ${FRONTEND_IMAGE}:latest"
            }
        }

        stage('Confirm deploy to Azure') { //Jenkins job 在網頁上設定只在 main 有變動時才會 build，所以這裡不用再另外判斷分支
            steps {
                //手動確認關卡：image 已經 push 到 DockerHub 了，但 main 上可能有 PR 合併進來、還沒被人工驗證過真的沒問題
                //所以不要讓它自動流到正式環境，這裡先暫停 pipeline，等有人按下去才會繼續往下部署 Azure
                input message: "要把 ${BACKEND_IMAGE}:${IMAGE_TAG} 部署到 Azure Container App (${AZURE_CONTAINERAPP_NAME}) 嗎？", ok: '確認部署'
            }
        }

        stage('Deploy backend to Azure') { //把剛剛 push 上去的後端 image 部署到 Azure Container Apps，資料庫是另外用 Azure SQL Database，這裡不用管
            steps {
                sh '''
                    az login \
                        --service-principal \
                        -u "$AZURE_CREDENTIALS_USR" \
                        -p "$AZURE_CREDENTIALS_PSW" \
                        --tenant "$AZURE_TENANT_ID"
                    az account set --subscription "$AZURE_SUBSCRIPTION_ID"
                    az containerapp update \
                        --name "$AZURE_CONTAINERAPP_NAME" \
                        --resource-group "$AZURE_RESOURCE_GROUP" \
                        --image "${BACKEND_IMAGE}:${IMAGE_TAG}"
                '''
            }
        }
    }

    post { //類似finally，不管前面怎麼會做的事情
        //success：只有整個 pipeline 成功才執行
        //failure：只有失敗才執行
        //unstable：build 標記為不穩定時執行（例如測試有失敗但沒讓整個 build 掛掉）
        always { //不管成功、失敗、被中止，一定執行
            sh 'docker logout || true'
            sh 'az logout || true'
        }
    }
}
