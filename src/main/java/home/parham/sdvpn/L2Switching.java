package home.parham.sdvpn;

import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostEvent.Type;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class L2Switching implements HostListener {

	private HostService hostService;
	private FlowRuleService flowRuleService;
	private ApplicationId appId;

	private final Logger log = LoggerFactory.getLogger(getClass());

	public L2Switching(ApplicationId appId, HostService hostService, FlowRuleService flowRuleService) {
		this.hostService = hostService;
		this.appId = appId;
		this.flowRuleService = flowRuleService;

	}

	public void event(HostEvent event) {
		if (event.type() == Type.HOST_ADDED) {
			log.info("Start adding new host " + event.subject());

			VlanId vid = event.subject().vlan();
			HostId id = event.subject().id();
			Host host = hostService.getHost(id);

			DeviceId deviceId = host.location().deviceId();

			/* Build traffic selector */
			TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
			TrafficSelector selector = selectorBuilder.matchVlanId(vid).build();

			/* Build traffic treatment */
			TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();
			treatmentBuilder.setOutput(host.location().port()).build();
			TrafficTreatment treatment = treatmentBuilder.build();

			/* Build flow rule based on our treatment and selector */
			FlowRule.Builder flowBuilder = new DefaultFlowRule.Builder();
			flowBuilder.forDevice(deviceId).withSelector(selector).withTreatment(treatment);
			flowBuilder.makePermanent();
			flowBuilder.fromApp(appId);
			flowBuilder.withPriority(10);
			FlowRule flowRule = flowBuilder.build();

			flowRuleService.applyFlowRules(flowRule);
			//
		}
	}
}
