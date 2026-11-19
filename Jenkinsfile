pipeline {
    agent any

    environment {
        TARGET_HOST = 'root@192.168.11.110'
        REMOTE_JAR  = '/opt/meocare/meocare.jar'
    }

    stages {
        stage('Build') {
            steps {
                dir('meocare') {
                    sh 'mvn clean package -DskipTests'
                }
            }
        }

        stage('Deploy') {
            steps {
                sh """
                    scp -o BatchMode=yes -o StrictHostKeyChecking=no meocare/target/*.jar ${TARGET_HOST}:${REMOTE_JAR}
                    ssh -o BatchMode=yes -o StrictHostKeyChecking=no ${TARGET_HOST} "systemctl restart meocare"
                """
            }
        }
    }
}