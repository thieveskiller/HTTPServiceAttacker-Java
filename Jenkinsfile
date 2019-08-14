pipeline {
    agent any
    
    stages {
        stage('Maven build') {
            steps {
                sh 'mvn package'
            }
        }
    }
    
    post {
        always {
            archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
        }
    }
}