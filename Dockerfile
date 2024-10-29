# Docker 이미지를 생성할 때 기반이 되는 베이스 이미지 설정
FROM openjdk:17

# Dockerfile 내에서 사용할 변수 JAR_FILE을 정의
ARG JAR_FILE=build/libs/*-SNAPSHOT.jar

# jar 빌드 파일을 도커 컨테이너(Docker 이미지 내부)로 복사
COPY ${JAR_FILE} app.jar

# jar 파일 실행
ENTRYPOINT ["java","-jar","/app.jar"]