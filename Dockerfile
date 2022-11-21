FROM openjdk:17 as builder
WORKDIR app
COPY target/*.jar investment_tracker.jar
RUN java -Djarmode=layertools -jar investment_tracker.jar extract

FROM openjdk:17
WORKDIR app
ADD https://raw.githubusercontent.com/vishnubob/wait-for-it/master/wait-for-it.sh .
RUN chmod +x ./wait-for-it.sh
COPY --from=builder app/dependencies/ ./
COPY --from=builder app/spring-boot-loader/ ./
COPY --from=builder app/snapshot-dependencies/ ./
COPY --from=builder app/application/ ./
CMD ["java", "org.springframework.boot.loader.JarLauncher"]
