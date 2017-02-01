#########################################
federation-with-rabbit Installation Guide
#########################################

Overview
========

The federation service is a project that facilitates the exchange of state between multiple
OpenDaylight deployments. Detailed explanation can be found in the developers guide. It comes
as part of the ODL installation package, but not activated by default.

Pre Requisites for Installing federation-with-rabbit
====================================================

Software Requirements
---------------------
The federation service which is based on the Rabbit MQ implementation, expects a Rabbit MQ Broker to be installed on a machine that is reachable by the OpenDaylight. If the broker is installed on the same machine as the OpenDaylight, the default user and password can be used (guest/guest). If the broker is installed on a different machine, the broker will deny access for the default user, and a new user needs to created and given permissions. 

The creation of the user should happen in the rabbit broker machine. For example:

sudo /usr/sbin/rabbitmqctl add_user newusername newuserpass

sudo rabbitmqctl set_permissions -p / newusername ".*" ".*" ".*"

Preparing for Installation
==========================

The federation service consists of a default configuration that should be modified in order to enable its functionality, and connect it to the correct rabbit broker. The YANG model that declares the configuration knobs and defaults is defined in federation-service.yang. The configuration that is used to override the defaults is federation-service-config.xml.

Verifying your Installation
===========================

FederationProducerMgr and FederationConsumerMgr logs prints seems valid and do not indicate an ERROR.
