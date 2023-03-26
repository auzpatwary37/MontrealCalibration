package population;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

public class Trip {
	
	private Id<Trip> tripId;
	private Id<Member> memberId;
	
	
	private Coord originalOCoord;
	private Coord originalDCoord;
	
	private Double originCT;
	private Double destinationCT;
	
	private double departureTime;
	private double arrivalTime;
	
	
	private String[] modes;
	
	private int motive;
	
	private Trip(String tripId, String memberId, Double oX, Double oY, Double dX, Double dY, double departureTime, Double arrivalTime, Double originCt, Double destinationCT) {
		this.tripId = Id.create(tripId, Trip.class);
		this.memberId = Id.create(memberId, Member.class);
		this.originalOCoord = new Coord(oX,oY);
		this.originalDCoord = new Coord(dX,dY);
		this.originCT = originCt;
		this.destinationCT = destinationCT;
		
	}
	

	public Id<Trip> getTripId() {
		return tripId;
	}

	public Id<Member> getMemberId() {
		return memberId;
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

	public int getMotive() {
		return motive;
	}
	
	
	
	

}
