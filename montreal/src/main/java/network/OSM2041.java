package network;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.lanes.Lane;
import org.matsim.lanes.Lanes;
import org.matsim.lanes.LanesFactory;
import org.matsim.lanes.LanesReader;
import org.matsim.lanes.LanesToLinkAssignment;
import org.matsim.lanes.LanesWriter;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.pt.utils.TransitScheduleValidator;
import org.matsim.pt.utils.TransitScheduleValidator.ValidationResult;
import org.matsim.pt2matsim.run.Gtfs2TransitSchedule;
import org.matsim.vehicles.MatsimVehicleReader;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

public class OSM2041 {
	public static void main(String[] args) {
		Network osmNet = NetworkUtils.readNetwork("data/osm/valid1252OSM/osmMultimodal.xml");
		Network changes = NetworkUtils.readNetwork("data/osm/valid1252OSM/emNetChange.xml");
		String net2041Connections = "data/osm/valid1252OSM/changesToOSM2041.csv";
		Scenario scnLane = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new LanesReader(scnLane).readFile("data/osm/valid1252OSM/testLanes_out.xml");
		new TransitScheduleReader(scnLane).readFile("data/osm/valid1252OSM/osmTsMapped.xml");
		TransitSchedule totalTs = scnLane.getTransitSchedule();
		Vehicles tv = VehicleUtils.createVehiclesContainer();
		new MatsimVehicleReader(tv).readFile("data/osm/valid1252OSM/osmVehicles.xml");
		Lanes lanes = scnLane.getLanes();
		NetworkFactory netFac = osmNet.getFactory();
		
		changes.getNodes().values().forEach(n->{
			Node nn = netFac.createNode(n.getId(), n.getCoord());
			osmNet.addNode(nn);
		});
		
		changes.getLinks().entrySet().forEach(l->{
			Node fromNode = osmNet.getNodes().get(l.getValue().getFromNode().getId());
			Node toNode = osmNet.getNodes().get(l.getValue().getToNode().getId());
			Link ll = netFac.createLink(l.getKey(), fromNode, toNode);
			ll.setAllowedModes(l.getValue().getAllowedModes());
			ll.setCapacity(l.getValue().getCapacity());
			ll.setFreespeed(l.getValue().getFreespeed());
			ll.setLength(l.getValue().getLength());
			ll.setNumberOfLanes(l.getValue().getNumberOfLanes());
			l.getValue().getAttributes().getAsMap().entrySet().forEach(a->{
				ll.getAttributes().putAttribute(a.getKey(), a.getValue());
			});
			osmNet.addLink(ll);
		});
		Set<String> modes = new HashSet<>();
		modes.add("car");
		modes.add("car_passenger");
		modes.add("bus");
		modes.add("pt");
		
		try {
			BufferedReader bf = new BufferedReader(new FileReader(new File(net2041Connections)));
			bf.readLine();
			String line = "";
			while((line = bf.readLine())!=null) {
				String[] parts = line.split(",");
				
				if(parts.length==3) {
					osmNet.removeLink(Id.createLinkId(parts[2]));
				}
				Link l = netFac.createLink(Id.createLinkId(parts[0]+"_"+parts[1]), osmNet.getNodes().get(Id.createNodeId(parts[0])), osmNet.getNodes().get(Id.createNodeId(parts[1])));
				l.setAllowedModes(modes);
				l.setLength(NetworkUtils.getEuclideanDistance(l.getFromNode().getCoord(), l.getToNode().getCoord()));
				l.setCapacity(1800*2);
				l.setNumberOfLanes(2);
				l.setFreespeed(60*1000/3600);
				osmNet.addLink(l);
				fixLinkToLinkConnectionsUpstream(l, lanes);
			}
			
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//Fix the lane connections
		
		addMaximeTransit(osmNet, totalTs, tv);
		
		new LanesWriter(lanes).write("data/osm/valid1252OSM/testLanes_out2041.xml");
		new NetworkWriter(osmNet).write("data/osm/valid1252OSM/osmMultimodal2041.xml");
		new TransitScheduleWriter(totalTs).writeFile("data/osm/valid1252OSM/osmTsMapped2041.xml");
		new MatsimVehicleWriter(tv).writeFile("data/osm/valid1252OSM/osmVehicles2041.xml");
		
	}
	
	private static double handleTime(String time) {
		double t = Double.parseDouble(time);
		double out = (int)t/100*3600+(t%100)*60;
		return out;
	}
	public static double distancePointToLineSegment(Coord point, Link line) {
		double x1 = line.getFromNode().getCoord().getX();
		double y1 = line.getFromNode().getCoord().getY();
		double x2 = line.getToNode().getCoord().getX();
		double y2 = line.getToNode().getCoord().getY();
		double x0 = point.getX();
		double y0 = point.getY();

		double dotProduct = ((x0 - x1) * (x2 - x1)) + ((y0 - y1) * (y2 - y1));
		if (dotProduct <= 0) {
			return NetworkUtils.getEuclideanDistance(point,line.getFromNode().getCoord());
		}

		double squaredLengthBA = Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2);
		if (dotProduct >= squaredLengthBA) {
			return NetworkUtils.getEuclideanDistance(point,line.getToNode().getCoord());
		}

		double crossProduct = ((x0 - x1) * (y2 - y1)) - ((y0 - y1) * (x2 - x1));
		return Math.abs(crossProduct) / Math.sqrt(squaredLengthBA);
	}

