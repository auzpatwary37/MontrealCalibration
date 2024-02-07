package montreal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.facilities.ActivityFacility;

import population.HouseHold;

public class GeneralTestin {
public static void main(String[] args) {
	
 Population newPop = PopulationUtils.readPopulation("data\\outputODPopulation_18_1.0.xml.gz");
 Population oldPop = PopulationUtils.readPopulation("data\\population\\outputODPopulation_41_0.05.xml.gz");
 
 System.out.println("number of person in old OD = "+oldPop.getPersons().size());
 System.out.println("number of person in new OD = "+newPop.getPersons().size());
 
 int tripPerson = 0;
 int noTripPerson = 0;
 int tripMakingPopulation = 0;
 int hhPersonOld = 0;
 int hhPersonNew = 0;
 Set<String> oldHH = new HashSet<>();
 Set<String> newHH = new HashSet<>();
 
 for(Person p:newPop.getPersons().values()) {
	 if(p.getSelectedPlan().getPlanElements().size()<=1) {
		 noTripPerson++;
	 }else if(p.getSelectedPlan().getPlanElements().size()==3){
		tripPerson++; 
	 }else {
		 tripMakingPopulation++;
	 }
 }
Map<Id<ActivityFacility>,Integer> totalTrips = new HashMap<>();
for(Person p:oldPop.getPersons().values()){
	if(p.getAttributes().getAttribute("household id")!=null) {
		hhPersonOld++;
		oldHH.add((String) p.getAttributes().getAttribute("household id"));
	}
	List<Trip> trips = TripStructureUtils.getTrips(p.getSelectedPlan());
	trips.forEach(tp->{
		if(!totalTrips.containsKey(tp.getOriginActivity().getFacilityId()))totalTrips.put(tp.getOriginActivity().getFacilityId(),0);
		totalTrips.compute(tp.getOriginActivity().getFacilityId(),(k,v)->v=v+1);
	});
}

Map<Id<ActivityFacility>,Integer> totalTripsNew = new HashMap<>();
for(Person p:newPop.getPersons().values()){
	List<Trip> trips = TripStructureUtils.getTrips(p.getSelectedPlan());
	if(p.getAttributes().getAttribute("household id")!=null) {
		hhPersonNew++;
		newHH.add((String) p.getAttributes().getAttribute("household id"));
	}
	trips.forEach(tp->{
		if(!totalTripsNew.containsKey(tp.getOriginActivity().getFacilityId()))totalTripsNew.put(tp.getOriginActivity().getFacilityId(),0);
		totalTripsNew.compute(tp.getOriginActivity().getFacilityId(),(k,v)->v=v+1);
	});
}
int oldTrip = 0;
for(int i:totalTrips.values()) {
	oldTrip+=i;
}
int newTrip = 0;
for(int i:totalTripsNew.values()) {
	newTrip+=i;
}
 System.out.println("tripPerson = "+tripPerson);
 System.out.println("noTripPerson = "+noTripPerson);
 System.out.println("hh person old = "+hhPersonOld);
 System.out.println("hh person new = "+hhPersonNew);
 System.out.println("hh new = "+newHH.size());
 System.out.println("hh old = "+oldHH.size());
 System.out.println("tripMakingPopulation = "+tripMakingPopulation);
 System.out.println("total old Trips = "+oldTrip);
 System.out.println("total new Trips = "+newTrip);
// 
// Config c;
 
// Network net = NetworkUtils.readNetwork("data\\emDetails\\emMultimodal.xml");
// Network net41 = NetworkUtils.readNetwork("data\\emDetails\\emMultimodal2041Maxime.xml");
// double[] maxOld = NetworkUtils.getBoundingBox(net.getNodes().values());
// double[] maxNew = NetworkUtils.getBoundingBox(net41.getNodes().values());
// 
// System.out.println(Arrays.toString(maxOld));
// System.out.println(Arrays.toString(maxNew));
	
//String netLocation = "data/osm/valid1252OSM/osmMultimodal.xml";
//String lanesLocation = "data/osm/valid1252OSM/testLanes_out2041.xml";
//	String tsLocation = "data/osm/valid1252OSM/osmTsMapped.xml";
//	
//	String emNet = "data/kinan/emMultimodal.xml";
//	String emNetOut = "data/kinan/emMultimodalTagged.xml";
//	String emNet2041 = "data/kinan/emMultimodal2041.xml";
//	String emNet2041Out = "data/kinan/emMultimodal2041Tagged.xml";
//	
//	identifyChanges(emNet,emNet2041,emNetOut,emNet2041Out);
	
//	String netLocation = "data/kinan/emMultimodal.xml";
//	String lanesLocation = "data/kinan/emLanes.xml";
//	String tsLocation = "data/kinan/emTsMapped.xml";

	
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
//Config config = ConfigUtils.createConfig();
//config.network().setInputFile(netLocation);
////	config.transit().setTransitScheduleFile(tsLocation);
//config.network().setLaneDefinitionsFile(lanesLocation);
//Scenario scn = ScenarioUtils.loadScenario(config);
//Network net = scn.getNetwork();
//Lanes lanes = scn.getLanes();
//
//new HashSet<>(lanes.getLanesToLinkAssignments().keySet()).forEach(l->{
//	if(!net.getLinks().containsKey(l)) {
//		lanes.getLanesToLinkAssignments().remove(l);
//		return;
//	}
//	for(Lane lane:lanes.getLanesToLinkAssignments().get(l).getLanes().values()) {
//		for(Id<Link> lid:new ArrayList<>(lane.getToLinkIds())) {
//			if(!net.getLinks().containsKey(lid)) {
//				lane.getToLinkIds().remove(lid);
//			}
//		}
//		if(lane.getToLinkIds().isEmpty())lanes.getLanesToLinkAssignments().get(l).getLanes().remove(lane.getId());
//	}
//	if(lanes.getLanesToLinkAssignments().get(l).getLanes().size()==0)lanes.getLanesToLinkAssignments().remove(l);
//});
//
//new LanesWriter(lanes).write("data/osm/valid1252OSM/testLanes_out.xml");

//	TransitSchedule ts = scn.getTransitSchedule();
//	ValidationResult result = TransitScheduleValidator.validateAll(ts, net);
//	PublicTransitMapper.checkConsistensy(net, ts, lanes);
//	
//
//	System.out.println(lanes.getLanesToLinkAssignments().size());
//	
//	int laneNo = 0;
//	for(LanesToLinkAssignment l:lanes.getLanesToLinkAssignments().values()) {
//		laneNo+=l.getLanes().size();
//	}
//	
//	
//	
//	System.out.println(laneNo);
//	
//	Run.runLaneBasedNetworkCleaner(net,lanes, true);
//	new NetworkCleaner().run( net);
//	new NetworkWriter(net).write("testNetCleaned.xml");
//	double wrongRoute = 0;
//	double total = 0;
//	for(TransitLine tl:ts.getTransitLines().values()){
//		for(TransitRoute tr:tl.getRoutes().values()) {
//			total++;
//			if(!net.getLinks().containsKey(tr.getRoute().getStartLinkId())){
//				wrongRoute++;
//				//continue;
//			}
//			if(!net.getLinks().containsKey(tr.getRoute().getEndLinkId())){
//				wrongRoute++;
//				//continue;
//			}
//			for(Id<Link> l: tr.getRoute().getLinkIds()) {
//				if(!net.getLinks().containsKey(l)){
//					wrongRoute++;
//					//continue;
//				}	
//			}
//		}
//	}
//	System.out.println(wrongRoute +"out of "+ total);
//
//	CheckMappedSchedulePlausibility.run(tsLocation, netLocation, "epsg:32188", "data/osm/plausibility");

}

public static String getMode(Trip trip) {
	for(Leg l:trip.getLegsOnly()) {
		if(l.getMode().contains("pt"))return "pt";
		if(l.getMode().contains("car"))return "car";
		if(l.getMode().contains("car_passenger"))return "car_passenger";
	}
	return "non mechanical";
}

public static void identifyChanges(String net1In,String n2In, String outNet1, String outNet2) {
	Network net1 = NetworkUtils.readNetwork(net1In);
	Network n2 = NetworkUtils.readNetwork(n2In);
	net1.getLinks().values().forEach(l->{
		if(!l.getId().toString().contains("pt") && n2.getLinks().get(l.getId())==null) {
			l.getAttributes().putAttribute("changed", true);
			return;
		}else {
			Link l2 = n2.getLinks().get(l.getId());
			if(l.getId().toString().contains("pt"))return;
			if(l.getNumberOfLanes()!=l2.getNumberOfLanes()) {
				l.getAttributes().putAttribute("changed", true);
				return;
			}
			if(Math.abs(l.getCapacity()-l2.getCapacity())>.01) {
				l.getAttributes().putAttribute("changed", true);
				return;
			}

			if(Math.abs(l.getLength()-l2.getLength())>0.01) {
				l.getAttributes().putAttribute("changed", true);
				return;
			}

		}
		l.getAttributes().putAttribute("changed", false);
	});

	n2.getLinks().values().forEach(l->{
		if(net1.getLinks().get(l.getId())==null) {
			l.getAttributes().putAttribute("changed", true);
			return;
		}else {
			Link l2 = net1.getLinks().get(l.getId());
			if(l.getNumberOfLanes()!=l2.getNumberOfLanes()) {
				l.getAttributes().putAttribute("changed", true);
				return;
			}
			if(Math.abs(l.getCapacity()-l2.getCapacity())>.01) {
				l.getAttributes().putAttribute("changed", true);
				return;
			}

			if(Math.abs(l.getLength()-l2.getLength())>0.01) {
				l.getAttributes().putAttribute("changed", true);
				return;
			}

		}
		l.getAttributes().putAttribute("changed", false);
	});
	new NetworkWriter(net1).write(outNet1);
	new NetworkWriter(n2).write(outNet2);

}



}
