package population;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

public class Trip {
	
	private Id<Trip> tripId;
	private Member member;
	
	private double tripExpFactror;
	private Coord originalOCoord;
	private Coord originalDCoord;
	
	private Double originCT;
	private Double destinationCT;
	
	private double departureTime;
	private double arrivalTime;
	
	private String previousAct;
	
	private String[] modes;
	
	private String motive;
	private int mobile;
	
	private String day;
	
	private double newExpFac;
	
	public Trip(String tripId, Member member, Double oX, Double oY, Double dX, Double dY, 
			double departureTime, Double tripExpFactor, Double originCt, Double destinationCT,
			String motive, int mobile, String[]modes, String day) {
		this.tripId = Id.create(tripId, Trip.class);
		this.member = member;
		this.originalOCoord = new Coord(oX,oY);
		this.originalDCoord = new Coord(dX,dY);
		this.originCT = originCt;
		this.destinationCT = destinationCT;
		this.motive = motive;
		this.mobile = mobile;
		if(mobile==3)this.member.getHouseHold().setIfKids(true);
		this.modes = modes;
		this.day = day;
		this.tripExpFactror = tripExpFactor;
		this.newExpFac = this.tripExpFactror;
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
}
