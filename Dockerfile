FROM java:8
COPY target/smcdr-0.0.1-SNAPSHOT.jar /
WORKDIR /
EXPOSE 9997
CMD ["java", "-cp",  "smcdr-0.0.1-SNAPSHOT.jar",  "se.symsoft.codecamp.smcdr.SmcdrWriter"]