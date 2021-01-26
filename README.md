# Realization Engine

The goal of the Realization Engine is to provide a central suite of containerized services that enable configuration, deployment and subsequent management of user applications and their components. The main functionalities provided by the Realization Engine are:
 * Registration and storage of user applications (either via complete BigDataStack Playbooks or in smaller units).
 * Deconstruction of BigDataStack Playbooks into constituent components for easier management. These components are: the application definition; comprised object definitions (Deployment Configs, Jobs, Services, Routes, etc.); exported metrics; service level objectives; operation sequences; and application states.
 * Provision of built-in object-level management actions for the user’s application
 * Support for complex deployment or alteration actions in the form of operation sequences.
 * Live Openshift cluster state monitoring, enabling synchronisation of application states between the cluster and Realization Engine supporting automated action triggering.
 * Short-term time-series data storage for Realization Engine managed metrics.
 * Provision of a REST API for accessing application, component, and cluster status, as well as triggering actions.

## Application Model

![alt text](http://www.dcs.gla.ac.uk/~richardm/BigDataStack/REModel.png "Realization Engine Application Model")

Under the application model used by the Realization Engine, the user account or ‘owner’ owns one or more applications and can also define metrics. A single application has a state, zero or more object (templates) representing the different components of the application, zero or more operation sequences representing actions that can be performed for the application, and a series of events generated about the application. An object template (application component) can be instantiated multiple times, producing object instances. Object instances may have an associated resource template describing the resources assigned to that object. An object instance contains a definition of an underlying Kubernetes or Openshift object that contains the deployment information. Operation sequences represent actions to perform on the application and contain multiple atomic operations. An operation targets either an object template or instance, performing either some alteration or deployment action upon it. Service level objectives can be attached to an object instance, which track a metric exported by or about that object. 

## Engine Components

![alt text](http://www.dcs.gla.ac.uk/~richardm/BigDataStack/REArch.png "Realization Engine Components")

 * Realization Engine (and Application API): This is a containerized service that houses the main application management logic. It also exposes the Realization Engine API that provides other components with access to user application states and actions (REQ-RE-04), as well as enabling the registration of new applications by the Data Toolkit (REQ-RE-01).
 * Realization UI: This is a graphical user interface exposed by the Realization Engine that enables users to view the state of their applications, as well as trigger actions for them (REQ-RE-05).
 * Cluster Monitoring: This component is responsible for synchronizing the state of the underlying Kubernetes/Openshift objects running on the cluster with their associated BigDataStack Object definitions stored in the State DB.
 * Resource Monitoring: This component acts as a bridge between Openshift Monitoring (a built-in set of services to Openshift that track node and pod-level resource usage) and the Realization Engine. This enables the Realization Engine to access live CPU and Memory usage by the application components. 
 * Cost Estimation: The cost estimation component, as its name suggests, generates estimated costs (in US dollars) for the different application components. By doing so, it enables service level objectives such as cost per hour or total cost to be evaluated.
 * Log Search: This component hosts a search engine that indexes the logs of each running container within the user application and provides custom search functionality for those logs.



This software has been developed under the BigDataStack project, as part of the holistic solution for big data applications and operations. More information can be found here: https://bigdatastack.eu/the-bigdatastack-solution.  BigDataStack has received funding from the European Union’s Horizon 2020 research and innovation programme under grant agreement No 779747.
