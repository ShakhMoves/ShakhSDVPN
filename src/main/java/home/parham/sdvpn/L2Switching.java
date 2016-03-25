package home.parham.sdvpn;

import org.onlab.packet.EthType.EtherType;
import org.onlab.packet.Ethernet;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultGroupId;
import org.onosproject.core.GroupId;
import org.onosproject.net.*;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.*;
import org.onosproject.net.group.*;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostEvent.Type;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.packet.*;
import org.onosproject.net.topology.TopologyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class L2Switching implements HostListener, PacketProcessor {

	private ApplicationId appId;

	private FlowRuleService flowRuleService;
	private GroupService groupService;
	private PacketService packetService;
	private DeviceService deviceService;
	private TopologyService topologyService;

	private Map<VlanId, List<Host>> vLanIdMap;

	private final Logger log = LoggerFactory.getLogger(getClass());

	public L2Switching(ApplicationId appId, FlowRuleService flowRuleService, GroupService groupService,
	                   PacketService packetService, DeviceService deviceService, TopologyService topologyService) {
		this.appId = appId;

		this.flowRuleService = flowRuleService;
		this.groupService = groupService;
		this.packetService = packetService;
		this.deviceService = deviceService;
		this.topologyService = topologyService;
		this.vLanIdMap = new HashMap<>();
	}

	public void event(HostEvent event) {
		if (event.type() == Type.HOST_ADDED) {
			Host host = event.subject();
			DeviceId deviceId = host.location().deviceId();
			GroupId gid = new DefaultGroupId(host.vlan().toShort() + 1373);
			GroupKey gkey = new DefaultGroupKey(host.vlan().toString().getBytes());
			MplsLabel mplsLabel = MplsLabel.mplsLabel(host.vlan().toShort() + 1373);


			/* When we see new VLan */
			if (!vLanIdMap.containsKey(host.vlan())) {
				/* Create empty array for hosts of these VLan */
				vLanIdMap.put(host.vlan(), new ArrayList<>());

				/* Rules for removing the MPLS tag */

				for (Device d : deviceService.getAvailableDevices()) {
					/* Add empty group table for our future use */
					GroupBuckets buckets = new GroupBuckets(new ArrayList<>());
					GroupDescription groupDescription = new DefaultGroupDescription(d.id(),
						GroupDescription.Type.ALL, buckets, gkey, gid.id(), appId);
					groupService.addGroup(groupDescription);

					/* Add rule for our empty group in table 1 */

					/* Build traffic selector */
					TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
					// TrafficSelector selector = selectorBuilder.matchMplsLabel(mplsLabel).build();
					TrafficSelector selector = selectorBuilder.matchVlanId(host.vlan()).build();

					/* Build traffic treatment */
					TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();
					treatmentBuilder.group(gid);
					TrafficTreatment treatment = treatmentBuilder.build();

					/*
					 * Build flow rule based on our treatment and
					 * selector for table 1
					 * because we want another rule for table 0
				        */
					FlowRule.Builder flowBuilder = new DefaultFlowRule.Builder();
					flowBuilder.forDevice(d.id()).withSelector(selector).withTreatment(treatment);
					flowBuilder.makePermanent();
					flowBuilder.fromApp(appId);
					//flowBuilder.forTable(1);
					flowBuilder.withPriority(10);
					FlowRule flowRule = flowBuilder.build();

					flowRuleService.applyFlowRules(flowRule);

					/* Add rule for redirect our flow into table 1 for table 0 */

					/* Build traffic selector */
					selector = selectorBuilder.matchMplsLabel(mplsLabel).build();

					/* Build traffic treatment */
					treatment = treatmentBuilder.transition(1).build();

					/* Build the rule at the end :) */
					flowBuilder = new DefaultFlowRule.Builder();
					flowBuilder.forDevice(d.id()).withSelector(selector).withTreatment(treatment);
					flowBuilder.makePermanent();
					flowBuilder.fromApp(appId);
					flowBuilder.forTable(0);
					flowBuilder.withPriority(10);
					flowRule = flowBuilder.build();

					//flowRuleService.applyFlowRules(flowRule);


				}
			}

			/* And we do this for all our new hosts :) */

			/*
			 * Create path between our new host and all old hosts
			 * that has the same VLanID
			 */
			Set<Path> paths;
			for (Host h : vLanIdMap.get(host.vlan())) {
				paths = topologyService.getPaths(topologyService.currentTopology(),
					deviceId, h.location().deviceId());

				if (paths.size() > 0) {
					/* We just want 1 path for our purpose :) */
					Path p = paths.iterator().next();

					buildTunnelPath(p, gkey);
				}

				paths = topologyService.getPaths(topologyService.currentTopology(),
					h.location().deviceId(), deviceId);

				if (paths.size() > 0) {
					/* We just want 1 path for our purpose :) */
					Path p = paths.iterator().next();

					buildTunnelPath(p, gkey);
				}

				/* Add our last rule for sink device */
				TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();
				treatmentBuilder.setOutput(h.location().port());
				//treatmentBuilder.popMpls();
				//if (host.vlan().toShort() != -1) {
				//	treatmentBuilder.setVlanId(host.vlan());
				//}
				TrafficTreatment treatment = treatmentBuilder.build();
				GroupBucket bucket = DefaultGroupBucket.createAllGroupBucket(treatment);
				List<GroupBucket> bucketList = new ArrayList<>();
				bucketList.add(bucket);
				GroupBuckets buckets = new GroupBuckets(bucketList);

				groupService.addBucketsToGroup(h.location().deviceId(), gkey, buckets, gkey, appId);
			}

			/* Rules for applying the MPLS tag at source switch */

			/* Build traffic selector */
			TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
			TrafficSelector selector = selectorBuilder.matchInPhyPort(host.location().port()).build();

			/* Build traffic treatment */
			TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();
			treatmentBuilder.setMpls(mplsLabel);
			treatmentBuilder.setVlanId(VlanId.NONE);
			treatmentBuilder.transition(1);
			TrafficTreatment treatment = treatmentBuilder.build();

			/*
			 * Build flow rule based on our treatment and
			 * selector
			*/
			FlowRule.Builder flowBuilder = new DefaultFlowRule.Builder();
			flowBuilder.forDevice(deviceId).withSelector(selector).withTreatment(treatment);
			flowBuilder.makePermanent();
			flowBuilder.fromApp(appId);
			flowBuilder.withPriority(20);
			FlowRule flowRule = flowBuilder.build();

			/* Apply rule on device */
			//flowRuleService.applyFlowRules(flowRule);

			vLanIdMap.get(host.vlan()).add(host);
		}
	}

	private void buildTunnelPath(Path p, GroupKey gkey) {
		/* Add MPLS forwarding rule for all devices except sink device */
		for (Link l : p.links()) {
			DeviceId pathNodeDevice = l.src().deviceId();
			PortNumber pathNodePort = l.src().port();

			TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();
			treatmentBuilder.setOutput(pathNodePort);
			TrafficTreatment treatment = treatmentBuilder.build();
			GroupBucket bucket = DefaultGroupBucket.createAllGroupBucket(treatment);
			List<GroupBucket> bucketList = new ArrayList<>();
			bucketList.add(bucket);
			GroupBuckets buckets = new GroupBuckets(bucketList);

			groupService.addBucketsToGroup(pathNodeDevice, gkey, buckets, gkey, appId);
		}
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
			TrafficTreatment treatment = treatmentBuilder.build();
			OutboundPacket outboundPacket = new DefaultOutboundPacket(deviceId, treatment,
				context.inPacket().unparsed());
			packetService.emit(outboundPacket);
		}
	}
}
