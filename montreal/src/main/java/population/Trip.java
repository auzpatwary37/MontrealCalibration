package population;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;

public class Trip {
	
	private Id<Trip> tripId;
	private Member member;
	
	private double tripExpFactror;
	private Coord originalOCoord;
	private Coord originalDCoord;
	
	private Double originCT;
	private Double destinationCT;
	
	private Double originalOCT;
	private Double originalDCT;
	
	private double departureTime;
	private double arrivalTime;
	
	private String previousAct;
	
	private String[] modes;
	
	private String motive;
	private int mobile;
	
	private String day;
	
	private double newExpFac;
	public int clonedTrip = 0;
	
	@SuppressWarnings("removal")
	public Trip(String tripId, Member member, Double oX, Double oY, Double dX, Double dY, 
			double departureTime, Double tripExpFactor, Double originCt, Double destinationCT,
			String motive, int mobile, String[]modes, String day, CoordinateTransformation tsf, Network odNet) {
		
		this.tripId = Id.create(tripId, Trip.class);
		this.member = member;
		if(tsf!=null) {
			this.originalOCoord = tsf.transform(new Coord(oX,oY));
			this.originalDCoord = tsf.transform(new Coord(dX,dY));
		}else {
			this.originalOCoord = new Coord(oX,oY);
			this.originalDCoord = new Coord(dX,dY);
		}
		if(originCt!=null && Double.compare(originCt, 0.0)!=0) {
			this.originCT = originCt;
			this.originalOCT = originCt;
		}
		if(destinationCT!=null && Double.compare(destinationCT, 0.0)!=0) {
			this.destinationCT = destinationCT;
			this.originalDCT = destinationCT;
		}
		if(odNet!=null && this.originCT==null && this.originalOCoord!=null && Double.compare(this.originalOCoord.getX(), 0.0)!=0 && Double.compare(this.originalOCoord.getY(), 0.0)!=0) {
			this.originCT = Double.parseDouble(NetworkUtils.getNearestNode(odNet, this.originalOCoord).getId().toString());
			this.originalOCT = Double.valueOf(this.originCT);
		}
		if(odNet!=null && this.destinationCT==null && this.originalDCoord!=null && Double.compare(this.originalDCoord.getX(), 0.0)!=0 && Double.compare(this.originalDCoord.getY(), 0.0)!=0) {
			this.destinationCT = Double.parseDouble(NetworkUtils.getNearestNode(odNet, this.originalDCoord).getId().toString());
			this.originalDCT = Double.valueOf(this.destinationCT);
		}
		this.departureTime = departureTime;
		this.motive = motive;
		this.mobile = mobile;
		if(mobile==3)this.member.getHouseHold().setIfKids(true);
		this.modes = modes;
		this.day = day;
		this.tripExpFactror = tripExpFactor;
		this.newExpFac = this.tripExpFactror;
	}
	
	
	

	public void setOriginCT(Double originCT) {
		this.originCT = originCT;
	}




	public void setDestinationCT(Double destinationCT) {
		this.destinationCT = destinationCT;
	}




	public double getNewExpFac() {
		return newExpFac;
	}



	public void setNewExpFac(double newExpFac) {
		this.newExpFac = newExpFac;
	}



	public double getTripExpFactror() {
		return tripExpFactror;
	}


	public Id<Trip> getTripId() {
		return tripId;
	}

	public Member getMember() {
		return member;
	}

	public Coord getOriginalOCoord() {
		return originalOCoord;
	}

	public Coord getOriginalDCoord() {
		return originalDCoord;
	}

	public Double getOriginCT() {
		return originCT;
	}

	public Double getDestinationCT() {
		return destinationCT;
	}

	public double getDepartureTime() {
		return departureTime;
	}

	public double getArrivalTime() {
		return arrivalTime;
	}

	public String[] getModes() {
		return modes;
	}

	public String getMotive() {
		return motive;
	}


	public int getMobile() {
		return mobile;
	}
	
	public String getDay() {
		return this.day;
	}


	public String getPreviousAct() {
		return previousAct;
	}


	public void setPreviousAct(String previousAct) {
		this.previousAct = previousAct;
	}
	
	
	public String generateBehavioralKey() {// this will determine what behaviors are taken into account
		return generateBehavioralKey(this.originCT,this.destinationCT,this.previousAct,this.motive,this.departureTime, this.modes[0]);
	}
	
	
	
	public Double getOriginalOCT() {
		return originalOCT;
	}




	public Double getOriginalDCT() {
		return originalDCT;
	}




	public void setOriginalOCoord(Coord originalOCoord) {
		this.originalOCoord = originalOCoord;
	}




	public void setOriginalDCoord(Coord originalDCoord) {
		this.originalDCoord = originalDCoord;
	}




	public static String generateBehavioralKey(Double originCT, Double destinationCT, String previousAct,String currentAct, Double departureTime, String mode) {// this will determine what behaviors are taken into account
		String key = "";
		key = key+originCT+"___";
		key = key+destinationCT+"___";
		key = key+previousAct+"____";
		key = key+currentAct+"___";
		key = key+mode+"___";
		key = key+departureTime;
		
		return key;
	}
	
	public String getMode() {
		Set<String> uniqueModes = new HashSet<>();
		for(String s:modes)uniqueModes.add(s);
		if(uniqueModes.contains("pt"))return "pt";
		if(uniqueModes.contains("car"))return "car";
		if(uniqueModes.contains("bike"))return "bike";
		if(uniqueModes.contains("car_passenger"))return "car_passenger";
		
		return "walk";
	}
	
}
