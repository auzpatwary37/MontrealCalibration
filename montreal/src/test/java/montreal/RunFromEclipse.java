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
				"--network", "matsimRun/osmMultimodal.xml",
				"--ts", "matsimRun/osmTsMapped.xml", 
				"--tv", "matsimRun/osmVehicles.xml", 
				"--plan", "matsimRun/output_plans.xml.gz", 
				"--facilities", "matsimRun/output_facilities.xml.gz", 
				"--clearplan", "true",
				"--household","matsimRun/montreal_households.xml.gz",
				"--lanes","data/osm/outputLanes_out.xml"
			};
		
		String[] args3 = new String[] {
				"--iterations","250",
				"--thread", "10",
				"--scale", ".05", 
				"--config", "5_percent/newData/configMine.xml",
				"--network", "5_percent/newData/montreal_network.xml.gz",
				"--ts", "5_percent/newData/montreal_transitSchedule.xml.gz", 
				"--tv", "5_percent/newData/montreal_transitVehicles.xml.gz", 
				"--plan", "5_percent/newData/output_plans_.2LessCar.xml.gz", 
				"--facilities", "5_percent/newData/output_facilities.xml.gz", 
				"--clearplan", "false",
				"--household","5_percent/newData/montreal_households.xml.gz",
				"--lanes",""
			};
		Run.main(args2);
		
	}
}
