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
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.packet.*;

public class ARPHandler implements PacketProcessor {

	private PacketService packetService;

	public ARPHandler(PacketService packetService) {
		this.packetService = packetService;
	}

	@Override
	public void process(PacketContext context) {
		if (context.isHandled()) {
			return;
		}
		InboundPacket pkt = context.inPacket();
		Ethernet ethPkt = pkt.parsed();
		DeviceId deviceId = context.inPacket().receivedFrom().deviceId();

		if (ethPkt == null) {
			return;
		}

		if (ethPkt.getEtherType() == EtherType.ARP.ethType().toShort()) {
			TrafficTreatment.Builder treatmentBuilder;
			treatmentBuilder = DefaultTrafficTreatment.builder();
			treatmentBuilder.setOutput(PortNumber.FLOOD);
			treatmentBuilder.decNwTtl();
			TrafficTreatment treatment = treatmentBuilder.build();
			OutboundPacket outboundPacket = new DefaultOutboundPacket(deviceId, treatment,
				context.inPacket().unparsed());
			packetService.emit(outboundPacket);
		}
	}
}
