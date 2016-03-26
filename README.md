# ShakhSDVPN
## Introduction
Shakh Implementation of SDVPN article just for having more fun.
SDVPN is a simple SDN based platform for creating and managing VPLS
using ONOS platform, indeed we implement it as ONOS app.

### VPLS (Virtual Private LAN Service)
Virtual Private LAN Service (VPLS) is a way to provide Ethernet-based
multipoint to multipoint communication over IP or MPLS networks.
It allows geographically dispersed sites to share an Ethernet broadcast
domain by connecting sites through pseudo-wires.

### ONOS (Open Networking Operating System)
The Open Network Operating System (ONOS) is a software defined networking
(SDN) OS for service providers that has scalability, high availability,
high performance and abstractions to make it easy to create apps and services.

## Installation
### Installing ONOS
First of all you must have ONOS installed on your system.
you can install it from [here](https://wiki.onosproject.org/display/ONOS/Installing+and+Running+ONOS).
### Compile :)
```shell
mvn install
```
### Up and Running
```shell
onos-karaf
```
```shell
onos-app 127.0.0.1 install target/sdvpn-{version}.oar
onos-app 127.0.0.1 activate home.parham.sdvpn
```