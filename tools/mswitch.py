"""
vlan.py: Switch subclass that can connect to different controllers.
"""
# In The Name Of God
# ========================================
# [] File Name : mswitch.py
#
# [] Creation Date : 09-07-2016
#
# [] Created By : Parham Alvani (parham.alvani@gmail.com)
# =======================================

from mininet.node import OVSSwitch


class MultiSwitch(OVSSwitch):
    "Custom Switch() subclass that connects to different controllers"
    def start(self, controllers):
        return OVSSwitch.start(self, controllers)
