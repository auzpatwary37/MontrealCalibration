package network;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.network.SignalsAndLanesOsmNetworkReader;
import org.matsim.contrib.signals.utils.SignalUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.lanes.Lane;
import org.matsim.lanes.Lanes;
import org.matsim.lanes.LanesToLinkAssignment;
import org.matsim.lanes.LanesUtils;
import org.matsim.lanes.LanesWriter;
import org.matsim.pt2matsim.config.PublicTransitMappingConfigGroup;
import org.matsim.pt2matsim.run.CheckMappedSchedulePlausibility;
import org.matsim.pt2matsim.run.Gtfs2TransitSchedule;
import org.matsim.pt2matsim.run.PublicTransitMapper;

public class NetworkWithLanesTrial {
	@SuppressWarnings("deprecation")
	public static void main(String[] args) {
		String mathildeOsmConverterConfig = "data/osm/osm_config.xml";
		String osmNet = "data/osm/RegionMontrealaise.osm";
		String osmTransit = "data/osm/fixOsm.osm";
		String outputNet = "data/osm/outputNet.xml";
		String outputLanes = "data/osm/outputLanes.xml";
		String gtfsFolder = "data/kinan/gtfsData/out/";
		String outTs = "data/osm/osmTs.xml";
		String outTv= "data/osm/osmVehicles.xml";
		String finalnet = "data/osm/osmMultimodal.xml";
		String mappedTs = "data/osm/osmTsMapped.xml";
		String ptMapperConfig = "data/osm/ptMapperConfig.xml";
		int thread = 12;
		double distanceMultiplier = 5;
		double candidateDistance = 300;
		double maxTravelCostFactor = 200;
		int nLink = 10;
		
		if(args.length!=0) {
				distanceMultiplier = Double.parseDouble(args[0]);
				candidateDistance = Double.parseDouble(args[1]);
				maxTravelCostFactor = Double.parseDouble(args[2]);
				nLink = Integer.parseInt(args[3]);
			}
//		
//		Network network = NetworkUtils.createNetwork();
//		CoordinateTransformation coordTransfer = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:32188");
//		SignalSystemsConfigGroup signalConfig = new SignalSystemsConfigGroup();
//		SignalsData signals = SignalUtils.createSignalsData(signalConfig);
//		Lanes lanes = LanesUtils.createLanesContainer();
//		SignalsAndLanesOsmNetworkReader netReader = new SignalsAndLanesOsmNetworkReader(network, coordTransfer, true, signals, lanes);
//		
//		netReader.setBoundingBox(45.0, -74.7, 46.15, -72.8);
//		
//		netReader.setAcceptFourPlusCrossings(true);
//		netReader.setAllowUTurnAtLeftLaneOnly(true);
//		netReader.setKeepPaths(false);
//		netReader.setMakePedestrianSignals(true);
//		netReader.parse(osmNet);//"data/osm/Region Montrealaise.osm"
//		
//		addTransit(network,osmTransit);
//		//NetworkUtils.runNetworkCleaner(network);
//		System.out.println(checkForBadCapacity(network));
//		int wrongLane = 0;
//		int totalLane = 0;
//		for(Entry<Id<Link>, LanesToLinkAssignment> l2l:new HashMap<>(lanes.getLanesToLinkAssignments()).entrySet()){
//			if(l2l.getValue().getLanes().isEmpty()||l2l.getValue().getLanes()==null) {
//				lanes.getLanesToLinkAssignments().remove(l2l.getKey());
//				wrongLane++;
//				continue;
//				
//			}
//			for(Entry<Id<Lane>, Lane> lane:new HashMap<>(l2l.getValue().getLanes()).entrySet()){
//				totalLane++;
//				if((lane.getValue().getToLinkIds()==null || lane.getValue().getToLinkIds().isEmpty()) && (lane.getValue().getToLaneIds()==null || lane.getValue().getToLaneIds().isEmpty())) {
//					wrongLane++;
//					l2l.getValue().getLanes().remove(lane.getKey());		
//					if(l2l.getValue().getLanes().isEmpty())lanes.getLanesToLinkAssignments().remove(l2l.getKey());
//				}
//			};
//		};
//		if(args.length!=0) {
//			
//		}
//		new NetworkWriter(network).write(outputNet);
//		new LanesWriter(lanes).write(outputLanes);
		
		
//		System.out.println("Wrong Lanes = "+wrongLane+" out of "+totalLane);
		
		Gtfs2TransitSchedule.run(gtfsFolder, "dayWithMostServices", "epsg:32188", outTs,outTv);
		
		Config config = ConfigUtils.createConfig();
		//CreateDefaultPTMapperConfig.main(new String[]{"data/kinan/ptMapperConfig.xml"});
		//ConfigUtils.loadConfig(config,"data/kinan/ptMapperConfig.xml");
		
		ConfigGroup cDefault = PublicTransitMappingConfigGroup.createDefaultConfig();
		ConfigGroup c = PublicTransitMappingConfigGroup.loadConfig(mathildeOsmConverterConfig);
		config.addModule(c);
		
		PublicTransitMappingConfigGroup configPt = ConfigUtils.addOrGetModule(config, PublicTransitMappingConfigGroup.GROUP_NAME, PublicTransitMappingConfigGroup.class); 

//		Set<String> toRemove = config.getModules().keySet().stream().filter(module -> !module.equals(PublicTransitMappingConfigGroup.GROUP_NAME)).collect(Collectors.toSet());
//		toRemove.forEach(config::removeModule);
		
		configPt.setInputNetworkFile("data/osm/testNet.xml");//outputNet
		config.network().setLaneDefinitionsFile("data/osm/testLanes.xml");//outputLanes
		configPt.setInputScheduleFile(outTs);
		configPt.setOutputNetworkFile(finalnet);
		configPt.setOutputScheduleFile(mappedTs);
		
		
		configPt.setCandidateDistanceMultiplier(distanceMultiplier);
		configPt.setMaxLinkCandidateDistance(candidateDistance);
		configPt.setMaxTravelCostFactor(maxTravelCostFactor);
		configPt.setNLinkThreshold(nLink);
		configPt.setNumOfThreads(thread);
		Map<String,Set<String>> modes = new HashMap<>();
		modes.put("bus", new HashSet<>());
		modes.put("subway", new HashSet<>());
		modes.put("rail", new HashSet<>());
		
		modes.get("bus").add("bus");
		modes.get("bus").add("car");
		modes.get("bus").add("car_passenger");
		
		modes.get("subway").add("subway");
		modes.get("subway").add("light_rail");
		
		
		modes.get("rail").add("rail");
		modes.get("rail").add("light_rail");
		
		configPt.setTransportModeAssignment(modes);
		
		
		

		new ConfigWriter(config).write(ptMapperConfig);
		
		//CreateDefaultPTMapperConfig.main(new String[]{"data/kinan/ptMapperConfig.xml"});
		
		PublicTransitMapper.run(config, configPt);
		
		CheckMappedSchedulePlausibility.run(mappedTs, finalnet, "epsg:32188", "data/osm/plausibility");
		
		
		
		//new SignalsWriter(signals).write("data/osm/outputSignals.xml");
		//new SignalsScenarioWriter();
	}
	
	
	public static double checkForBadCapacity(Network net) {
		double badCap = 0;
		for(Link link:net.getLinks().values()) {
			if(link.getLength()==0||link.getCapacity()<=0) {
				badCap++;
			}
		}
		return badCap;
	}
	
