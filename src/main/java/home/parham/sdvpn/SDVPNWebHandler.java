/*
 * In The Name Of God
 * ======================================
 * [] Project Name : ShakhSDVPN 
 * 
 * [] Package Name : home.parham.sdvpn
 *
 * [] Creation Date : 16-05-2016
 *
 * [] Created By : Parham Alvani (parham.alvani@gmail.com)
 * =======================================
*/

package home.parham.sdvpn;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.net.Host;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;

public class SDVPNWebHandler extends AppUiMessageHandler implements HostListener {
	SDVPNWebHandler() {
	}

	@Override
	public void event(HostEvent hostEvent) {
		if (hostEvent.type() == HostEvent.Type.HOST_ADDED) {
			Host host = hostEvent.subject();
			ObjectNode hostMessage = objectNode();
			hostMessage.put("mac", host.mac().toString());
			hostMessage.put("vlan", host.vlan().toShort());
			hostMessage.put("ip", host.ipAddresses().toString());
			sendMessage(hostMessage);
		}
	}
}
