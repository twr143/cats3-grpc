FROM openjdk:8-jre

ENV JAVA_OPTS -XX:+UnlockExperimentalVMOptions -XX:+UseConcMarkSweepGC -XX:+CMSClassUnloadingEnabled -XX:+UseContainerSupport -XX:MinRAMPercentage=50.0 -XX:MaxRAMPercentage=95.0
ENV LANG C.UTF-8
ENV TZ Europe/Moscow
ENV config /root/config/application.conf
ENV logback /root/config/logback.xml

COPY ./entrypoint.sh /root/bin/
RUN chmod +x /root/bin/entrypoint.sh

COPY ./src/main/resources/logback.xml ${logback}
COPY ./src/main/resources/application.conf ${config}

COPY ./src/main/resources/cert/* /root/cert/

COPY ./lib/* /root/lib/

CMD ["/root/bin/entrypoint.sh"]
