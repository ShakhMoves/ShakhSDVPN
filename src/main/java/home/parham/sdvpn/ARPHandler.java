/*
 * In The Name Of God
 * ======================================
 * [] Project Name : ShakhSDVPN 
 * 
 * [] Package Name : home.parham.sdvpn
 *
 * [] Creation Date : 26-03-2016
 *
 * [] Created By : Parham Alvani (parham.alvani@gmail.com)
 * =======================================
*/

package home.parham.sdvpn;

import org.onlab.packet.EthType.EtherType;
import org.onlab.packet.Ethernet;
import org.onosproject.net.PortNumber;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;

public class ARPHandler implements PacketProcessor {

	public ARPHandler() {
	}

	@Override
	public void process(PacketContext context) {
		if (context.isHandled()) {
			return;
		}
		InboundPacket pkt = context.inPacket();
		Ethernet ethPkt = pkt.parsed();

		if (ethPkt == null) {
			return;
		}

		if (ethPkt.getEtherType() == EtherType.ARP.ethType().toShort()) {
			context.treatmentBuilder().setOutput(PortNumber.FLOOD);
			context.send();
		}
	}
}
