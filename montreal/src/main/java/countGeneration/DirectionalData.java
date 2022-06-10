package countGeneration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.collections.Tuple;

/**
 * This class corresponds to each row of the directional data csv from https://open.canada.ca/data/en/dataset/584de76b-13b9-47ea-af12-0c37b8eb5de5
 * @param row
 */
public class DirectionalData{
	
	private DateTimeFormatter formatterDate = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private DateTimeFormatter formatterTime = DateTimeFormatter.ofPattern("hh:mm:ss");
	Set<Id<Link>> linkIds= new HashSet<>();
	LocalDate date;
	boolean weekday = true;
	String intersectionId;
	String intersectionName;
	String refId;
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
	double time_in_secFrom;
	double time_in_secto;
	Coord coordGPS;
	Coord coord;
	int vt;
	String vtName;
	double vtpcu;
	double nbin;
	double nbout;
	double sbin;
	double sbout;
	double ebin;
	double ebout;
	double wbin;
	double wbout;
	public final static String NorthBoundLeft = "nbl";
	public final static String NorthBoundRight = "nbr";
	public final static String NorthBoundThrough = "nbt";
	public final static String SouthBoundLeft = "sbl";
	public final static String SouthBoundRight = "sbr";
	public final static String SouthBoundThrough = "sbt";
	public final static String EastBoundLeft = "ebl";
	public final static String EastBoundRight = "ebr";
	public final static String EastBoundThrough = "ebt";
	public final static String WestBoundLeft = "wbl";
	public final static String WestBoundRight = "wbr";
	public final static String WestBoundThrough = "wbt";
	
	public final static String NorthBoundIn = "nbi";
	public final static String NorthBoundOut = "nbo";
	public final static String SouthBoundIn = "sbi";
	public final static String SouthBoundOut = "sbo";
	public final static String EastBoundIn = "ebi";
	public final static String EastBoundOut = "ebo";
	public final static String WestBoundIn = "wbi";
	public final static String WestBoundOut = "wbo";
	
	public DirectionalData(String refId,String intersectionId,String intersectionName,String date, double hour, double min, double sec,int vt, String vtName, double vtpcu, double nbl, double nbt, double nbr,
			double sbl, double sbt, double sbr, double ebl, double ebt, double ebr, double wbl, double wbt, double wbr,Coord coordGPS,Coord coord) {
		this.date = LocalDate.parse(date, formatterDate);
		if(this.date.getDayOfWeek().equals(DayOfWeek.SATURDAY)||this.date.getDayOfWeek().equals(DayOfWeek.SUNDAY)) {
			this.weekday = false;
		};
		this.intersectionId = intersectionId;
		this.intersectionName = intersectionName;
		this.vtName = vtName;
		this.refId = refId;
		this.time_in_secto = hour*3600+(min+15)*60+sec;
		this.time_in_secFrom = hour*3600+(min)*60+sec+1;
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
		this.coordGPS = coordGPS;
		this.coord = coord;
	}
	
	
	
