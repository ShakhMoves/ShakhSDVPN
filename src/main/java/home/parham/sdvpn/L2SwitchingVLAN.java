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
import org.onosproject.net.topology.TopologyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * building Vlan tunnels for all the hosts with a specific Vlan
 * by adding a host, tunnels will be build automatically
 */
public class L2SwitchingVLAN implements HostListener {

	private ApplicationId appId;

	private FlowRuleService flowRuleService;
	private GroupService groupService;
	private DeviceService deviceService;
	private TopologyService topologyService;

	private Map<VlanId, List<Host>> vLanIdMap;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * @param appId           application ID
	 * @param flowRuleService flowRuleService that we use in order to apply flow rules
	 * @param groupService    group service that we use to make a group of flowRules with the same traffic selector
	 * @param deviceService   device service that we use for getting all available switches
	 * @param topologyService topology service for getting available path between two devices
	 */
	public L2SwitchingVLAN(ApplicationId appId, FlowRuleService flowRuleService, GroupService groupService,
	                       DeviceService deviceService, TopologyService topologyService) {
		this.appId = appId;

		this.flowRuleService = flowRuleService;
		this.groupService = groupService;
		this.deviceService = deviceService;
		this.topologyService = topologyService;

		this.vLanIdMap = new HashMap<>();
	}

	/**
	 * @param event HostEvent that we use to detect adding new hosts to the network
	 */
	@Override
	public void event(HostEvent event) {
		if (event.type() == Type.HOST_ADDED) {
			Host host = event.subject();
			DeviceId deviceId = host.location().deviceId();
			GroupId gid = new DefaultGroupId(host.vlan().toShort() + 1373);
			GroupKey gkey = new DefaultGroupKey(host.vlan().toString().getBytes());
			VlanId vlanId = VlanId.vlanId((short) (host.vlan().toShort() + 1373));


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
					TrafficSelector selector = selectorBuilder.matchVlanId(vlanId).build();

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
					flowBuilder.forTable(1);
					flowBuilder.withPriority(10);
					FlowRule flowRule = flowBuilder.build();

					flowRuleService.applyFlowRules(flowRule);

					/* Add rule for redirect our flow into table 1 for table 0 */

					/* Build traffic selector */
					selectorBuilder = DefaultTrafficSelector.builder();
					selector = selectorBuilder.matchVlanId(vlanId).build();

					/* Build traffic treatment */
					treatmentBuilder = DefaultTrafficTreatment.builder();
					treatment = treatmentBuilder.transition(1).build();

					/* Build the rule at the end :) */
					flowBuilder = new DefaultFlowRule.Builder();
					flowBuilder.forDevice(d.id()).withSelector(selector).withTreatment(treatment);
					flowBuilder.makePermanent();
					flowBuilder.fromApp(appId);
					flowBuilder.forTable(0);
					flowBuilder.withPriority(10);
					flowRule = flowBuilder.build();

					flowRuleService.applyFlowRules(flowRule);


				}
			}

			/* Add new host into VLan based host list */
			vLanIdMap.get(host.vlan()).add(host);

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


			}
			/* Rules for swapping the VLAN tag at sink Switch */

			TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();
			treatmentBuilder.setOutput(host.location().port());
			treatmentBuilder.setVlanId(host.vlan());
			TrafficTreatment treatment = treatmentBuilder.build();
			GroupBucket bucket = DefaultGroupBucket.createAllGroupBucket(treatment);
			List<GroupBucket> bucketList = new ArrayList<>();
			bucketList.add(bucket);
			GroupBuckets buckets = new GroupBuckets(bucketList);

			groupService.addBucketsToGroup(host.location().deviceId(), gkey, buckets, gkey, appId);

			/* Rules for applying the new VLAN tag at source switch */

			/* Build traffic selector */
			TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
			TrafficSelector selector = selectorBuilder.matchInPort(host.location().port()).build();

			/* Build traffic treatment */
			treatmentBuilder = DefaultTrafficTreatment.builder();
			treatmentBuilder.pushVlan();
			treatmentBuilder.setVlanId(vlanId);
			treatmentBuilder.transition(1);
			treatment = treatmentBuilder.build();

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
			flowRuleService.applyFlowRules(flowRule);

		}
	}

	/**
	 * We use this function in order to simplify our MPLS tunnel creation :)
	 *
	 * @param p    : create MPLS tunnel based on path p.
	 * @param gkey : group key to identifies OpenFlow group tables.
	 */
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


}
