package montreal;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.Lanes;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.utils.TransitScheduleValidator;
import org.matsim.pt.utils.TransitScheduleValidator.ValidationResult;
import org.matsim.pt2matsim.run.PublicTransitMapper;

import run.Run;

public class GeneralTestin {
public static void main(String[] args) {
	
	String netLocation = "data/osm/8000Restrictions760ArtificialAllmode/osmMultimodal.xml";
	String lanesLocation = "data/osm/8000Restrictions760ArtificialAllmode/testLanes_out.xml";
	String tsLocation = "data/osm/8000Restrictions760ArtificialAllmode/osmTsMapped.xml";
	
//	Network testNet = NetworkUtils.createNetwork();
//	Node n1 = NetworkUtils.createAndAddNode(testNet, Id.createNodeId("1"), new Coord(1000,1000));
//	
//	Node n2 = NetworkUtils.createAndAddNode(testNet, Id.createNodeId("2"), new Coord(1100,100));
//	
//	Node n3 = NetworkUtils.createAndAddNode(testNet, Id.createNodeId("3"), new Coord(900,100));
//	
//	Node n4 = NetworkUtils.createAndAddNode(testNet, Id.createNodeId("4"), new Coord(1000,0));
//	
//	Node n5 = NetworkUtils.createAndAddNode(testNet, Id.createNodeId("5"), new Coord(1000,-1000));
//	
//	Link l14 = NetworkUtils.createAndAddLink(testNet, Id.createLinkId("1_4"), n1, n4, 17,1000, 1800, 1, null, null);
//	
//	Link l34 = NetworkUtils.createAndAddLink(testNet, Id.createLinkId("3_4"), n3, n4, 17,1000, 1800, 1, null, null);
//	Link l24 = NetworkUtils.createAndAddLink(testNet, Id.createLinkId("2_4"), n2, n4, 17,1000, 1800, 1, null, null);
//	Link l41 = NetworkUtils.createAndAddLink(testNet, Id.createLinkId("4_1"), n4, n1, 17,1000, 1800, 1, null, null);
//	Link l42 = NetworkUtils.createAndAddLink(testNet, Id.createLinkId("4_2"), n4, n2, 17,1000, 1800, 1, null, null);
//	Link l43 = NetworkUtils.createAndAddLink(testNet, Id.createLinkId("4_3"), n4, n3, 17,1000, 1800, 1, null, null);
//	Link l54 = NetworkUtils.createAndAddLink(testNet, Id.createLinkId("5_4"), n5, n4, 17,1000, 1800, 1, null, null);
//	Link l45 = NetworkUtils.createAndAddLink(testNet, Id.createLinkId("4_5"), n4, n5, 17,1000, 1800, 1, null, null);
//	
//	System.out.println("for 14 to 43 = "+EmNetworkCreator.getAngle(l14, l43));
//	System.out.println("for 14 to 42 = "+EmNetworkCreator.getAngle(l14, l42));
//	System.out.println("for 34 to 41 = "+EmNetworkCreator.getAngle(l34, l41));
//	System.out.println("for 34 to 42 = "+EmNetworkCreator.getAngle(l34, l42));
//	System.out.println("for 54 to 43 = "+EmNetworkCreator.getAngle(l54, l43));
//	System.out.println("for 54 to 42 = "+EmNetworkCreator.getAngle(l54, l42));
//	new NetworkWriter(testNet).write("testNetToy.xml");
//	
//	for(Link link:testNet.getLinks().values()) {
//	
//		Map<Id<Link>,Double>order =  EmNetworkCreator.getOrderedAnglesforToLinks(link);
//		List<Id<Link>> lefts = new ArrayList<>();
//		List<Id<Link>> rights = new ArrayList<>();
//		List<Id<Link>> straight = new ArrayList<>();
//		
//		for(Entry<Id<Link>, Double> d:order.entrySet()) {
//			if(Double.compare(Math.abs(d.getValue()), 180)==0) {
//				continue;
//			}else if(d.getValue()>45) {
//				lefts.add(d.getKey());
//			}else if(d.getValue()<-45) {
//				rights.add(d.getKey());
//			}else {
//				straight.add(d.getKey());
//			}
//		}
//		System.out.println("For link "+ link.getId());
//		System.out.println(lefts);
//		System.out.println(rights);
//		System.out.println(straight);
//	}
	Config config = ConfigUtils.createConfig();
	config.network().setInputFile(netLocation);
	config.transit().setTransitScheduleFile(tsLocation);
	config.network().setLaneDefinitionsFile(lanesLocation);
	Scenario scn = ScenarioUtils.loadScenario(config);
	Network net = scn.getNetwork();
	Lanes lanes = scn.getLanes();
	TransitSchedule ts = scn.getTransitSchedule();
	ValidationResult result = TransitScheduleValidator.validateAll(ts, net);
	PublicTransitMapper.checkConsistensy(net, ts, lanes);
	
	Run.runLaneBasedNetworkCleaner(net,lanes, true);
	new NetworkCleaner().run( net);
	new NetworkWriter(net).write("testNetCleaned.xml");
	double wrongRoute = 0;
	double total = 0;
	for(TransitLine tl:ts.getTransitLines().values()){
		for(TransitRoute tr:tl.getRoutes().values()) {
			total++;
			if(!net.getLinks().containsKey(tr.getRoute().getStartLinkId())){
				wrongRoute++;
				//continue;
			}
			if(!net.getLinks().containsKey(tr.getRoute().getEndLinkId())){
				wrongRoute++;
				//continue;
			}
			for(Id<Link> l: tr.getRoute().getLinkIds()) {
				if(!net.getLinks().containsKey(l)){
					wrongRoute++;
					//continue;
				}	
			}
		}
	}
	System.out.println(wrongRoute +"out of "+ total);
}
}
