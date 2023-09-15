package turnRestriction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.lanes.Lanes;
import org.matsim.lanes.LanesFactory;
import org.matsim.pt2matsim.osm.lib.Osm;
import org.matsim.pt2matsim.osm.lib.OsmData;
import org.matsim.pt2matsim.osm.lib.Osm.Way;
import org.matsim.pt2matsim.osm.lib.Osm.Relation;

public class OsmTurnRestrictions {
 private Map<Id<OsmTurnRestriction>, OsmTurnRestriction> restrictions = new HashMap<>();
	public OsmTurnRestrictions() {
		 
	}
	
	public OsmTurnRestrictions(OsmData osmData) {
		 Map<Id<Relation>,Relation> relation = new HashMap<>();
		 osmData.getRelations().entrySet().forEach(r->{
			 if(r.getValue().getTags().get(Osm.Key.TYPE).equals(Osm.Key.RESTRICTION)) {
				 relation.put(r.getKey(), r.getValue());
			 }
		 });
		 relation.entrySet().forEach(r->{
			 OsmTurnRestriction res = new OsmTurnRestriction(r.getValue());
			 this.restrictions.put(res.getRestrictionId(), res);
		 });
	}
	
	public Map<Id<OsmTurnRestriction>, OsmTurnRestriction> getRestrictions() {
		return restrictions;
	}
 
	/**
	 * Will be a csv file
	 * @param fileLoc
	 */
	public void writeToFile(String fileLoc) {
		try {
			FileWriter fw = new FileWriter(new File(fileLoc));
			for(OsmTurnRestriction r:this.restrictions.values()){
				if(r.active) {
					fw.append(r.toString()+"\n");
					fw.flush();
				}
			}
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static OsmTurnRestrictions readFromFile(String fileLoc) {
		OsmTurnRestrictions rs = new OsmTurnRestrictions();
		String line = null;
		try {
			BufferedReader bf = new BufferedReader(new FileReader(new File(fileLoc)));
			while((line = bf.readLine())!=null) {
				OsmTurnRestriction r = OsmTurnRestriction.parse(line);
				rs.getRestrictions().put(r.getRestrictionId(), r);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return rs;
	}
	
	public void extractLinkIds(Network net) {
		for(OsmTurnRestriction r:this.restrictions.values()) {
			r.extractLinkIds(net);
		}
	}
	
	public void extractLinkIds(Network net, Map<Id<Link>,Id<Way>> mapping) {
		int totalActive = 0;
		for(OsmTurnRestriction r:this.restrictions.values()) {
			boolean out = r.extractLinkIds(net, mapping);
			if(out)totalActive++;
			System.out.println("Total restrictions active = "+totalActive+" out of "+ this.restrictions.size());
		}
		System.out.println("Total restrictions active = "+totalActive+" out of "+ this.restrictions.size());
	}
	
	public void applyRestrictions(Network net, Lanes lane) {
		LanesFactory lFac = lane.getFactory();
		this.restrictions.values().forEach(r->{
			r.applyRestriction(net, lane, lFac);
		});
	}
 
}
