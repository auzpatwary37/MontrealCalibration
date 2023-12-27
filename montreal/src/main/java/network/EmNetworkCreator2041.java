package network;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.lanes.Lane;
import org.matsim.lanes.Lanes;
import org.matsim.lanes.LanesFactory;
import org.matsim.lanes.LanesToLinkAssignment;
import org.matsim.lanes.LanesUtils;
import org.matsim.lanes.LanesWriter;
import org.matsim.pt.utils.TransitScheduleValidator;
import org.matsim.pt.utils.TransitScheduleValidator.ValidationResult;
import org.matsim.pt2matsim.config.PublicTransitMappingConfigGroup;
import org.matsim.pt2matsim.run.CheckMappedSchedulePlausibility;
import org.matsim.pt2matsim.run.Gtfs2TransitSchedule;
import org.matsim.pt2matsim.run.PublicTransitMapper;

public class EmNetworkCreator2041 {
public static void main(String[] args) throws IOException {
	String mathildeOsmConverterConfig = "data/osm/osm_config.xml";
	String osmNet = "data/osm/RegionMontrealaise.osm";
	String emNodes = "data/kinan/emNodes2041.csv";
	String emLinks = "data/kinan/emLinks2041.csv";
	String emTurns = "data/kinan/emme_Turns2041.csv";
	
	Network outNet = NetworkUtils.createNetwork();
	NetworkFactory netFac = outNet.getFactory();
	
	Reader emNodeIn = new FileReader(emNodes);
	Reader emLinksIn = new FileReader(emLinks);
	Reader emTurnsIn = new FileReader(emTurns);
	
	String[] headerNode = "ID,X,Y,DATA1,DATA2,DATA3,ISZONE,ISINTERSEC,LABEL".split(",");
	String[] headerLink = "ID,INODE,JNODE,LENGTH,TYPE,MODES,LANES,VDF,DATA1,DATA2,DATA3,VOLAU,VOLAD,TIMAU,@cam,@caphl,@dfah,@dfah0,@dfah1,@dfah10,@dfah11,@dfah12,@dfah13,@dfah14,@dfah15,@dfah16,@dfah17,@dfah18,@dfah19,@dfah2,@dfah20,@dfah21,@dfah22,@dfah23,@dfah3,@dfah4,@dfah5,@dfah6,@dfah7,@dfah8,@dfah9,@ftim,@jur,@lanam,@lanhp,@lanpm,@moves,@peage_v,@peage_w,@peage_y,@peage_z,@sm,@tau0,@tau1,@tau10,@tau11,@tau12,@tau13,@tau14,@tau15,@tau16,@tau17,@tau18,@tau19,@tau2,@tau20,@tau21,@tau22,@tau23,@tau3,@tau4,@tau5,@tau6,@tau7,@tau8,@tau9,@tauff_0,@tauff_1,@tauff_10,@tauff_11,@tauff_12,@tauff_13,@tauff_14,@tauff_15,@tauff_16,@tauff_17,@tauff_18,@tauff_19,@tauff_2,@tauff_20,@tauff_21,@tauff_22,@tauff_23,@tauff_3,@tauff_4,@tauff_5,@tauff_6,@tauff_7,@tauff_8,@tauff_9,@vdfam,@vdfhp,@vdfpm,@vlc0,@vlc1,@vlc10,@vlc11,@vlc12,@vlc13,@vlc14,@vlc15,@vlc16,@vlc17,@vlc18,@vlc19,@vlc2,@vlc20,@vlc21,@vlc22,@vlc23,@vlc3,@vlc4,@vlc5,@vlc6,@vlc7,@vlc8,@vlc9,@vlo0,@vlo1,@vlo10,@vlo11,@vlo12,@vlo13,@vlo14,@vlo15,@vlo16,@vlo17,@vlo18,@vlo19,@vlo2,@vlo20,@vlo21,@vlo22,@vlo23,@vlo3,@vlo4,@vlo5,@vlo6,@vlo7,@vlo8,@vlo9,@vlp0,@vlp1,@vlp10,@vlp11,@vlp12,@vlp13,@vlp14,@vlp15,@vlp16,@vlp17,@vlp18,@vlp19,@vlp2,@vlp20,@vlp21,@vlp22,@vlp23,@vlp3,@vlp4,@vlp5,@vlp6,@vlp7,@vlp8,@vlp9,@vrg0,@vrg1,@vrg10,@vrg11,@vrg12,@vrg13,@vrg14,@vrg15,@vrg16,@vrg17,@vrg18,@vrg19,@vrg2,@vrg20,@vrg21,@vrg22,@vrg23,@vrg3,@vrg4,@vrg5,@vrg6,@vrg7,@vrg8,@vrg9".split(",");
	String[] headerTurn = "ID,JNODE,INODE,KNODE,TPF,DATA1,DATA2,DATA3,VOLAU,VOLAD,TIMAU".split(",");
	
	CSVFormat csvFormatNode = CSVFormat.DEFAULT.builder()
	        .setHeader(headerNode)
	        .setSkipHeaderRecord(true)
	        .build();
	
	CSVFormat csvFormatLink = CSVFormat.DEFAULT.builder()
	        .setHeader(headerLink)
	        .setSkipHeaderRecord(true)
	        .build();
	CSVFormat csvFormatTurn = CSVFormat.DEFAULT.builder()
	        .setHeader(headerTurn)
	        .setSkipHeaderRecord(true)
	        .build();
	
	Iterable<CSVRecord> recordsNodes = csvFormatNode.parse(emNodeIn);
	
	for (CSVRecord record : recordsNodes) {
        Node node = netFac.createNode(Id.createNodeId(record.get("ID")),new Coord(Double.parseDouble(record.get("X")),Double.parseDouble(record.get("Y"))));
        node.getAttributes().putAttribute("DATA1", record.get("DATA1"));
        node.getAttributes().putAttribute("DATA2", record.get("DATA2"));
        node.getAttributes().putAttribute("DATA3", record.get("DATA3"));
        node.getAttributes().putAttribute("ISZONE", record.get("ISZONE"));
        node.getAttributes().putAttribute("ISINTERSEC", record.get("ISINTERSEC"));
        node.getAttributes().putAttribute("LABEL", record.get("LABEL"));
        outNet.addNode(node);
	}
	
	Iterable<CSVRecord> recordsLinks = csvFormatLink.parse(emLinksIn);

	for (CSVRecord record : recordsLinks) {
		Id<Link> linkId = Id.createLinkId(record.get("ID"));
        Id<Node> fromNodeId = Id.createNodeId(record.get("INODE"));
        Id<Node> toNodeId = Id.createNodeId(record.get("JNODE"));
        Double length = Double.parseDouble(record.get("LENGTH"))*1000;// looks like it is in km so multiplied by 1000
        int laneAm = (int)Double.parseDouble(record.get("@lanam"));
        int lanepm = (int)Double.parseDouble(record.get("@lanpm"));
        int laneoffpeak = (int)Double.parseDouble(record.get("@lanhp"));
        double capacityTheoretical = Double.parseDouble(record.get("@caphl"));
        int type = Integer.parseInt(record.get("TYPE"));
        String modes = record.get("MODES");
        int cam = (int)Double.parseDouble(record.get("@cam"));
        Link link = netFac.createLink(linkId, outNet.getNodes().get(fromNodeId), outNet.getNodes().get(toNodeId));
        link.setLength(length);
        if(laneAm==0) {
        	System.out.println("Number of lane zero");
        }
        link.setCapacity(capacityTheoretical*laneAm);
        link.setNumberOfLanes(laneAm);
        link.getAttributes().putAttribute("type_em", type);
        switch(type) {
        case 7:
        	link.setFreespeed(40*1000/3600);
        	break;
        case 6:
        	link.setFreespeed(50*1000/3600);
        	break;
        case 5:
        	link.setFreespeed(60*1000/3600);
        	break;
        case 4:
        	link.setFreespeed(70*1000/3600);
        	break;
        case 3:
        	link.setFreespeed(90*1000/3600);
        	break;
        case 2:
        	link.setFreespeed(100*1000/3600);
        	break;
        case 1:
        	link.setFreespeed(100*1000/3600);
        	break;
        default:
        	link.setFreespeed(100*1000/3600);
        	break;
        }

        link.getAttributes().putAttribute("cam", cam);
        link.getAttributes().putAttribute("modes", modes);
        link.getAttributes().putAttribute("lanes_am", laneAm);
        link.getAttributes().putAttribute("lanes_pm", lanepm);
        link.getAttributes().putAttribute("lanes_offpeak", laneoffpeak);
        Set<String> modeString = new HashSet<>();
        modeString.add("car");
        modeString.add("bus");
        modeString.add("car_passenger");
        link.setAllowedModes(modeString);
        outNet.addLink(link);
	}
	
	NetworkWithLanesTrial.addTransit(outNet,"data/osm/rail.osm");
	
	Iterable<CSVRecord> recordsTurns = csvFormatTurn.parse(emTurnsIn);
	Map<Id<Link>,Map<Id<Link>,Integer>> restrictions = new HashMap<>();
	
	for (CSVRecord record : recordsTurns) {
		String id = record.get("ID");
		String[] ids = id.split("-");
		Id<Link> fromLink = Id.create(ids[0]+"-"+ids[1], Link.class);
		Id<Link> toLink = Id.create(ids[1]+"-"+ids[2], Link.class);
		if(!restrictions.containsKey(fromLink))restrictions.put(fromLink, new HashMap<>());
		restrictions.get(fromLink).put(toLink, (int)Double.parseDouble(record.get("TPF")));
	}
	
	Lanes lanes = LanesUtils.createLanesContainer();
	LanesFactory laneFac = lanes.getFactory();
	
	for(Entry<Id<Link>, Map<Id<Link>, Integer>> fromLinkEntry:restrictions.entrySet()) {
		if(outNet.getLinks().get(fromLinkEntry.getKey())!=null) {
			Link fromLink = outNet.getLinks().get(fromLinkEntry.getKey());
			Map<Id<Link>, Integer> order = orderToLinks(fromLink,fromLinkEntry.getValue());
			Map<Id<Link>, Double> assignedLane  = distributeLanes(fromLinkEntry.getValue(),order,fromLink.getNumberOfLanes());
			LanesToLinkAssignment l2l = laneFac.createLanesToLinkAssignment(fromLink.getId());
			for(Entry<Id<Link>, Integer> o:order.entrySet()) {
				Link toLink = outNet.getLinks().get(o.getKey());
				Lane l = laneFac.createLane(Id.create(fromLink.getFromNode().getId().toString()+"-"+fromLink.getToNode().getId().toString()+"-"+toLink.getToNode().getId().toString(), Lane.class));
				l.setAlignment(o.getValue());
				l.setCapacityVehiclesPerHour(1800*assignedLane.get(toLink.getId()));
				l.setStartsAtMeterFromLinkEnd(100);
				l.setNumberOfRepresentedLanes(assignedLane.get(toLink.getId()));
				l.addToLinkId(o.getKey());
				l2l.addLane(l);
			}
			
			if(!l2l.getLanes().isEmpty())lanes.addLanesToLinkAssignment(l2l);
		}
	}
	NetworkUtils.runNetworkCleaner(outNet);
	
	new LanesWriter(lanes).write("data/kinan/emLanes2041.xml");
	new NetworkWriter(outNet).write("data/kinan/emNetworkAm2041.xml");
	
	
	
	Gtfs2TransitSchedule.run("data/kinan/gtfsData/out/", "dayWithMostTrips", "epsg:32188", "data/kinan/emTs.xml", "data/kinan/emVehicles2041.xml");
	
	Config config = ConfigUtils.createConfig();
	//CreateDefaultPTMapperConfig.main(new String[]{"data/kinan/ptMapperConfig.xml"});
	//ConfigUtils.loadConfig(config,"data/kinan/ptMapperConfig.xml");
	
	ConfigGroup c = PublicTransitMappingConfigGroup.createDefaultConfig();
	c = PublicTransitMappingConfigGroup.loadConfig(mathildeOsmConverterConfig);
	config.addModule(c);
	
	PublicTransitMappingConfigGroup configPt = ConfigUtils.addOrGetModule(config, PublicTransitMappingConfigGroup.GROUP_NAME, PublicTransitMappingConfigGroup.class); 

	Set<String> toRemove = config.getModules().keySet().stream().filter(module -> !module.equals(PublicTransitMappingConfigGroup.GROUP_NAME)).collect(Collectors.toSet());
	toRemove.forEach(config::removeModule);
	
	configPt.setInputNetworkFile("data/kinan/emNetworkAm2041.xml");
	config.network().setLaneDefinitionsFile("data/kinan/emLanes2041.xml");
	configPt.setInputScheduleFile("data/kinan/emTs.xml");
	configPt.setOutputNetworkFile("data/kinan/emMultimodal2041.xml");
	configPt.setOutputScheduleFile("data/kinan/emTsMapped2041.xml");
	
	
	configPt.setCandidateDistanceMultiplier(7);
	configPt.setMaxLinkCandidateDistance(85);
	configPt.setMaxTravelCostFactor(1000);
	configPt.setNLinkThreshold(4);
	configPt.setNumOfThreads(10);
	
	Map<String,Set<String>> modes = new HashMap<>();
	modes.put("bus", new HashSet<>());
	modes.put("subway", new HashSet<>());
	modes.put("rail", new HashSet<>());
	
	modes.get("bus").add("bus");
	modes.get("bus").add("car");
	modes.get("bus").add("car_passenger");
	
	modes.get("subway").add("subway");
	
	
	modes.get("rail").add("rail");
	modes.get("rail").add("light_rail");
	
	configPt.setTransportModeAssignment(modes);
	Set<String> modesToKeep = new HashSet<>();
	modesToKeep.add("car");
	modesToKeep.add("car_passenger");
//	modesToKeep.add("subway");
//	modesToKeep.add("pt");
	
	configPt.setModesToKeepOnCleanUp(modesToKeep);

	new ConfigWriter(config).write("data/kinan/ptMapperConfig.xml");
	
	//CreateDefaultPTMapperConfig.main(new String[]{"data/kinan/ptMapperConfig.xml"});
	
	PublicTransitMapper.run(config, configPt);
	Network outNetFinal = NetworkUtils.readNetwork("data/kinan/emMultimodal2041.xml");
	CheckMappedSchedulePlausibility.run("data/kinan/emTsMapped2041.xml", "data/kinan/emMultimodal2041.xml", "epsg:32188", "data/kinan/plausibility");
	
	Config cc = ConfigUtils.createConfig();
	cc.transit().setTransitScheduleFile("data/kinan/emTsMapped2041.xml");
	cc.network().setInputFile( "data/kinan/emMultimodal2041.xml");
	Scenario scn = ScenarioUtils.loadScenario(cc);
	
	ValidationResult r = TransitScheduleValidator.validateAll(scn.getTransitSchedule(), scn.getNetwork());
	System.out.println("transit is valid? "+ r.isValid());
	
}
/**
 * 
 * @param link the from Link for which lanes are to be ordered.
 * @param restrictions the lane restrictions. if null then will assume no restrictions.
 * @return
 */
public static Map<Id<Link>,Integer> orderToLinks(Link link, Map<Id<Link>,Integer>restrictions) {
	Map<Id<Link>, Integer> order = new LinkedHashMap<>();
	TreeMap<Double,Id<Link>> angle = new TreeMap<>();
	List<Tuple<Id<Link>,Double>> angles = new ArrayList<>();
	
	for(Link l: link.getToNode().getOutLinks().values()) {
		if(restrictions==null || restrictions.containsKey(l.getId()) && restrictions.get(l.getId())!=0) {
			double a =  getAngle(link,l);
			angles.add(new Tuple<>(l.getId(),a));
			
		}
	}
	
	angles.sort(new Comparator<Tuple<Id<Link>,Double>>(){

		@Override
		public int compare(Tuple<Id<Link>, Double> o1, Tuple<Id<Link>, Double> o2) {
			if (o1.getSecond() < o2.getSecond()) return -1;
	        if (o1.getSecond() > o2.getSecond()) return 1;
	        return 0;
		}
	});
	
	int nPlus = 0;
	int nMinus = 0;
	if(angles.size()%2==0) {
		nPlus = angle.size()/2-1;
		nMinus = -1*angle.size()/2;
	}else {
		nPlus = (angle.size()-1)/2;
		nMinus = -1*(angle.size()-1)/2;
	}
	int j = 0;
	for(int i = nMinus;i<=nPlus;i++) {
		angles.get(j);
		order.put(angles.get(j).getFirst(),i);
		j++;
	}
	
	return order;
}


/**
 * 
 * @param link the from Link for which lanes are to be ordered.
 * @return the angle in order from negative to positive
 */
public static Map<Id<Link>,Double> getOrderedAnglesforToLinks(Link link) {
	Map<Id<Link>, Double> order = new LinkedHashMap<>();
	List<Tuple<Id<Link>,Double>> angles = new ArrayList<>();
	
	for(Link l: link.getToNode().getOutLinks().values()) {
		if( l.getAllowedModes().containsAll(link.getAllowedModes())) {
			double a =  getAngle(link,l);
			angles.add(new Tuple<>(l.getId(),a));
		}
		
	}
	
	angles.sort(new Comparator<Tuple<Id<Link>,Double>>(){

		@Override
		public int compare(Tuple<Id<Link>, Double> o1, Tuple<Id<Link>, Double> o2) {
			if (o1.getSecond() < o2.getSecond()) return -1;
	        if (o1.getSecond() > o2.getSecond()) return 1;
	        return 0;
		}
	});
	
	
	for(int i = 0;i<angles.size();i++) {
		order.put(angles.get(i).getFirst(), angles.get(i).getSecond());
	}
	
	return order;
}

public static Map<Id<Link>,Double> distributeLanes(Map<Id<Link>,Integer> restrictions, Map<Id<Link>,Integer> order,double numOfLane){
	Map<Id<Link>,Double> out = new HashMap<>();
	Map<Id<Link>,Double> weight = new HashMap<>();
	double sum = 0;
	for(Id<Link> l:order.keySet()) {
		weight.put(l, 1./(2+restrictions.get(l)));
		sum+= weight.get(l);
	}
	for(Entry<Id<Link>, Double> w:weight.entrySet()) {
		out.put(w.getKey(),numOfLane*w.getValue()/sum);
	}
	return out;
}

public static double getAngle(Link l1, Link l2) {
	
	Tuple<Double,Double> vec1 = new Tuple<>(l1.getToNode().getCoord().getX()-l1.getFromNode().getCoord().getX(),l1.getToNode().getCoord().getY()-l1.getFromNode().getCoord().getY());
	Tuple<Double,Double> vec2 = new Tuple<>(l2.getToNode().getCoord().getX()-l2.getFromNode().getCoord().getX(),l2.getToNode().getCoord().getY()-l2.getFromNode().getCoord().getY());
	
	double dot = vec1.getFirst()*vec2.getFirst()+vec1.getSecond()*vec2.getSecond();
	
	double det = vec1.getFirst()*vec2.getSecond() - vec1.getSecond()*vec2.getFirst();    
	double angle = Math.toDegrees(Math.atan2(det, dot));  
	
	return angle;
}





}
