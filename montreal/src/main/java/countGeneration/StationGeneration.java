package countGeneration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.System.Logger;
import java.util.ArrayList;
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
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;

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
	//timeBean.put("AADT", new Tuple<Double,Double>(0.,24*3600.));
	Map<Id<Link>,Set<Measurement>> linkToM = new HashMap<>();
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
	String stationFileName = "src/main/resources/linkMatching_mael_cleanedJune28.csv";
	Map<Id<Link>,Measurement> linkBasedMeasurements = new HashMap<>();
	Map<Id<Link>,Map<String,List<Map<String,Double>>>> countsPerLinkPerTimePerDate = new HashMap<>();
	try {
		BufferedReader bf = new BufferedReader(new FileReader(new File(stationFileName)));
		bf.readLine();//get rid of the header
		String line = null;
		int rep = 0;
		while((line = bf.readLine())!=null) {
			String[] part = line.split(",");
			String intersectionId = part[0];
			String stationId = part[0]+"___"+part[1]+"___"+part[2];// intersectionId, 
//			if(stationId.equals("416___SB___in")) {
//				System.out.println("debug!!!");
//			}
			Coord coord = new Coord(Double.parseDouble(part[3]),Double.parseDouble(part[4]));
			String matchedLink = part[5];
			matchedLink = matchedLink.replaceAll("[\\[\\]]", "");
			//System.out.println("Matched Link - "+matchedLink);
			Measurement mm = m.createAnadAddMeasurement(stationId, MeasurementType.linkVolume);
			List<Id<Link>> linkList = new ArrayList<>();
			Id<Link> lId = Id.createLinkId(matchedLink);
			linkList.add(lId);
			if(!linkBasedMeasurements.containsKey(lId)) {
				linkBasedMeasurements.put(lId, mm);
				if(!net.getLinks().containsKey(Id.createLinkId(matchedLink))) {
					System.out.println("Link Id "+matchedLink+" not found!!!");
					continue;
				}
				mm.setAttribute(Measurement.linkListAttributeName,linkList);
				
			}else {
				m.removeMeasurement(mm.getId());
				mm = linkBasedMeasurements.get(lId);
				
			}
			
			
			
			
			for(Entry<String, Tuple<Double, Double>> t:timeBean.entrySet()){
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
				if(!countsPerLinkPerTimePerDate.containsKey(lId))countsPerLinkPerTimePerDate.put(lId, new HashMap<>());
				if(!countsPerLinkPerTimePerDate.get(lId).containsKey(t.getKey()))countsPerLinkPerTimePerDate.get(lId).put(t.getKey(), new ArrayList<>());
				countsPerLinkPerTimePerDate.get(lId).get(t.getKey()).add(volume);

				double val = 0;
				int counter = 0;
				for(Map<String,Double> v:countsPerLinkPerTimePerDate.get(lId).get(t.getKey())) {
					for(double d:v.values()) {
						val+=d;
						counter++;
					}
				}
				
				
				
				if(counter!=0) {
					val = val/counter;
					mm.putVolume(t.getKey(), val);
					timeBeanUnique.add(t.getKey());
				}
				
				}
			}
			
			if(!linkToM.containsKey(Id.createLinkId(matchedLink))) {
				linkToM.put(Id.createLinkId(matchedLink), new HashSet<>());
				linkToM.get(Id.createLinkId(matchedLink)).add(mm);
			}else {
				Set<Measurement> mms = linkToM.get(Id.createLinkId(matchedLink));
				linkToM.get(Id.createLinkId(matchedLink)).add(mm);
				rep++;
			}
				
		}
		System.out.println("Total repetation = "+rep);
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

	Counts<Link> con = new Counts<Link>();
	m.getMeasurements().values().forEach(mm->{
		Count<Link> c = con.createAndAddCount(((List<Id<Link>>)mm.getAttributes().get(Measurement.linkListAttributeName)).get(0), mm.getId().toString());
		mm.getVolumes().entrySet().forEach(v->{
			c.createVolume(Integer.parseInt(v.getKey()), v.getValue());
		});
		
	});
	new CountsWriter(con).write("src\\main\\resources\\countsMontreal_2020_2022.xml");
	
	try {
		FileWriter fw = new FileWriter(new File("src\\main\\resources\\problems.csv"));
		linkToM.entrySet().forEach(e->{
			if(e.getValue().size()>1) {
				boolean rep = false;
				List<String> sId = new ArrayList<>();
				for(Measurement mId:e.getValue()) {
					String s = mId.getId().toString().split("___")[0];
					if(!sId.contains(s)) {
						sId.add(s);
					}else {
						rep = true;
						break;
					}
				}
				if(rep == true) {
					try {
						fw.append(e.getKey().toString());
						for(Measurement mme:e.getValue()) {
							fw.append(","+mme.getId().toString());
						}
						fw.append("\n");
						fw.flush();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
		});
		fw.close();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}
}
