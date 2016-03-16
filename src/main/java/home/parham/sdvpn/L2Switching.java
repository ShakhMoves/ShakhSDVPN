package home.parham.sdvpn;

import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultGroupId;
import org.onosproject.core.GroupId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.*;
import org.onosproject.net.group.*;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostEvent.Type;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class L2Switching implements HostListener {

	private ApplicationId appId;

	private FlowRuleService flowRuleService;
	private GroupService groupService;
	private PacketService packetService;

	private Map<VlanId, List<Host>> vlanIdMap;

	private final Logger log = LoggerFactory.getLogger(getClass());

	public L2Switching(ApplicationId appId, FlowRuleService flowRuleService, GroupService groupService, PacketService packetService) {
		this.appId = appId;

		this.flowRuleService = flowRuleService;
		this.groupService = groupService;
		this.packetService = packetService;

		this.vlanIdMap = new HashMap<>();
	}

	public void event(HostEvent event) {
		if (event.type() == Type.HOST_ADDED) {
			log.info("Start adding new host " + event.subject());

			Host host = event.subject();
			DeviceId deviceId = host.location().deviceId();
			GroupId gid = new DefaultGroupId(host.vlan().toShort() + 1373);
			GroupKey gkey = new DefaultGroupKey(host.vlan().toString().getBytes());


			if (!vlanIdMap.containsKey(host.vlan())) {
				vlanIdMap.put(host.vlan(), new ArrayList<>());

				/* Add empty group table for our feature use :) */
				GroupBuckets buckets = new GroupBuckets(new ArrayList<>());
				GroupDescription groupDescription = new DefaultGroupDescription(deviceId, GroupDescription.Type.ALL, buckets, gkey, gid.id(), appId);
				groupService.addGroup(groupDescription);

				/* Build traffic selector */
				TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
				TrafficSelector selector = selectorBuilder.matchVlanId(host.vlan()).build();

				/* Build traffic treatment */
				TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();
				treatmentBuilder.group(gid);
				TrafficTreatment treatment = treatmentBuilder.build();

				/* Build flow rule based on our treatment and selector */
				FlowRule.Builder flowBuilder = new DefaultFlowRule.Builder();
				flowBuilder.forDevice(deviceId).withSelector(selector).withTreatment(treatment);
				flowBuilder.makePermanent();
				flowBuilder.fromApp(appId);
				flowBuilder.withPriority(10);
				FlowRule flowRule = flowBuilder.build();

				/* Apply rule on device :) */
				flowRuleService.applyFlowRules(flowRule);

			}

			vlanIdMap.get(host.vlan()).add(host);

			TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();
			treatmentBuilder.setOutput(host.location().port());
			TrafficTreatment treatment = treatmentBuilder.build();
			GroupBucket bucket = DefaultGroupBucket.createAllGroupBucket(treatment);
			List<GroupBucket> bucketList = new ArrayList<>();
			bucketList.add(bucket);
			GroupBuckets buckets = new GroupBuckets(bucketList);

			groupService.addBucketsToGroup(deviceId, gkey, buckets, gkey, appId);

			/* Pass our ARP packet to his destination ... */
			/*
			treatmentBuilder = DefaultTrafficTreatment.builder();
			treatmentBuilder.setOutput(PortNumber.FLOOD);
			treatment = treatmentBuilder.build();
			OutboundPacket outboundPacket = new DefaultOutboundPacket(deviceId, treatment);
			packetService.emit(outboundPacket);
			*/
		}
	}
}
