######################################
federation-with-rabbit Developer Guide
######################################

Overview
========

The federation service is a project that facilitates the exchange of state between multiple
OpenDaylight deployments (henceforth 'sites'). These sites may be single node deployments or cluster deployments. The 'federation-with-rabbit' feature is a specific implementation of the federation service, based on Rabbit MQ broker. Federation service currently only supports the Rabbit MQ implementation.

federation-with-rabbit Architecture
===================================

In the context of federation, each site can wear two hats. A site can be a producer of messages and/or a consumer or messages. This is why each component is logically divided to two parts: Egress(Producer) part and Ingress(Consumer) part.

High-Level Components
---------------------
Federation Plugin SPI
^^^^^^^^^^^^^^^^^^^^^
This plugin SPI is implemented by applications that want to use the federation capabilities. 
The plugin declares which entities in the MD-SAL it wants the federation infrastructure to listen to, and it gets notified when the state of the entity in the MD-SAL changes. Upon a notification, the plugin can decide to do what ever it wants with the entity -> Send it to the other site, filter it out, transform it and even send a completely different entity. 

In each site, the amount of instances of the plugin is equal to the amount of remote connected sites.

Federation Service Infrastructure
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
This layer hosts the Federation Plugins. In the producer side, it is responsible for the creation and destruction of the plugins, listening to the MD-SAL and passing of the modification notifications to the plugin themselves, and the tunnel for publishing remote messages by the plugins. 

In the consumer side, it is responsible for consuming the remote sites messages and passing them to the corresponding plugins. 

Rabbit MQ Infrastructure
^^^^^^^^^^^^^^^^^^^^^^^^
This layer is used by the Federation Service Infrastructure for sending and receiving messages from other sites. It also exposes the ability to create and destroy queues for the messages themselves. This component communicates directly with the Rabbit Broker. A prerequisite for the federation service is the existence of at least one Rabbit Broker that each site can reach, and all the exchanging of messages happens through this broker/s.  

Lifecycle
---------

Subscription
^^^^^^^^^^^^
To establish the initial connection between sites, a subscribe() must be invoked on the Federation Service Infrastructure. A subscription is a request between a consumer-site to a producer-site. This means that in order connect Site A with Site B, Site A will have to subscribe to Site B, and Site B will have to subscribe to Site A.

Stages of State Synchronization
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
After the initial connection, the sites transition into Full Sync stage. 

In Full Sync stage, the producer site sends all the existing relevant state to the consumer site. The Full Sync stage start with a StartFullSyncFederationMessage, follows with all the entity messages, and ends with a EndFullSyncFederationMessage. When the Full Sync stage is over, the sites transition into the next stage - Steady Sync.

In the Steady Sync stage, ongoing MD-SAL updates for the relevant state are notified in the producer site to the instances of the plugins. The plugins then send the interesting state to the consumer sites using the federation infrastructure.


Key APIs and Interfaces
=======================

Introduction
------------

When observing the federation service in a perspective of Egress and Ingress, the classes and interfaces responsible for each task are divided in the following way.

Egress/Producer
^^^^^^^^^^^^^^^
* Federation plugin SPI - IFederationPluginEgress
* Federation service infrastructure - FederationProducerMgr
* Rabbit MQ infrastructure - RabbitMessageBus.sendMsg()

Ingress/Consumer
^^^^^^^^^^^^^^^^
* Federation plugin SPI - IFederationPluginIngress
* Federation service infrastructure - FederationConsumerMgr
* Rabbit MQ infrastructure - RabbitMessageBus.attachHandler()

API Reference Documentation
===========================
IFederationPluginEgress
-----------------------
JavaDocs link

IFederationPluginIngress
------------------------
JavaDocs link

FederationProducerMgr
---------------------
JavaDocs link

FederationConsumerMgr
---------------------
JavaDocs link

RabbitMessageBus
---------------------
JavaDocs link



