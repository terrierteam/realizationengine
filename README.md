# Realization Engine

The realization engine is a suite of containerized services that provide configuration, deployment and subsequent state-monitoring, as well as manage tooling for applications deployed upon Kubernetes. As a container suite, the Realization Engine is divided into a core, along with a set of plugins that add functionality. The core platform is comprised of three services: 

 * Realization Engine API: This is a containerized RESTful service that allows an application developer to register new applications, deploy registered applications, and/or issue commands to alter the configuration of those applications at run-time. This is the central management service of the suite.
 * Cluster Monitoring: This component acts as a synchronisation layer between the Realization Engine and the underlying cluster management system, in our case Kubernetes. Its role is to continuously monitor the namespace(s) registered with a user’s application and record any changes in states observed (that can be used by other components to trigger actions).
 * Application State DB: As the name suggests, this is a database that stores the user’s application configuration, as well as the present and past states of associated Kubernetes objects. 
 * Operation Sequence Runner: Unlike the aforementioned components that are persistent, an operation sequence runner is a transient container that performs a sequence of operations that will result in the deployment or alteration of a user application. Operation Sequence Runners are spawned by the Realization Engine API on demand.

The Realization Engine provides a central place where complex applications can be managed holistically. This is different to Kubernetes itself, which is only concerned with the management of containers, i.e. the Realization Engine models at the level of an Application, while Kubernetes models at the level of a container, pod or other low-level object. It is primarally designed for use with Openshift 3.11 based clusters, but is compatable with base-level Kubernetes clusters.

## Installation

### Stage 1: Preparing For Engine Bootstrap
To install the realization engine, you will first need to prepare a database and bootstrap container. For the database, we provide a kubernetes deployment specification for a MySQL database, which you can find in the kubernetes/db.yaml file, along with its configuration map kubernetes/cm.yaml. You should register the config map and deployment config with your cluster to start the database. When doing so, you should replace the NAMESPACE and MYSQLPASSWORD entries in these files with meaningful values for your installation. 

Next we will need to build the all-in-one container image for the Realization Engine based on the current version of the software from the Git repository. You should clone this (https://github.com/terrierteam/realizationengine) repository and use Docker to build a local copy of the container image. The repository includes a Dockerfile that defines the build process in the root directory. Before doing so however, you should edit the gdt.config.yaml file to enter the details for your cluster:
 * database.type: mysql
 * database.host: IP/service name of the database container
 * database.port: The port of the database container
 * database.name: The name of the database used for storage (BigDataStackGDTDB by default, set in db.yaml)
 * database.username: The database username (GDT by default, set in db.yaml)
 * database.password: The database password (what you set MYSQLPASSWORD to in db.yaml)
 * openshift.client: fabric8io
 * openshift.host: The IP/URL of the kubernetes/openshift API
 * openshift.port: The port number of the kubernetes/openshift API
 * openshift.username: The username of the kubernetes user that you want the realisation engine to use
 * openshift.password: The password of the kubernetes user that you want the realization engine to use
 * openshift.hostExtension: The host extension for your cluster, this is the static part of route URLs that the cluster creates (which are usually of the form http://<routename>.<namespace>.<hostExtension>/
 * imageRepositoryHost: docker-registry.default.svc:5000 for openshift 3.11 clusters
 * namespace: The namespace you want the realisation engine to manage
 * openshiftPrometheus: If you have an openshift installation and its prometheus time series database is available you can set the URL for it here (optional)

Once done, you need to upload it to an image repostory, e.g. DockerHub, or a cluster local repository. Note the URL of the image, as we will need it next.

Once the database is running and we have the image, you should then start a realization engine cli container, from which we can bootstrap the platform. The deployment specification can be found in kubernetes/cli.yaml. Edit this file to replace YOURIMAGE with the image URL, and also set the namespace. Once done, use the file to deploy the realization cli container to the cluster.

### Stage 2: Engine Bootstrap
Once the cli container is running, you should connect to the container to bring up a command line terminal, from there, we can issue commands to the realization engine. Here we first need to register a definition of the container namespace to manage. You should create a file in the /tmp/ directory of the container with the following content:

```
namespace: <namespace>
host: <openshift.host>
port: <openshift.port>
```

Where the <> should match your the gdt.config.yaml file. From here we will be running a series of terminal commands to start the realization engine. If you have not already, I recommend you run 'bash' so you have tab- auto-complete available. From the /gdt directory, run 

```
java -jar target/BigDataStack-GDT-0.8.jar
```

to get the realization engine help page. Next, run:

```
java -jar target/BigDataStack-GDT-0.8.jar register namespace /tmp/<yournamespacefile>
```

where <yournamespacefile> is the file you created above. Then run:

```
java -jar target/BigDataStack-GDT-0.8.jar monitor start <username> <namespace>
```

where username is openshift.username and namespace is your namespace. This should deploy all of the realization engine components into that namespace, which completes the realization engine installation.

