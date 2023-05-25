package network;

import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsScenarioWriter;
import org.matsim.contrib.signals.network.SignalsAndLanesOsmNetworkReader;
import org.matsim.contrib.signals.utils.SignalUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkReaderTeleatlas;
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
		
		
		Network network = NetworkUtils.createNetwork();
		CoordinateTransformation coordTransfer = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:32188");
		SignalSystemsConfigGroup signalConfig = new SignalSystemsConfigGroup();
		SignalsData signals = SignalUtils.createSignalsData(signalConfig);
		Lanes lanes = LanesUtils.createLanesContainer();
		SignalsAndLanesOsmNetworkReader netReader = new SignalsAndLanesOsmNetworkReader(network, coordTransfer, true, signals, lanes);
		
		netReader.setBoundingBox(45.0, -74.7, 46.15, -72.8);
		
		netReader.setAcceptFourPlusCrossings(true);
		netReader.setAllowUTurnAtLeftLaneOnly(true);
		netReader.setKeepPaths(false);
		netReader.setMakePedestrianSignals(true);
		netReader.parse("data/osm/Region Montrealaise.osm");
		int wrongLane = 0;
		int totalLane = 0;
		for(Entry<Id<Link>, LanesToLinkAssignment> l2l:lanes.getLanesToLinkAssignments().entrySet()){
			for(Entry<Id<Lane>, Lane> lane:new HashMap<>(l2l.getValue().getLanes()).entrySet()){
				totalLane++;
				if((lane.getValue().getToLinkIds()==null || lane.getValue().getToLinkIds().isEmpty()) && (lane.getValue().getToLaneIds()==null || lane.getValue().getToLaneIds().isEmpty())) {
					wrongLane++;
					l2l.getValue().getLanes().remove(lane.getKey());		
					
				}
			};
		};
		new NetworkWriter(network).write("data/osm/outputNet.xml");
		new LanesWriter(lanes).write("data/osm/outputLanes.xml");
		System.out.println("Wrong Lanes = "+wrongLane+" out of "+totalLane);
		
		Gtfs2TransitSchedule.run("data/kinan/gtfsData/out/", "dayWithMostTrips", "epsg:32188", "data/osm/osmTs.xml", "data/osm/osmVehicles.xml");
		
		Config config = ConfigUtils.createConfig();
		//CreateDefaultPTMapperConfig.main(new String[]{"data/kinan/ptMapperConfig.xml"});
		//ConfigUtils.loadConfig(config,"data/kinan/ptMapperConfig.xml");
		
		ConfigGroup c = PublicTransitMappingConfigGroup.createDefaultConfig();
		config.addModule(c);
		
		PublicTransitMappingConfigGroup configPt = ConfigUtils.addOrGetModule(config, PublicTransitMappingConfigGroup.GROUP_NAME, PublicTransitMappingConfigGroup.class); 

		Set<String> toRemove = config.getModules().keySet().stream().filter(module -> !module.equals(PublicTransitMappingConfigGroup.GROUP_NAME)).collect(Collectors.toSet());
		toRemove.forEach(config::removeModule);
		
		configPt.setInputNetworkFile("data/osm/outputNet.xml");
		config.network().setLaneDefinitionsFile("data/osm/outputLanes.xml");
		configPt.setInputScheduleFile("data/osm/osmTs.xml");
		configPt.setOutputNetworkFile("data/osm/osmMultimodal.xml");
		configPt.setOutputScheduleFile("data/osm/osmTsMapped.xml");
		
		
		configPt.setCandidateDistanceMultiplier(5);
		configPt.setMaxLinkCandidateDistance(1000);
		configPt.setMaxTravelCostFactor(200);
		configPt.setNLinkThreshold(10);
		

		new ConfigWriter(config).write("data/osm/ptMapperConfig.xml");
		
		//CreateDefaultPTMapperConfig.main(new String[]{"data/kinan/ptMapperConfig.xml"});
		
		PublicTransitMapper.run("data/osm/ptMapperConfig.xml");
		Network outNetFinal = NetworkUtils.readNetwork("data/osm/osmMultimodal.xml");
		CheckMappedSchedulePlausibility.run("data/osm/osmTsMapped.xml", "data/osm/osmMultimodal.xml", "epsg:32188", "data/osm/plausibility");
		
		
		
		//new SignalsWriter(signals).write("data/osm/outputSignals.xml");
		//new SignalsScenarioWriter();
	}
}
