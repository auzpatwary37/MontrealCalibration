package calibration;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;

public class Analysis {
	public static void main(String[] args) {
		Network net = NetworkUtils.readNetwork("5_percent\\montreal_network.xml.gz");
		int lowCapLink = 0;
		for (Link link : net.getLinks().values()) {
			if(link.getFreespeed()<4) {
				lowCapLink++;
			}
		}
		System.out.println("Number of low capacity links = "+lowCapLink + " out of total links = "+ net.getLinks().size());
		int a = 0;
		
	}
	
}
