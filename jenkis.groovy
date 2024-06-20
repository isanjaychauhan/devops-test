pipeline {
    agent any

    environment {
        MYSQL_HOST = 'db'
        MYSQL_USER = 'user'
        MYSQL_PASSWORD = 'password'
        MYSQL_DATABASE = 'sample_app'
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'master', url: 'https://github.com/your-repo/sample-php-app.git'
            }
        }
        stage('Build') {
            steps {
                script {
                    def dockerCompose = """
                    version: '3.8'

                    services:
                      web:
                        image: php:7.4-apache
                        container_name: php_web
                        volumes:
                          - ./src:/var/www/html
                        ports:
                          - "8080:80"
                        depends_on:
                          - db
                        environment:
                          - MYSQL_HOST=db
                          - MYSQL_USER=${MYSQL_USER}
                          - MYSQL_PASSWORD=${MYSQL_PASSWORD}
                          - MYSQL_DATABASE=${MYSQL_DATABASE}

                      db:
                        image: mysql:5.7
                        container_name: mysql_db
                        environment:
                          MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
                          MYSQL_DATABASE: ${MYSQL_DATABASE}
                          MYSQL_USER: ${MYSQL_USER}
                          MYSQL_PASSWORD: ${MYSQL_PASSWORD}
                        ports:
                          - "3306:3306"
                    """
                    writeFile file: 'docker-compose.yml', text: dockerCompose
                }
                sh 'docker-compose down'
                sh 'docker-compose up -d'
            }
        }
        stage('Static Code Analysis') {
            steps {
                sh 'docker run --rm -v $(pwd)/src:/var/www/html php:7.4-cli sh -c "composer require squizlabs/php_codesniffer --dev && vendor/bin/phpcs --standard=PSR2 /var/www/html"'
            }
        }
        stage('Test') {
            steps {
                sh 'docker-compose exec web php -r "echo \'Testing environment...\';"'
                // Add actual testing commands here
            }
        }
        stage('Deploy') {
            steps {
                echo 'Deploying...'
                // Add deployment steps here if different from build
            }
        }
    }

    post {
        always {
            sh 'docker-compose down'
        }
    }
}
