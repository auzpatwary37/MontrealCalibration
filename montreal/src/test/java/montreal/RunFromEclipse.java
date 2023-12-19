package montreal;


import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.network.NetworkUtils;

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
				"--network", "data/osm/valid1252OSM/osmMultimodal.xml",
				"--ts", "data/osm/valid1252OSM/osmTsMapped.xml", 
				"--tv", "data/osm/valid1252OSM/osmVehicles.xml", 
				"--plan", "matsimRun/output_plans.xml.gz", 
				"--facilities", "matsimRun/output_facilities.xml.gz", 
				"--clearplan", "true",
				"--household","matsimRun/montreal_households.xml.gz",
				"--lanes","data/osm/valid1252OSM/testLanes_out.xml",
				"--paramfile","src/main/resources/paramReaderTrial1_newData REsult (1).csv",
				"--output","outputosm21"
			};
		
		String[] args3 = new String[] {
				"--iterations","250",
				"--thread", "10",
				"--scale", ".05", 
				"--config", "5_percent/newData/configMine.xml",
				"--network", "data/kinan/emMultimodal2041.xml",
				"--ts", "data/kinan/emTsMapped2041.xml", 
				"--tv", "data/kinan/emVehicles2041.xml", 
				"--plan", "matsimRun/output_plans.xml.gz", 
				"--facilities", "matsimRun/output_facilities.xml.gz", 
				"--clearplan", "true",
				"--household","matsimRun/montreal_households.xml.gz",
				"--lanes","data/kinan/emLanes2041_out.xml",
				"--paramfile","src/main/resources/paramReaderTrial1_newData REsult (1).csv",
				"--output","outputEm2041BasePop"
			};
		
		String[] args4 = new String[] {
				"--iterations","250",
				"--thread", "10",
				"--scale", ".05", 
				"--config", "5_percent/newData/configMine.xml",
				"--network", "data/kinan/emMultimodal.xml",
				"--ts", "data/kinan/emTsMapped.xml", 
				"--tv", "data/kinan/emVehicles.xml", 
				"--plan", "matsimRun/output_plans.xml.gz", 
				"--facilities", "matsimRun/output_facilities.xml.gz", 
				"--clearplan", "true",
				"--household","matsimRun/montreal_households.xml.gz",
				"--lanes","data/kinan/emLanes_out.xml",
				"--paramfile","src/main/resources/paramReaderTrial1_newData REsult (1).csv",
				"--output","outputEmBasePop"
			};
		
		String[] args5 = new String[] {
				"--iterations","250",
				"--thread", "10",
				"--scale", ".25", 
				"--config", "matsimRun/configMine.xml",
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
		String[] args6 = new String[] {
				"--iterations","250",
				"--thread", "10",
				"--scale", ".25", 
				"--config", "matsimRun/configMine.xml",
				"--network", "data\\osm\\valid1252OSM\\osmMultimodal.xml",
				"--ts", "data\\osm\\valid1252OSM\\osmTsMapped.xml", 
				"--tv", "data\\osm\\valid1252OSM\\osmVehicles.xml", 
				"--lanes","data\\osm\\valid1252OSM\\testLanes_out.xml",
				"--plan", "data\\outputODPopulation_21_0.25.xml.gz", 
				"--facilities", "data\\outputODFacilities21_0.25.xml.gz",
				"--household","data\\outputODHouseholds_21_0.25.xml.gz",
				"--clearplan", "true",
				"--output","output21.25",
				"--vehicles","data\\outputODVehicle_21_0.25.xml.gz"
			};
		Run.main(args6);
		
	}
}
