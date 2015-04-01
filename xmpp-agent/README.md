# XMPP Event Source User agent

Purpose of this project is to showcase client interaction with
event-source topology and republishing information obtained via
event source topology model to external system.

As one of showcase systems, XMPP was selected. This application contains
minimum necessary code, with limited functionality:

* listen on XMPP agent configuration model usign MD-SAL Binding APIs
* listen on event-aggregator notifications using MD-SAL DOM APIs
* republish this notifications to XMPP server and connected clients

## Building Demo Code

### XMPP Event Source User Agent

XMPP Event source user agant is based on Opendaylight Karaf Archetype,
which generated initial skeleton along with custom Karaf distribution creation.
In order to build  
