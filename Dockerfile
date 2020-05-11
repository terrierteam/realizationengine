FROM maven:3.6.3-openjdk-8
RUN mkdir /gdt/
COPY . /gdt/
WORKDIR /gdt/
RUN export CLASSPATH=$CLASSPATH:/gdt/lib/qe-driver-0.500-20200406.151302-4-jdbc-client.jar:/gdt/lib/kivi-api-0.500-20200407.124711-4-direct-client.jar \
 && find /gdt/ \
 && cd /gdt/dependantProjects/openshift-restclient-java \
 && mvn install \
 && cd /gdt/ \
 && mvn package -DskipTests=true
CMD ["java", "-jar", "target/BigDataStack-GDT-Data-0.0.1-SNAPSHOT.jar", "namespaceMonitor"]