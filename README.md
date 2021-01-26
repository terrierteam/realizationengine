# Realization Engine

The goal of the Realization Engine is to provide a central suite of containerized services that enable configuration, deployment and subsequent management of user applications and their components. The main functionalities provided by the Realization Engine are:
 * Registration and storage of user applications (either via complete BigDataStack Playbooks or in smaller units).
 * Deconstruction of BigDataStack Playbooks into constituent components for easier management. These components are: the application definition; comprised object definitions (Deployment Configs, Jobs, Services, Routes, etc.); exported metrics; service level objectives; operation sequences; and application states.
 * Provision of built-in object-level management actions for the user’s application
 * Support for complex deployment or alteration actions in the form of operation sequences.
 * Live Openshift cluster state monitoring, enabling synchronisation of application states between the cluster and Realization Engine supporting automated action triggering.
 * Short-term time-series data storage for Realization Engine managed metrics.
 * Provision of a REST API for accessing application, component, and cluster status, as well as triggering actions.



This software has been developed under the BigDataStack project, as part of the holistic solution for big data applications and operations. More information can be found here: https://bigdatastack.eu/the-bigdatastack-solution.  BigDataStack has received funding from the European Union’s Horizon 2020 research and innovation programme under grant agreement No 779747.
