package montreal;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkInverter;
import org.matsim.core.network.algorithms.NetworkTurnInfoBuilder;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.Lanes;
import org.matsim.lanes.LanesToLinkAssignment;

public class LaneDebug {
public static void main(String[] args) {
//	Config config = ConfigUtils.createConfig();
//	config.network().setLaneDefinitionsFile("data\\osm\\newOSM\\testLanes_out.xml");
//	config.network().setInputFile("data\\osm\\newOSM\\osmMultimodal.xml");
//	
//	Scenario scn = ScenarioUtils.loadScenario(config);
//	
//	Network net = scn.getNetwork();
//	Lanes lanes = scn.getLanes();
//	LanesToLinkAssignment l2l = lanes.getLanesToLinkAssignments().get(Id.createLinkId("323149"));
//	l2l.getLanes();
//	Link l = net.getLinks().get(l2l.getLinkId());
//	NetworkTurnInfoBuilder netTurnBuilder = new NetworkTurnInfoBuilder(scn);
//	Network invertedNet = new NetworkInverter(scn.getNetwork(),netTurnBuilder.createAllowedTurnInfos()).getInvertedNetwork();
//	Node n = invertedNet.getNodes().get(Id.createNodeId(l.getId().toString()));
	Network net = NetworkUtils.readNetwork("data\\osm\\newOSM\\osmMultimodal.xml");
	double minCap = Double.MAX_VALUE;
	for(Link link:net.getLinks().values()) {
		if(link.getCapacity()<=600)link.setCapacity(800);;
		if(link.getCapacity()<minCap)minCap = link.getCapacity();
	}
	System.out.println(minCap);
	//System.out.println(rail);
}
}