	public static Link findNearestLineSegment(Coord point, Set<Link> lines) {
		double minDistance = Double.MAX_VALUE;
		Link nearestLine = null;

		for (Link line : lines) {
			double distance = distancePointToLineSegment(point, line);
			if (distance < minDistance) {
				minDistance = distance;
				nearestLine = line;
			}
		}

		return nearestLine;
	}

	public static void addMaximeTransit(Network originalNet, TransitSchedule totalTs, Vehicles tv) {
		Gtfs2TransitSchedule.run("data/maxime/newData23/gtfs_transit_projects_2041", "dayWithMostTrips", "epsg:32188", "data/maxime/additionalTs23.xml", "data/maxime/additionalVehicles23.xml");
		String lines = "data/maxime/newLines.csv";
		String line = null;
		BufferedReader bf;
		Map<String,List<Link>>linkSeq = new HashMap<>();
		Map<String,List<Link>>linkSeqR = new HashMap<>();
		Map<String,List<Double>> headWay = new HashMap<>();
		Map<String,List<Tuple<Double,Double>>> timeBean = new HashMap<>();
		Double minLength = 5.;
		Network net = NetworkUtils.createNetwork();// A network can be read as well.
		NetworkFactory netFac = net.getFactory();
		try {
			bf = new BufferedReader(new FileReader(new File(lines)));

			bf.readLine();
			while((line = bf.readLine())!=null) {
				String[] parts = line.split(";");
				String lId = parts[0];

				Double length = Double.parseDouble(parts[3])*1000;
				Double speed = Double.parseDouble(parts[4])*1000/3600;
				String direction = parts[5];
				String directionR = "R";
				linkSeq.put(lId+"_"+direction, new ArrayList<>());
				linkSeqR.put(lId+"_"+directionR, new ArrayList<>());
				headWay.put(lId+"_"+direction, new ArrayList<>());
				timeBean.put(lId+"_"+direction, new ArrayList<>());
				String description = parts[6];
				for(int i = 7;i<12;i++) {
					double headway = Double.parseDouble(parts[i])*60;
					Tuple<Double,Double> t = new Tuple<>(handleTime(parts[i+5]),handleTime(parts[i+6]));
					headWay.get(lId+"_"+direction).add(headway);
					timeBean.get(lId+"_"+direction).add(t);
				}
				int type = Integer.parseInt(parts[18]);
				if(type!=1)type = 2;
				String lineString = parts[23];
				lineString = lineString.replace("LINESTRING","");
				lineString = lineString.replace("(","");
				lineString = lineString.replace(")","");
				String[] nodes = lineString.split(",");
				int i = 0;
				Node oldNode = null;
				List<Link> linkList = linkSeq.get(lId+"_"+direction);
				List<Link> linkListR = linkSeqR.get(lId+"_"+directionR);
				for(String node:nodes) {

					node = node.trim();
					Double x = Double.parseDouble(node.split(" ")[0]);
					Double y = Double.parseDouble(node.split(" ")[1]);

					double dist = Double.MAX_VALUE;

					if(i>0)dist = NetworkUtils.getEuclideanDistance(oldNode.getCoord(), new Coord(x,y));

					if(dist>minLength) {
						Node n = netFac.createNode(Id.createNodeId(x+"_"+y), new Coord(x,y));
						if(!net.getNodes().containsKey(n.getId()))net.addNode(n);
						if(i>0) {
							Id<Link> linkId = Id.createLinkId(oldNode.getId().toString()+"_"+n.getId().toString()+"_"+type);
							Link l = net.getLinks().get(linkId);

							if(l==null) {
								l = netFac.createLink(linkId,oldNode,n);
								l.setFreespeed(speed);
								l.setCapacity(3000);
								Set<String> modes = new HashSet<>();
								if(type==1) {
									modes.add("subway");
									modes.add("train");
									modes.add("pt");
								}else {
									modes.add("bus");
									modes.add("pt");
								}
								l.setAllowedModes(modes);
								l.setLength(length);
								l.setNumberOfLanes(1);

								net.addLink(l);


							}
							linkList.add(net.getLinks().get(l.getId()));


							Id<Link> linkIdR = Id.createLinkId(n.getId().toString()+"_"+oldNode.getId().toString()+"_"+type);
							Link lR = net.getLinks().get(linkIdR);


							if(lR==null) {
								lR = netFac.createLink(linkIdR,oldNode,n);
								lR.setFreespeed(speed);
								lR.setCapacity(3000);
								Set<String> modes = new HashSet<>();
								if(type==1) {
									modes.add("subway");
									modes.add("train");
									modes.add("pt");
								}else {
									modes.add("bus");
									modes.add("pt");
								}
								lR.setAllowedModes(modes);
								lR.setLength(length);
								lR.setNumberOfLanes(1);

								net.addLink(lR);


							}
							linkListR.add(net.getLinks().get(lR.getId()));
						}

						oldNode = n;
						i++;
					}
				}

			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		new NetworkWriter(net).write("data/maxime/outNet23.xml");	
		Scenario scn = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new TransitScheduleReader(scn).readFile("data/maxime/additionalTs23.xml");
		TransitSchedule rawTs = scn.getTransitSchedule();
		Vehicles rawTv = VehicleUtils.createVehiclesContainer();
		new MatsimVehicleReader(rawTv).readFile("data/maxime/additionalVehicles23.xml");
		TransitSchedule ts = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getTransitSchedule();
		TransitScheduleFactory tf = ts.getFactory();
		//_________hardcode to fix the error in the given data from MAXIME

		//Following are the problematic stops 57, 557, 54101 and 69 
		//57, 557 and 69 is due to north and south bound lines sharing the same stop. 54101 is left and right 
		// solution is to make 57_n, 57_s,57_t 557_n, 557_s, 69_n, and 69_s 

		//57_n -> 5 57_s -> 2001, 2002, 2003
		//557_n -> 10005 557_s -> 12001, 12002, 12003 
		//69_n -> 5 69_s -> 439, 539, 639
		//54101_l -> 639 54101_r -> 439, 539

		// 57 

		TransitStopFacility stop57 = rawTs.getFacilities().get(Id.create("57", TransitStopFacility.class));
		TransitStopFacility stop557 = rawTs.getFacilities().get(Id.create("557", TransitStopFacility.class));
		TransitStopFacility stop69 = rawTs.getFacilities().get(Id.create("69", TransitStopFacility.class));
		TransitStopFacility stop54101 = rawTs.getFacilities().get(Id.create("54101", TransitStopFacility.class));

		TransitStopFacility stop57_n = tf.createTransitStopFacility(Id.create(stop57.getId().toString()+"_n", TransitStopFacility.class), stop57.getCoord(), false);
		TransitStopFacility stop57_s = tf.createTransitStopFacility(Id.create(stop57.getId().toString()+"_s", TransitStopFacility.class), stop57.getCoord(), false);
		rawTs.addStopFacility(stop57_s);
		rawTs.addStopFacility(stop57_n);
		//rawTs.removeStopFacility(stop57);

		TransitStopFacility stop557_n = tf.createTransitStopFacility(Id.create(stop557.getId().toString()+"_n", TransitStopFacility.class), stop557.getCoord(), false);
		TransitStopFacility stop557_s = tf.createTransitStopFacility(Id.create(stop557.getId().toString()+"_s", TransitStopFacility.class), stop557.getCoord(), false);
		rawTs.addStopFacility(stop557_s);
		rawTs.addStopFacility(stop557_n);
		//rawTs.removeStopFacility(stop557);
		
		

		TransitStopFacility stop69_n = tf.createTransitStopFacility(Id.create(stop69.getId().toString()+"_n", TransitStopFacility.class), stop69.getCoord(), false);
		TransitStopFacility stop69_s = tf.createTransitStopFacility(Id.create(stop69.getId().toString()+"_s", TransitStopFacility.class), stop69.getCoord(), false);
		rawTs.addStopFacility(stop69_s);
		rawTs.addStopFacility(stop69_n);
		//rawTs.removeStopFacility(stop69);

		TransitStopFacility stop54101_l = tf.createTransitStopFacility(Id.create(stop54101.getId().toString()+"_n", TransitStopFacility.class), stop54101.getCoord(), false);
		TransitStopFacility stop54101_r = tf.createTransitStopFacility(Id.create(stop54101.getId().toString()+"_s", TransitStopFacility.class), stop54101.getCoord(), false);
		rawTs.addStopFacility(stop54101_l);
		rawTs.addStopFacility(stop54101_r);
		//rawTs.removeStopFacility(stop54101);

		for(TransitLine tl:rawTs.getTransitLines().values()) {
			for(TransitRoute tr:new HashSet<>(tl.getRoutes().values())) {
				List<TransitRouteStop> newStops = new ArrayList<>(tr.getStops());
				int i = 0;
				for(TransitRouteStop trs:new ArrayList<>(tr.getStops())) {
					if(trs.getStopFacility().getId().equals(stop57.getId())) {
						if(tl.getId().toString().equals("5")) {
							TransitRouteStop replacementStop = tf.createTransitRouteStop(stop57_n, trs.getArrivalOffset(), trs.getDepartureOffset());
							newStops.set(i, replacementStop);
						}else {
							TransitRouteStop replacementStop = tf.createTransitRouteStop(stop57_s, trs.getArrivalOffset(), trs.getDepartureOffset());
							newStops.set(i, replacementStop);
						}
					}

					if(trs.getStopFacility().getId().equals(stop557.getId())) {
						if(tl.getId().toString().equals("10005")) {
							TransitRouteStop replacementStop = tf.createTransitRouteStop(stop557_n, trs.getArrivalOffset(), trs.getDepartureOffset());
							newStops.set(i, replacementStop);
						}else {
							TransitRouteStop replacementStop = tf.createTransitRouteStop(stop557_s, trs.getArrivalOffset(), trs.getDepartureOffset());
							newStops.set(i, replacementStop);
						}
					}

					if(trs.getStopFacility().getId().equals(stop69.getId())) {
						if(tl.getId().toString().equals("5")) {
							TransitRouteStop replacementStop = tf.createTransitRouteStop(stop69_n, trs.getArrivalOffset(), trs.getDepartureOffset());
							newStops.set(i, replacementStop);
						}else {
							TransitRouteStop replacementStop = tf.createTransitRouteStop(stop69_s, trs.getArrivalOffset(), trs.getDepartureOffset());
							newStops.set(i, replacementStop);
						}
					}
					if(trs.getStopFacility().getId().equals(stop54101.getId())) {
						if(tl.getId().toString().equals("639")) {
							TransitRouteStop replacementStop = tf.createTransitRouteStop(stop54101_l, trs.getArrivalOffset(), trs.getDepartureOffset());
							newStops.set(i, replacementStop);
						}else {
							TransitRouteStop replacementStop = tf.createTransitRouteStop(stop54101_r, trs.getArrivalOffset(), trs.getDepartureOffset());
							newStops.set(i, replacementStop);
						}
						
					}
					i++;
				}
				TransitRoute trNew = tf.createTransitRoute(tr.getId(), null, newStops, tr.getTransportMode());
				tr.getDepartures().entrySet().forEach(d->{
					Departure dd = tf.createDeparture(d.getKey(), d.getValue().getDepartureTime());
					dd.setVehicleId(d.getValue().getVehicleId());
					trNew.addDeparture(dd);
				});
				tl.removeRoute(tr);
				tl.addRoute(trNew);
			}
		}


		//__________________________________





		Map<Id<TransitStopFacility>,Set<Link>> stopToLinkMapping = new HashMap<>();
		for(TransitLine tl:rawTs.getTransitLines().values()) {
			for(TransitRoute tr:new HashSet<>(tl.getRoutes().values())) {
				String direction = tr.getId().toString().contains("_A")?"A":"R";
				String key = tl.getId().toString()+"_"+direction;
				List<Link>links = direction.equals("A")?linkSeq.get(key):linkSeqR.get(key);
				String mode = tr.getTransportMode();
				
				if(mode.equals("subway")) {
					int i = 0;
					List<TransitRouteStop>newStops = new ArrayList<>(tr.getStops());
					for(TransitRouteStop trs:new ArrayList<>(tr.getStops())){
						TransitStopFacility tsf = tf.createTransitStopFacility(Id.create(trs.getStopFacility().getId().toString()+"_t", TransitStopFacility.class), trs.getStopFacility().getCoord(), false);
						rawTs.addStopFacility(tsf);
						TransitRouteStop trs_replace = tf.createTransitRouteStop(tsf, trs.getArrivalOffset(),trs.getDepartureOffset());
						newStops.set(i, trs_replace);
						i++;
					}
					TransitRoute trNew = tf.createTransitRoute(tr.getId(), null, newStops, tr.getTransportMode());
					tr.getDepartures().entrySet().forEach(d->{
						Departure dd = tf.createDeparture(d.getKey(), d.getValue().getDepartureTime());
						dd.setVehicleId(d.getValue().getVehicleId());
						trNew.addDeparture(dd);
					});
					tl.removeRoute(tr);
					tl.addRoute(trNew);
					tr = trNew;
				}
				
				

				for(TransitRouteStop stop:tr.getStops()) {
					if(!stopToLinkMapping.containsKey(stop.getStopFacility().getId()))stopToLinkMapping.put(stop.getStopFacility().getId(),new HashSet<>());
					stopToLinkMapping.get(stop.getStopFacility().getId()).addAll(links);
				}
			}
		}

		for(TransitStopFacility tsf:new HashSet<>(rawTs.getFacilities().values())) {
			if(stopToLinkMapping.get(tsf.getId())==null)rawTs.removeStopFacility(tsf);
		}

		rawTs.getFacilities().entrySet().forEach(e->{
			//			Network netn = NetworkUtils.createNetwork();
			//			
			//			NetworkUtils.getNearestRightEntryLink(net,e.getValue().getCoord());
			e.getValue().setLinkId(findNearestLineSegment(e.getValue().getCoord(), stopToLinkMapping.get(e.getKey())).getId());
			ts.addStopFacility(e.getValue());
		});
		
		//new TransitScheduleWriter(rawTs).writeFile("data/maxime/tsTrial.xml");

		rawTs.getTransitLines().entrySet().forEach(e->{
			TransitLine tll = tf.createTransitLine(e.getKey());
			if(e.getKey().toString().equals("5"))tll = tf.createTransitLine(Id.create("c-5", TransitLine.class));
			if(e.getKey().toString().equals("539"))tll = tf.createTransitLine(Id.create("c-539", TransitLine.class));
			ts.addTransitLine(tll);
			TransitLine tl = tll;
			e.getValue().getRoutes().entrySet().forEach(ee->{
				String direction = "A";
				if(ee.getKey().toString().contains("_R"))direction = "R";
				String key = e.getKey().toString()+"_"+direction;
				List<Link> linkList = linkSeq.get(key);
				Id<Link> startLink = linkList.get(0).getId();
				Id<Link> endLink = linkList.get(linkList.size()-1).getId();
				List<Id<Link>> midLinks = new ArrayList<>();
				for(int i = 1; i<linkList.size()-1;i++)midLinks.add(linkList.get(i).getId());
				NetworkRoute r = RouteUtils.createLinkNetworkRouteImpl(startLink, midLinks, endLink);
				List<TransitRouteStop> stops = new ArrayList<>();
				for(TransitRouteStop trs:ee.getValue().getStops()) {
					Id<TransitStopFacility> stopId = Id.create(trs.getStopFacility().getId().toString(), TransitStopFacility.class);
					TransitRouteStop trs_new = tf.createTransitRouteStop(ts.getFacilities().get(stopId), trs.getArrivalOffset(),trs.getDepartureOffset());
					trs_new.setAwaitDepartureTime(true);
					stops.add(trs_new);
				}
				TransitRoute tr = tf.createTransitRoute(ee.getKey(), r, stops, ee.getValue().getTransportMode());
				tl.addRoute(tr);
				ee.getValue().getDepartures().entrySet().forEach(d->{
					Id<Vehicle> vId = d.getValue().getVehicleId();
					if(tv.getVehicles().containsKey(d.getValue().getVehicleId())) {
						Vehicle v = rawTv.getVehicles().get(vId);
						rawTv.removeVehicle(vId);
						vId = Id.createVehicleId(vId.toString()+"_new");
						rawTv.addVehicle(VehicleUtils.createVehicle(vId, v.getType()));
					}
					Departure dep = tf.createDeparture(d.getKey(),d.getValue().getDepartureTime());
					dep.setVehicleId(vId);
					tr.addDeparture(dep);
				});
			});
		});
	
		ValidationResult r = TransitScheduleValidator.validateAll(ts,net);
		System.out.println("transit is valid? "+ r.isValid());
		
		
		scn = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		TransitLine old5 = totalTs.getTransitLines().get(Id.create("c-5",TransitLine.class));
		totalTs.removeTransitLine(old5);
		
		TransitLine old439 = totalTs.getTransitLines().get(Id.create("439",TransitLine.class));
		totalTs.removeTransitLine(old439);
		Set<Id<Link>> linksToRemove = new HashSet<>();
		old5.getRoutes().values().forEach(tr->{
			linksToRemove.add(tr.getRoute().getStartLinkId());
			linksToRemove.add(tr.getRoute().getEndLinkId());
			linksToRemove.addAll(tr.getRoute().getLinkIds());
		});
		linksToRemove.forEach(l->originalNet.removeLink(l));
		NetworkFactory nf = originalNet.getFactory();
		net.getNodes().values().forEach(n->{
			Node nn = nf.createNode(n.getId(), n.getCoord());
			originalNet.addNode(nn);
		});
		net.getLinks().values().forEach(l->{
			Link ll = nf.createLink(l.getId(), l.getFromNode(), l.getToNode());
			ll.setAllowedModes(l.getAllowedModes());
			ll.setCapacity(l.getCapacity());
			ll.setFreespeed(l.getFreespeed());
			ll.setLength(l.getLength());
			ll.setNumberOfLanes(l.getNumberOfLanes());
			l.getAttributes().getAsMap().entrySet().forEach(a->{
				ll.getAttributes().putAttribute(a.getKey(), a.getValue());
			});
			originalNet.addLink(ll);
		});
		
		new HashSet<>(originalNet.getNodes().values()).forEach(n->{
			if(n.getInLinks().isEmpty() && n.getOutLinks().isEmpty()) {
				originalNet.removeNode(n.getId());
			}
		});
		
		ts.getFacilities().values().forEach(tsf->{
			totalTs.addStopFacility(tsf);
		});
		
		ts.getTransitLines().values().forEach(tl->{
			totalTs.addTransitLine(tl);
		});
		
		
		r = TransitScheduleValidator.validateAll(totalTs,originalNet);
		System.out.println("transit is valid? "+ r.isValid());
		for(VehicleType vt:rawTv.getVehicleTypes().values()){
			if(tv.getVehicleTypes().get(vt.getId())==null)tv.addVehicleType(vt);
		}
		//new TransitScheduleWriter(ts).writeFile("data/maxime/mappedTs23.xml");
//		try {
//			FileWriter fw = new FileWriter(new File("data/maxime/mappedStops23.csv"));
//			fw.append("stopId,x,y,linkId\n");
//			for(TransitStopFacility tsf:ts.getFacilities().values()) {
//				fw.append(tsf.getId().toString()+","+tsf.getCoord().getX()+","+tsf.getCoord().getY()+","+tsf.getLinkId().toString()+"\n");
//				fw.flush();
//			}
//			fw.close();
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
	}
	
	public static void fixLinkToLinkConnectionsUpstream(Link link, Lanes lanes) {
		Node fromNode = link.getFromNode();
		boolean haveLaneUpstream = false;
		for(Link inLink:fromNode.getInLinks().values()) {
			if(lanes.getLanesToLinkAssignments().containsKey(inLink.getId())) {
				haveLaneUpstream = true;
				break;
			}
		}
		if(haveLaneUpstream) {
			LanesFactory lf = lanes.getFactory();
			for(Link inLink:fromNode.getInLinks().values()) {
				LanesToLinkAssignment l2l = lanes.getLanesToLinkAssignments().get(inLink.getId());
				if(l2l == null) {
					l2l = lf.createLanesToLinkAssignment(inLink.getId());
					lanes.addLanesToLinkAssignment(l2l);
				}
				Lane lane = lf.createLane(Id.create(inLink.getId()+"___"+link.getId(),Lane.class));
				lane.addToLinkId(link.getId());
				lane.setCapacityVehiclesPerHour(1800);
				lane.setNumberOfRepresentedLanes(1);
				lane.setStartsAtMeterFromLinkEnd(50);
				l2l.addLane(lane);
			}
		}
	}

}
