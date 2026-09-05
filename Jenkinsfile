pipeline {
    agent any

    environment {
        DOCKERHUB_NAMESPACE   = 'yoyopoke'
        BACKEND_IMAGE         = "${DOCKERHUB_NAMESPACE}/gigafix-backend"
        FRONTEND_IMAGE        = "${DOCKERHUB_NAMESPACE}/gigafix-frontend"
        //BUILD_NUMBER(該資料build過幾次)是 Jenkins 系統本身自動維護的計數器， Jenkins 幫算好、自動塞進 env 這個全域變數裡
        IMAGE_TAG              = "${env.BUILD_NUMBER}"
        DOCKERHUB_CREDENTIALS  = credentials('gigafix') //會從jenkins裡面抓這個id的帳密出來
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

        stage('Build frontend image') {
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
    }

    post { //類似finally，不管前面怎麼會做的事情
        //success：只有整個 pipeline 成功才執行
        //failure：只有失敗才執行
        //unstable：build 標記為不穩定時執行（例如測試有失敗但沒讓整個 build 掛掉）
        always { //不管成功、失敗、被中止，一定執行
            sh 'docker logout || true'
        }
    }
}
