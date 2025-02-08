package montreal;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.Lane;
import org.matsim.lanes.Lanes;
import org.matsim.lanes.LanesReader;
import org.matsim.lanes.LanesToLinkAssignment;

public class NetDetails {
	public static void main(String[] args) {
		String oldEmme = "data\\osm\\newOSM\\osmMultimodal.xml";
		String oldEmmeLane = "data\\osm\\newOSM\\testLanes_out.xml";
		
		Network oldEm = NetworkUtils.readNetwork(oldEmme);
		int art = 0;
		for(Link l:oldEm.getLinks().values()) {
			if(l.getId().toString().contains("pt"))art++;
		}
		System.out.println("artificial Links = "+art);
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new LanesReader(scenario).readFile(oldEmmeLane);
		Lanes lanes = scenario.getLanes();
		int lane = 0;
		for(LanesToLinkAssignment l2l:lanes.getLanesToLinkAssignments().values()) {
			for(Lane l:l2l.getLanes().values()) {
				lane++;
			}
		}
		System.out.println("Total Lanes = "+lane);
	}
	
	public static void modifyQuebecNet(Network network) {
		Node n1 = network.getNodes().get(Id.createNodeId("3LLIQ9"));
		Node n2 = network.getNodes().get(Id.createNodeId("3LQIL9"));
		NetworkFactory netFac = network.getFactory();
		
		Node newId = network.getNodes().get(Id.createNodeId("282129544"));
		
		Link l1 = netFac.createLink(Id.createLinkId("3LLIQ9_282129544"),n1, newId);
		Link l2 = netFac.createLink(Id.createLinkId("282129544_3LLIQ9"), newId,n1);
		Link l3 = netFac.createLink(Id.createLinkId("282129544_3LQIL9"),newId,n2);
		Link l4 = netFac.createLink(Id.createLinkId("3LQIL9_282129544"),n2,newId);
		
		Set<String> modes = new HashSet<>();																	//define allowed mode
		modes.add("car");
		modes.add("car_passenger");
		modes.add("bus");
		l1.setAllowedModes(modes);
        l1.setCapacity(3600);
        l1.setFreespeed(25);
        l1.setLength(NetworkUtils.getEuclideanDistance(l1.getFromNode().getCoord(), l1.getToNode().getCoord()));
        l1.setNumberOfLanes(2);
        network.addLink(l1);
        
        l2.setAllowedModes(modes);
        l2.setCapacity(3600);
        l2.setFreespeed(25);
        l2.setLength(NetworkUtils.getEuclideanDistance(l1.getFromNode().getCoord(), l1.getToNode().getCoord()));
        l2.setNumberOfLanes(2);
        network.addLink(l2);
		
        l3.setAllowedModes(modes);
        l3.setCapacity(3600);
        l3.setFreespeed(25);
        l3.setLength(NetworkUtils.getEuclideanDistance(l1.getFromNode().getCoord(), l1.getToNode().getCoord()));
        l3.setNumberOfLanes(2);
        network.addLink(l3);
		
        
        l4.setAllowedModes(modes);
        l4.setCapacity(3600);
        l4.setFreespeed(25);
        l4.setLength(NetworkUtils.getEuclideanDistance(l1.getFromNode().getCoord(), l1.getToNode().getCoord()));
        l4.setNumberOfLanes(2);
        network.addLink(l4);
	}
	

}