	public static void addTransit(Network net, String osmFile) {
		String fileLoc = "data/osm/rail.osm";
		if(osmFile!=null)fileLoc = osmFile;
		OSMWayAndNodesReader reader = new OSMWayAndNodesReader();
		reader.read(fileLoc);
		System.out.println();
		
		
		NetworkFactory netFac = net.getFactory();
		
		
		//String[] types = new String[]{"rail","subway"};
		Set<String> types = reader.getWaysByType().keySet();
		
//		reader.getWaysByType().get(type).forEach(a->{
//			a.getNodeIds().forEach(n->{
//				if(reader.getGeneralNodes().get(n)!=null)nodes.add(reader.getGeneralNodes().get(n));
//				else nodes.add(reader.getTaggedNodes().get(n));
//					
//			});
//		});
		
//		nodes.forEach(n->{
//			net.addNode(netFac.createNode(Id.createNodeId(n.getId()), n.getCoordinate()));
//		});
		
		Map<Long,NodeOSM> osmNodes = new HashMap<>();
		osmNodes.putAll(reader.getGeneralNodes());
		osmNodes.putAll(reader.getTaggedNodes());
		
		for(String type:types) {
		reader.getWaysByType().get(type).forEach(a->{
			if(a.getAttributes().get("service")==null &&(a.getAttributes().get("usage")==null ||a.getAttributes().get("usage").equals("main")||a.getAttributes().get("usage").equals("branch"))) {
			NodeOSM fromNode = osmNodes.get(a.getNodeIds().get(0));
			if(!net.getNodes().containsKey(Id.createNodeId(fromNode.getId())))net.addNode(netFac.createNode(Id.createNodeId(fromNode.getId()),fromNode.getCoordinate()));
			for(int i = 1;i<a.getNodeIds().size();i++) {
				if(true || NetworkUtils.getEuclideanDistance(osmNodes.get(a.getNodeIds().get(i)).getCoordinate(),fromNode.getCoordinate())>250) {
					if(!net.getNodes().containsKey(Id.createNodeId(a.getNodeIds().get(i))))net.addNode(netFac.createNode(Id.createNodeId(a.getNodeIds().get(i)),osmNodes.get(a.getNodeIds().get(i)).getCoordinate()));
					Id<Link> lId = Id.createLinkId(Long.toString(fromNode.getId())+"_"+a.getNodeIds().get(i));
					if(!net.getLinks().containsKey(lId)) {
						net.addLink(netFac.createLink(lId,net.getNodes().get(Id.createNodeId(fromNode.getId())), net.getNodes().get(Id.createNodeId(a.getNodeIds().get(i)))));
						Link link = net.getLinks().get(lId);
						link.setCapacity(30000);
						link.setFreespeed(21);
						Set<String> modes = new HashSet<>();
						modes.add("pt");
						if(type.equals("subway"))modes.add("subway");
						else if(type.equals("rail")) {
							modes.add("light_rail");
							modes.add("rail");
							//modes.add("subway");
						}
						link.setAllowedModes(modes);
						link.setLength(NetworkUtils.getEuclideanDistance(osmNodes.get(a.getNodeIds().get(i)).getCoordinate(),fromNode.getCoordinate()));
					}
					fromNode = osmNodes.get(a.getNodeIds().get(i));
				}
			}
			}
		});
		}
	}
	
}
