package countGeneration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.collections.Tuple;

import ust.hk.praisehk.metamodelcalibration.measurements.Measurement;
import ust.hk.praisehk.metamodelcalibration.measurements.MeasurementType;
import ust.hk.praisehk.metamodelcalibration.measurements.Measurements;
import ust.hk.praisehk.metamodelcalibration.measurements.MeasurementsWriter;

public class StationGeneration {
public static void main(String[] args) {
	
	Map<String,Tuple<Double,Double>> timeBean = new HashMap<>();
	for(int i = 0;i<24;i++) {
		timeBean.put(Integer.toString(i), new Tuple<>((i-1)*3600.,i*3600.));
	}
	Network net = NetworkUtils.readNetwork("src\\main\\resources\\montreal_network.xml.gz");
//	timeBean.put("AADT", new Tuple<Double,Double>(0.,24*3600.));
	
	Measurements m = Measurements.createMeasurements(timeBean);
	Set<String> timeBeanUnique = new HashSet<>();
	
	String dataFileName = "src\\main\\resources\\Data_2020_2022.csv";
	Map<String,List<DirectionalData>> dataSets = new HashMap<>();
	try {
		BufferedReader bf = new BufferedReader(new FileReader(new File(dataFileName)));
		bf.readLine();
	//get rid of the header
	String line = null;
	while((line = bf.readLine())!=null) {
		DirectionalData data = new DirectionalData(line.split(","));
		if(!dataSets.containsKey(data.intersectionId))dataSets.put(data.intersectionId, new ArrayList<>());
		dataSets.get(data.intersectionId).add(data);
	}
	bf.close();
	} catch (IOException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	}
	String stationFileName = "src/main/resources/linkMatching_mael_cleaned.csv";
	try {
		BufferedReader bf = new BufferedReader(new FileReader(new File(stationFileName)));
		bf.readLine();//get rid of the header
		String line = null;
		while((line = bf.readLine())!=null) {
			String[] part = line.split(",");
			String intersectionId = part[0];
			String stationId = part[0]+"___"+part[1]+"___"+part[2];// intersectionId, 
			Coord coord = new Coord(Double.parseDouble(part[3]),Double.parseDouble(part[4]));
			String matchedLink = part[5];
			matchedLink = matchedLink.replaceAll("[\\[\\]]", "");
			//System.out.println("Matched Link - "+matchedLink);
			Measurement mm = m.createAnadAddMeasurement(stationId, MeasurementType.linkVolume);
			List<Id<Link>> linkList = new ArrayList<>();
			linkList.add(Id.createLinkId(matchedLink));
			if(!net.getLinks().containsKey(Id.createLinkId(matchedLink))) {
				System.out.println("Link Id "+matchedLink+" not found!!!");
				continue;
			}
			mm.setAttribute(Measurement.linkListAttributeName,linkList);
			
			timeBean.entrySet().forEach(t->{
				Map<String,Double>volume = new HashMap<>();
				if(dataSets.get(intersectionId)!=null) {
				for(DirectionalData data:dataSets.get(intersectionId)) {
					if(data.intersectionId.equals(intersectionId) && data.time_in_secFrom>t.getValue().getFirst() && data.time_in_secto<=t.getValue().getSecond() && data.weekday==true) {
						if(!volume.containsKey(data.date.toString())) {
							volume.put(data.date.toString(), 0.);
						}
						String inoutkey = "";
						if(part[1].equals("NB") && part[2].equals("in")) {
							inoutkey = DirectionalData.NorthBoundIn;
						}else if(part[1].equals("NB") && part[2].equals("out")) {
							inoutkey = DirectionalData.NorthBoundOut;
						}else if(part[1].equals("SB") && part[2].equals("in")) {
							inoutkey = DirectionalData.SouthBoundIn;
						}else if(part[1].equals("SB") && part[2].equals("out")) {
							inoutkey = DirectionalData.SouthBoundOut;
						}else if(part[1].equals("EB") && part[2].equals("in")) {
							inoutkey = DirectionalData.EastBoundIn;
						}else if(part[1].equals("EB") && part[2].equals("out")) {
							inoutkey = DirectionalData.EastBoundOut;
						}else if(part[1].equals("WB") && part[2].equals("in")) {
							inoutkey = DirectionalData.WestBoundIn;
						}else if(part[1].equals("WB") && part[2].equals("out")) {
							inoutkey = DirectionalData.WestBoundOut;
						}
						volume.put(data.date.toString(), volume.get(data.date.toString())+data.getDirectionalInOutVolumePCU().get(inoutkey));
					}
					
				}
				double val = 0;
				for(double d:volume.values()) {
					val+=d;
				}
				if(volume.size()!=0) {
					val = val/volume.size();
					mm.putVolume(t.getKey(), val);
					timeBeanUnique.add(t.getKey());
				}
				
				}
			});
			
		}
		
		bf.close();
	} catch (FileNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	for(String t:new HashSet<>(m.getTimeBean().keySet())) {
		if(!timeBeanUnique.contains(t))m.getTimeBean().remove(t);
	}
	new MeasurementsWriter(m).write("src\\main\\resources\\montrealMeasurements_2020_2022.xml");
	System.out.println("Total Measurements = "+m.getMeasurements().size());
}
}
