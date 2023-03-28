package population;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

public class Member {
	private Id<Member> memId;
	private Map<Id<Trip>,Trip> trips = new HashMap<>();
	int numOfTrips = 0;
	private int ageGroup;
	private boolean ifHaveLicense;
	private double personExFac;
	private HouseHold hh;
	private int incomeGroup;
	private int gender;
	private int occupation;
	private boolean remoteWork;
	private Coord workCoord;
	private Double workCT;
	private Id<Trip> lastTripKey = null;
	private double newExpFac;
	private double limitingFactor;
	
	
	public Member(String id, HouseHold hh, int ageGroup, boolean haveLicense,double personExFac,int incomeGroup,int gender, int occupation
			, boolean remoteWork,Double workX, Double workY, Double workCT) {
		this.hh = hh;
		this.memId = Id.create(memId, Member.class);
		this.ageGroup = ageGroup;
		this.incomeGroup = incomeGroup;
		this.gender = gender;
		this.personExFac = personExFac;
		this.occupation = occupation;
		this.remoteWork = remoteWork;
		if(workX!=null && workY!=null)this.workCoord = new Coord(workX,workY);
		this.newExpFac = this.personExFac;
		this.limitingFactor = this.personExFac;
	}
	
	
	
	
	public double getNewExpFac() {
		return newExpFac;
	}




	public void setNewExpFac(double newExpFac) {
		this.newExpFac = newExpFac;
	}




	public Coord getWorkCoord() {
		return workCoord;
	}


	

	public Double getWorkCT() {
		return workCT;
	}




	public boolean isRemoteWork() {
		return remoteWork;
	}




	public int getOccupation() {
		return occupation;
	}
	

	public int getNumOfTrips() {
		return numOfTrips;
	}




	public HouseHold getHh() {
		return hh;
	}




	public double getLimitingFactor() {
		return limitingFactor;
	}




	public Trip addTrip(Trip trip) {
		if(this.lastTripKey==null) {
			trip.setPreviousAct("home");
		}else {
			trip.setPreviousAct(trips.get(lastTripKey).getMotive());
		}
		if(trip.getTripExpFactror()<this.limitingFactor) {
			this.limitingFactor = trip.getTripExpFactror();
			this.hh.setLimitingFactor(this.limitingFactor);
		}
		this.lastTripKey = trip.getTripId();
		return trip;
	}
	
	public Id<Member> getMemId() {
		return memId;
	}
	public Map<Id<Trip>, Trip> getTrips() {
		return trips;
	}
	public int getAgeGroup() {
		return ageGroup;
	}
	public boolean isIfHaveLicense() {
		return ifHaveLicense;
	}
	public double getPersonExFac() {
		return personExFac;
	}
	public HouseHold getHouseHold() {
		return hh;
	}
	public int getIncomeGroup() {
		return incomeGroup;
	}


	public int getGender() {
		return gender;
	}
	
	public String generateBehavioralKey() {// this will determine what behaviors are taken into account
		return generateBehavioralKey(this.ageGroup,this.gender,this.hh.getCt(),this.workCT);
	}
	
	public static String generateBehavioralKey(int age, int gender, Double homeCT, Double workCT) {// this will determine what behaviors are taken into account
		String key = "";
		key = key+age+"___";
		key = key+gender+"___";
		key = key+homeCT+"____";
		key = key+workCT;
		
		return key;
	}
	
	
}