	public static Map<String,Double> getDefaultPCU(){
		Map<String,Double> pcuMap = new HashMap<>();
		pcuMap.put("0",	1.);//car
		pcuMap.put("2",	5.);//Heavy Trucks
		pcuMap.put("10", 0.);//pedestrian
		pcuMap.put("11", 0.5);//bikes
		pcuMap.put("12", 3.);//bus
		pcuMap.put("15", 3.);//Camions porteurs
		pcuMap.put("16", 5.);//Camions articules
		pcuMap.put("17", 0.5);//motorcycle
		pcuMap.put("100", 0.);//total we will not take this while consideration
		pcuMap.put("3", 3.);//Bus et Camions porteurs looks like simple bus or truck
		return pcuMap;
	}
	public DirectionalData(String[] part) {
		this(part[0],part[1],part[2].replaceAll("^\"|\"$", ""), part[3].replaceAll("^\"|\"$", ""), Double.parseDouble(part[5]), Double.parseDouble(part[6]), Double.parseDouble(part[7]), Integer.parseInt(part[8]), 
				part[9].replaceAll("^\"|\"$", ""), getDefaultPCU().get(part[8]), Double.parseDouble(part[10]), Double.parseDouble(part[11]), Double.parseDouble(part[12]), 
				Double.parseDouble(part[13]), Double.parseDouble(part[14]), Double.parseDouble(part[15]), 
				Double.parseDouble(part[16]), Double.parseDouble(part[17]),Double.parseDouble(part[18]),
				Double.parseDouble(part[19]),Double.parseDouble(part[20]),Double.parseDouble(part[21]), new Coord(Double.parseDouble(part[28]),Double.parseDouble(part[29])), new Coord(Double.parseDouble(part[26]),Double.parseDouble(part[27])));
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
		out.put(NorthBoundLeft, nbl);
		out.put(NorthBoundThrough, nbt);
		out.put(NorthBoundRight, nbr);
		out.put(SouthBoundLeft, sbl);
		out.put(SouthBoundThrough, sbt);
		out.put(SouthBoundRight, sbr);
		out.put(EastBoundLeft, ebl);
		out.put(EastBoundThrough, ebt);
		out.put(EastBoundRight, ebr);
		out.put(WestBoundLeft, wbl);
		out.put(WestBoundThrough, wbt);
		out.put(WestBoundRight, wbr);
		
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
		out.put(NorthBoundLeft, nbl);
		out.put(NorthBoundThrough, nbt);
		out.put(NorthBoundRight, nbr);
		out.put(SouthBoundLeft, sbl);
		out.put(SouthBoundThrough, sbt);
		out.put(SouthBoundRight, sbr);
		out.put(EastBoundLeft, ebl);
		out.put(EastBoundThrough, ebt);
		out.put(EastBoundRight, ebr);
		out.put(WestBoundLeft, wbl);
		out.put(WestBoundThrough, wbt);
		out.put(WestBoundRight, wbr);
		out.keySet().forEach(K->out.compute(K, (k,v)->v=v*vtpcu));
		return out;
	}
	/**
	 * Keys: nbi, nbo (north bound in out) and so on..
	 * sbi, sbo
	 * ebi, ebo
	 * wbi, wbo
	 * @return a map containing the pcu data with these specified keys
	 */
	
	public Map<String,Double> getDirectionalInOutVolume() {
		Map<String,Double> out = new HashMap<>();
		out.put(NorthBoundIn, nbin);
		out.put(NorthBoundOut, nbout);
		out.put(SouthBoundIn, sbin);
		out.put(SouthBoundOut, sbout);
		out.put(EastBoundIn, ebin);
		out.put(EastBoundOut, ebout);
		out.put(WestBoundIn, wbin);
		out.put(WestBoundOut, wbout);
		
		//out.keySet().forEach(K->out.compute(K, (k,v)->v=v*vtpcu));
		return out;
	}
	
	/**
	 * Keys: nbi, nbo (north bound in out) and so on..
	 * sbi, sbo
	 * ebi, ebo
	 * wbi, wbo
	 * @return a map containing the pcu data with these specified keys
	 */
	
	public Map<String,Double> getDirectionalInOutVolumePCU() {
		Map<String,Double> out = new HashMap<>();
		out.put(NorthBoundIn, nbin);
		out.put(NorthBoundOut, nbout);
		out.put(SouthBoundIn, sbin);
		out.put(SouthBoundOut, sbout);
		out.put(EastBoundIn, ebin);
		out.put(EastBoundOut, ebout);
		out.put(WestBoundIn, wbin);
		out.put(WestBoundOut, wbout);
		
		out.keySet().forEach(K->out.compute(K, (k,v)->v=v*vtpcu));
		return out;
	}
	
	public static void main(String[] args) throws IOException {
		String dataFileName = "src\\main\\resources\\Data_2020_2022.csv";
		List<DirectionalData> dataSets = new ArrayList<>();
		BufferedReader bf = new BufferedReader(new FileReader(new File(dataFileName)));
		bf.readLine();//get rid of the header
		String line = null;
		while((line = bf.readLine())!=null) {
			dataSets.add(new DirectionalData(line.split(",")));
		}
		bf.close();
	}
}
