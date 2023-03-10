FROM maven:3.6.3-openjdk-8
RUN mkdir /gdt/
COPY . /gdt/
WORKDIR /gdt/
RUN cd /gdt/ \
 && mvn initialize \
 && mvn package -DskipTests=true
RUN apt-get update \
 && apt-get -y install vim
CMD ["java", "-jar", "target/BigDataStack-GDT-Data-0.0.1-SNAPSHOT.jar", "namespaceMonitor"]