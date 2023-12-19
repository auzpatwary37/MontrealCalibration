package montreal;


import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.network.NetworkUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import run.Run;

public class RunFromEclipse {
	public static void main(String[] args) {
		Network net = NetworkUtils.readNetwork("matsimRun/osmMultimodal.xml");
		net.getLinks().values().forEach(l->{
			if(l.getAllowedModes().contains("car")) {
				Set<String> modes = new HashSet<>(l.getAllowedModes());
				modes.add("car_passenger");
				l.setAllowedModes(modes);
			}
		});
		
		new NetworkWriter(net).write("matsimRun/osmMultimodal.xml");
		String[] args2 = new String[] {
				"--thread", "10",
				"--scale", ".05", 
				"--config", "matsimRun/configMine.xml",
				"--network", "matsimRun/osmMultimodal.xml",
				"--ts", "matsimRun/osmTsMapped.xml", 
				"--tv", "matsimRun/osmVehicles.xml", 
				"--plan", "matsimRun/output_plans.xml.gz", 
				"--facilities", "matsimRun/output_facilities.xml.gz", 
				"--clearplan", "true",
				"--household","matsimRun/montreal_households.xml.gz"
			};
		
		String[] args3 = new String[] {
				"--iterations","250",
				"--thread", "10",
				"--scale", ".25", 
				"--config", "5_percent/newData/configMine.xml",
				"--network", "data\\osm\\valid1252OSM\\osmMultimodal2041.xml",
				"--ts", "data\\osm\\valid1252OSM\\osmTsMapped2041.xml", 
				"--tv", "data\\osm\\valid1252OSM\\osmVehicles2041.xml", 
				"--lanes","data\\osm\\valid1252OSM\\testLanes_out2041.xml",
				"--plan", "data\\outputODPopulation_41_0.25.xml.gz", 
				"--facilities", "data\\outputODFacilities41_0.25.xml.gz",
				"--household","data\\outputODHouseholds_41_0.25.xml.gz",
				"--clearplan", "true",
				"--output","output41.25",
				"--vehicles","data\\outputODVehicle_41_0.25.xml.gz"
			};
		Run.main(args3);
		
	}
}
