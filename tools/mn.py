#!/usr/bin/env python
# In The Name Of God
# ========================================
# [] File Name : mn.py
#
# [] Creation Date : 15/03/16
#
# [] Created By : Parham Alvani (parham.alvani@gmail.com)
# =======================================
"""
mn.py:
    Script for running our sample topology on mininet
    and connect it into contorller on remote host.
Usage (example uses IP = 192.168.1.2):
    From the command line:
        sudo python mn.py --ip 192.168.1.2
"""
from mininet.net import Mininet
from mininet.net import CLI
from mininet.log import setLogLevel
from mininet.node import RemoteController
from mininet.node import OVSSwitch
from mininet.topo import Topo

import argparse
from functools import partial

class SampleTopology(Topo):
    """
    Subclass of mininet Topo class for
    creating following topology:
    h1 --- s1 -+- s2 --- h4
    h2 --- |   |   | --- h5
               s5
               |
    h3 --- s3 -+- s4 --- h6
    """
    def build(self, *args, **params):
        switch1 = self.addSwitch(name='s1')
        switch2 = self.addSwitch(name='s2')
        switch3 = self.addSwitch(name='s3')
        switch4 = self.addSwitch(name='s4')
        switch5 = self.addSwitch(name='s5')
        host1 = self.addHost(name='h1')
        host2 = self.addHost(name='h2')
        host3 = self.addHost(name='h3')
        host4 = self.addHost(name='h4')
        host5 = self.addHost(name='h5')
        host6 = self.addHost(name='h6')
        self.addLink(host1, switch1)
        self.addLink(host2, switch1)
        self.addLink(host4, switch2)
        self.addLink(host5, switch2)
        self.addLink(host3, switch3)
        self.addLink(host6, switch4)
        self.addLink(switch1, switch5)
        self.addLink(switch2, switch5)
        self.addLink(switch3, switch5)
        self.addLink(switch4, switch5)


if __name__ == '__main__':
    PARSER = argparse.ArgumentParser()
    PARSER.add_argument('--ip', dest='ip', help='ONOS Network Controller IP Address', default='127.0.0.1', type=str)
    CLI_ARGS = PARSER.parse_args()

    setLogLevel('info')

    SWITCH = partial(OVSSwitch, protocols='OpenFlow13')
    NET = Mininet(topo=SampleTopology(), controller=RemoteController('ONOS', ip=CLI_ARGS.ip, port=6633), switch=SWITCH)
    NET.start()
    CLI(NET)
    NET.stop()
