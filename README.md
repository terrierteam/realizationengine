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


This software has been developed under the BigDataStack project, as part of the holistic solution for big data applications and operations. More information can be found here: https://bigdatastack.eu/the-bigdatastack-solution.  BigDataStack has received funding from the European Union’s Horizon 2020 research and innovation programme under grant agreement No 779747.
