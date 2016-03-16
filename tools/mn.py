#!/usr/bin/env python
# In The Name Of God
# ========================================
# [] File Name : mn.py
#
# [] Creation Date : 15/03/16
#
# [] Created By : Parham Alvani (parham.alvani@gmail.com)
# =======================================
from functools import partial

from mininet.net import Mininet
from mininet.net import CLI
from mininet.log import setLogLevel
from mininet.node import RemoteController
from mininet.node import OVSSwitch
from mininet.topo import Topo
import argparse

#
# We want to build following topology:
# h1 --- s1 --- s2 --- h2
#
class PathTopo(Topo):
    def build(self, *args, **params):
        h1 = self.addHost(name='h1')
        s1 = self.addSwitch(name='s1')
        h2 = self.addHost(name='h2')
        self.addLink(h1, s1)
        self.addLink(h2, s1)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--ip', dest='ip', help='Beehive Network Controller IP Address', default='127.0.0.1', type=str)
    args = parser.parse_args()

    setLogLevel('info')

    switch = partial(OVSSwitch, protocols='OpenFlow13')
    net = Mininet(topo=PathTopo(), controller=RemoteController('beehive-netctrl', ip=args.ip, port=6633), switch=switch)
    net.start()
    CLI(net)
    net.stop()
