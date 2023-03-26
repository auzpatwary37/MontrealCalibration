package population;

import java.util.HashMap;
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
	private Id<HouseHold> hId;
	private int incomeGroup;
	private int gender;
	private int occupation;
	private boolean remoteWork;
	private Coord workCoord;
	
	
	public Member(String id, String hhid, int ageGroup, boolean haveLicense,double personExFac,int incomeGroup,int gender, int occupation
			, boolean remoteWork,Double workX, Double workY) {
		this.hId = Id.create(hId, HouseHold.class);
		this.memId = Id.create(memId, Member.class);
		this.ageGroup = ageGroup;
		this.incomeGroup = incomeGroup;
		this.gender = gender;
		this.personExFac = personExFac;
		this.occupation = occupation;
		this.remoteWork = remoteWork;
		this.workCoord = new Coord(workX,workY);
	}
	
	
	
	
	public Coord getWorkCoord() {
		return workCoord;
	}




	public boolean isRemoteWork() {
		return remoteWork;
	}




	public int getOccupation() {
		return occupation;
	}


	public Trip addTrip(Trip trip) {
		
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
	public Id<HouseHold> gethId() {
		return hId;
	}
	public int getIncomeGroup() {
		return incomeGroup;
	}


	public int getGender() {
		return gender;
	}
	
	
}
