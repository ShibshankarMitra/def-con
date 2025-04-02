FROM docker.artifactory.homedepot.com/eclipse-temurin:17-jdk-focal AS buildvm

# copy the .jar file from runner
ARG jarFileName=build/libs/EnterpriseLaborManagement-0.0.1-SNAPSHOT.jar
ARG PROFILE
RUN echo ${PROFILE}
ENV PROFILE_VAR=$PROFILE
WORKDIR /home/${PROFILE}
COPY ${jarFileName} app.jar
ENTRYPOINT java -Dspring.profiles.active=${PROFILE_VAR} -jar app.jar
RUN echo ${jarFileName}
RUN ls
EXPOSE 8080
