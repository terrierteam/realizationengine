FROM maven:3.6.3-openjdk-8
RUN mkdir /gdt/
COPY . /gdt/
WORKDIR /gdt/
RUN find /gdt/ \
 && cd /gdt/dependantProjects/openshift-restclient-java \
 && mvn install \
 && cd /gdt/ \
 && mvn initialize \
 && mvn package -DskipTests=true
CMD ["java", "-jar", "target/BigDataStack-GDT-Data-0.0.1-SNAPSHOT.jar", "namespaceMonitor"]