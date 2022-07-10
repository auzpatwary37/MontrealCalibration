package countGeneration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

import ust.hk.praisehk.metamodelcalibration.measurements.Measurement;
import ust.hk.praisehk.metamodelcalibration.measurements.Measurements;
import ust.hk.praisehk.metamodelcalibration.measurements.MeasurementsReader;
import ust.hk.praisehk.metamodelcalibration.measurements.MeasurementsWriter;

public class CountConsistancyChecker {
public static void main(String[] args) {
	Measurements m = new MeasurementsReader().readMeasurements("src\\main\\resources\\montrealMeasurements_2020_2022.xml");
	Network network = NetworkUtils.readNetwork("5_percent\\newData\\montreal_network.xml.gz");
	Network networkOld = NetworkUtils.readNetwork("5_percent\\montreal_network.xml.gz");
	Set<Id<Link>> badLink = new HashSet<>();
	m.getLinksToCount().forEach(l->{
	if(!network.getLinks().containsKey(l)) {
			badLink.add(l);
		};
	});
	Set<Id<Link>> linksets = m.getLinksToCount();
	System.out.println("total bad link = "+badLink.size() + " out of "+m.getLinksToCount().size());
	try {
		FileWriter fw = new FileWriter(new File("5_percent\\newData\\badLinks.csv"));
		for (Id<Link> id : badLink) {
			fw.append(id.toString()+"\n");
			fw.flush();
		}
		fw.close();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	Map<Id<Link>,Id<Link>> oldToNew = new HashMap<>();
	linksets.forEach(l->{
		Link linkOld = networkOld.getLinks().get(l);
		Node fromNode = NetworkUtils.getNearestNode(network, linkOld.getFromNode().getCoord());
		Id<Link> linkIdNew = null;
		double dist = Double.MAX_VALUE;
		for(Entry<Id<Link>, ? extends Link> ll:fromNode.getOutLinks().entrySet()){
			double d = NetworkUtils.getEuclideanDistance(ll.getValue().getToNode().getCoord(),linkOld.getToNode().getCoord());
			if(d<dist) {
				dist = d;
				linkIdNew = ll.getKey();
				
			}
		}
		oldToNew.put(l, linkIdNew);
	});
	
	try {
		FileWriter fw = new FileWriter(new File("5_percent\\newData\\newLinkMatchesNew.csv"));
		fw.append("MeasurementsId,stationId,Bound,InOrOut, oldMatch, newMatch\n");
		for(Measurement mm:m.getMeasurements().values()) {
			List<Id<Link>>linkIds = (List<Id<Link>>) mm.getAttribute(Measurement.linkListAttributeName);
			String[] part = mm.getId().toString().split("___");
			//Coord coord = mm.getCoord();
			fw.append(mm.getId().toString()+","+part[0]+","+part[1]+","+part[2]+","+linkIds.get(0).toString()+","+oldToNew.get(linkIds.get(0)).toString()+"\n");
			fw.flush();
			linkIds.add(oldToNew.get(linkIds.get(0)));
			linkIds.remove(0);
			
		}
		fw.close();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	new MeasurementsWriter(m).write("5_percent\\newData\\newMeasurements.xml");
}
}
