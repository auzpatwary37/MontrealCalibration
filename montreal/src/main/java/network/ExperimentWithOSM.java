package network;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.lanes.Lane;
import org.matsim.lanes.Lanes;
import org.matsim.lanes.LanesFactory;
import org.matsim.lanes.LanesToLinkAssignment;
import org.matsim.lanes.LanesUtils;
import org.matsim.lanes.LanesWriter;
import org.matsim.pt2matsim.config.OsmConverterConfigGroup;
import org.matsim.pt2matsim.osm.OsmMultimodalNetworkConverter;
import org.matsim.pt2matsim.osm.lib.AllowedTagsFilter;
import org.matsim.pt2matsim.osm.lib.Osm;
import org.matsim.pt2matsim.osm.lib.OsmData;
import org.matsim.pt2matsim.osm.lib.OsmDataImpl;
import org.matsim.pt2matsim.osm.lib.OsmFileReader;
import org.matsim.pt2matsim.tools.NetworkTools;

public class ExperimentWithOSM {
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
		String outputGeometry = "data/osm/detailedNet.csv";
		int thread = 10;
		int distanceMultiplier = 5;
		int candidateDistance = 50;
		int maxTravelCostFactor = 200;
		int nLink = 10;
		AllowedTagsFilter filter = new AllowedTagsFilter();
		filter.add(Osm.ElementType.WAY, Osm.Key.HIGHWAY, null);
		filter.add(Osm.ElementType.WAY, Osm.Key.RAILWAY, null);

		
		OsmConverterConfigGroup osmconfig = OsmConverterConfigGroup.createDefaultConfig();
		osmconfig = OsmConverterConfigGroup.loadConfig(mathildeOsmConverterConfig);
		osmconfig.setKeepTagsAsAttributes(true);
		osmconfig.setOutputCoordinateSystem("EPSG:32188");
		osmconfig.setOsmFile(osmNet);
		osmconfig.setOutputDetailedLinkGeometryFile(outputGeometry);
		OsmData osmData = new OsmDataImpl(filter);
		new OsmFileReader(osmData).readFile(osmconfig.getOsmFile());
		
		OsmMultimodalNetworkConverter converter = new OsmMultimodalNetworkConverter(osmData);
		converter.convert(osmconfig);
		
		Network net = converter.getNetwork();
		
		NetworkWithLanesTrial.addTransit(net, osmTransit);
		
		NetworkTools.writeNetwork(net, "data/osm/testNet.xml");
		Lanes lanes = LanesUtils.createLanesContainer();
		LanesFactory lFac = lanes.getFactory();
		int oddLink = 0;
		int allLink = 0;
		
		for(Entry<Id<Link>, ? extends Link> link:net.getLinks().entrySet()){
			String turn = null;
			if((turn = (String) link.getValue().getAttributes().getAttribute("turn"))!=null) {
				allLink++;
				if(turn.equals("let;through|"))turn = "|left|through|";
				while(turn.contains("||")) {
					turn = turn.replace("||", "|empty|");
				}
				LanesToLinkAssignment l2l = lFac.createLanesToLinkAssignment(link.getKey());
				double numLane = link.getValue().getNumberOfLanes();
				Map<Id<Link>,Double>order =  EmNetworkCreator.getOrderedAnglesforToLinks(link.getValue());
				List<Id<Link>> lefts = new ArrayList<>();
				List<Id<Link>> rights = new ArrayList<>();
				List<Id<Link>> straight = new ArrayList<>();
				
				for(Entry<Id<Link>, Double> d:order.entrySet()) {
					if(Double.compare(Math.abs(d.getValue()), Math.PI)==0) {
						continue;
					}else if(d.getValue()<-1*Math.PI/4) {
						lefts.add(d.getKey());
					}else if(d.getValue()>Math.PI/4) {
						rights.add(d.getKey());
					}else {
						straight.add(d.getKey());
					}
				}
				
				String[] turns = turn.split("\\|");
				boolean addL2l = true;
				for(int i = 0;i<turns.length;i++) {
					Lane lane = lFac.createLane(Id.create(link.getKey().toString()+"_"+i,Lane.class));
					lane.setCapacityVehiclesPerHour(1800);
					lane.setNumberOfRepresentedLanes(1);
					if(turns[i].equals("empty")||turns[i].equals("none")) {//the lane will be straight
						lefts.forEach(l->lane.addToLinkId(l));
						rights.forEach(r->lane.addToLinkId(r));
						straight.forEach(s->lane.addToLinkId(s));
						l2l.addLane(lane);
						continue;
					}
					String[] turnsind = turns[i].split(";");
					for(int j = 0;j<turnsind.length;j++) {
						if(turnsind[j].equals("left")||turnsind[j].equals("slight_left")||turnsind[j].equals("ft")) {
							lefts.forEach(l->lane.addToLinkId(l));
							if(lefts.isEmpty()&&straight.size()>0)lane.addToLinkId(straight.get(0));
						}else if(turnsind[j].equals("right")||turnsind[j].equals("slight_right")||turnsind[j].equals("slight:right")) {
							rights.forEach(r->lane.addToLinkId(r));
							if(rights.isEmpty()&& straight.size()>0)lane.addToLinkId(straight.get(straight.size()-1));
						}else if(turnsind[j].equals("through")||turnsind[j].equals("merge_to_left")||turnsind[j].equals("merge_to_right")||turnsind[j].equals("throgu")||turnsind[j].equals("merge_right")) {
							straight.forEach(s->lane.addToLinkId(s));
							if(straight.isEmpty()&& rights.size()>0 && rights.size()>=lefts.size())lane.addToLinkId(rights.get(0));
							else if(straight.isEmpty() &&lefts.size()>0 && rights.size()<=lefts.size())lane.addToLinkId(lefts.get(lefts.size()-1));
						}else if(turnsind[j].equals("none")||turnsind[j].equals("")||turnsind[j].equals("empty")) {
							lefts.forEach(l->lane.addToLinkId(l));
							rights.forEach(r->lane.addToLinkId(r));
							straight.forEach(s->lane.addToLinkId(s));
						}else {
							System.out.println("Unrecognized turn restriction!!!" + turnsind[j]);
						}
					}
					if(lane.getToLinkIds()==null) {
						System.out.println("Debug, turn "+turn+" but left size "+lefts.size()+" right size "+ rights.size() +" and through size "+ straight.size());
						addL2l = false;
						oddLink++;
						break;
					}else {
						l2l.addLane(lane);
					}
				}
				
				if(addL2l)lanes.addLanesToLinkAssignment(l2l);
			}
		}
		new LanesWriter(lanes).write("data/osm/testLanes.xml");
		System.out.println("Total odd links = "+ oddLink+" out of "+allLink);
		Set<String> modes =  new HashSet<>();
		for(Link link:net.getLinks().values()) {
			if(modes.contains("pt,bus")) {
				modes.remove("pt,bus");
				modes.add("pt");
				modes.add("bus");
			}
			modes.addAll(link.getAllowedModes());
		}
		System.out.println(modes);
	}
}
