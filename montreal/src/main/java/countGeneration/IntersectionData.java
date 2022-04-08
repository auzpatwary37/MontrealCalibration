package countGeneration;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class IntersectionData {
	
	private String intersectionId;
	private Set<String> refIds;
	private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	
	
	
	
	

}

class directionalData{
	double nbl;
	double nbt;
	double nbr;
	double sbl;
	double sbt;
	double sbr;
	double ebl;
	double ebt;
	double ebr;
	double wbl;
	double wbt;
	double wbr;
	double time_in_sec;
	int time_in_hr;
	int vt;
	double vtpcu;
	double nbin;
	double nbout;
	double sbin;
	double sbout;
	double ebin;
	double ebout;
	double wbin;
	double wbout;
	
	public directionalData(double hour, double min, double sec,int vt, double vtpcu, double nbl, double nbt, double nbr,
			double sbl, double sbt, double sbr, double ebl, double ebt, double ebr, double wbl, double wbt, double wbr) {
		this.time_in_sec = hour*3600+min*60+sec;
		this.time_in_hr = (int) Math.floor(time_in_sec/3600);
		this.vt = vt;
		this.vtpcu = vtpcu;
		this.nbl = nbl;
		this.nbt = nbt;
		this.nbr = nbr;
		this.sbl = sbl;
		this.sbt = sbt;
		this.sbr = sbr;
		this.ebl = ebl;
		this.ebt = ebt;
		this.ebr = ebr;
		this.wbl = wbl;
		this.wbt = wbt;
		this.wbr = wbr;
		this.nbin = nbl+nbt+nbr;
		this.sbin = sbl+sbt+sbr;
		this.wbin = wbl+wbt+wbr;
		this.ebin = ebl+ebt+ebr;
		this.nbout = nbt+ebl+wbr;
		this.sbout = sbt+wbl+ebr;
		this.ebout = ebt+nbr+sbl;
		this.wbout = wbt+nbl+sbr;
		
	}
	
	/**
	 * Keys: nbl, nbt, nbr (north bound, left, through, right) and so on..
	 * sbl, sbt, sbr,
	 * ebl, ebt, ebr,
	 * wbl, wbt, wbr
	 * @return a map containing the data with these specified keys
	 */
	public Map<String,Double> getDirectionalTurnVolume() {
		Map<String,Double> out = new HashMap<>();
		out.put("nbl", nbl);
		out.put("nbt", nbt);
		out.put("nbr", nbr);
		out.put("sbl", sbl);
		out.put("sbt", sbt);
		out.put("sbr", sbr);
		out.put("ebl", ebl);
		out.put("ebt", ebt);
		out.put("ebr", ebr);
		out.put("wbl", wbl);
		out.put("wbt", wbt);
		out.put("wbr", wbr);
		
		return out;
	}
	
	/**
	 * Keys: nbl, nbt, nbr (north bound, left, through, right) and so on..
	 * sbl, sbt, sbr,
	 * ebl, ebt, ebr,
	 * wbl, wbt, wbr
	 * @return a map containing the pcu data with these specified keys
	 */
	public Map<String,Double> getDirectionalTurnVolumeInPCU() {
		Map<String,Double> out = new HashMap<>();
		out.put("nbl", nbl);
		out.put("nbt", nbt);
		out.put("nbr", nbr);
		out.put("sbl", sbl);
		out.put("sbt", sbt);
		out.put("sbr", sbr);
		out.put("ebl", ebl);
		out.put("ebt", ebt);
		out.put("ebr", ebr);
		out.put("wbl", wbl);
		out.put("wbt", wbt);
		out.put("wbr", wbr);
		out.keySet().forEach(K->out.compute(K, (k,v)->v=v*vtpcu));
		return out;
	}
	
}
