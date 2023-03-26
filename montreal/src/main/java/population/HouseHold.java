package population;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;


public class HouseHold {

	private Id<HouseHold> hhId;
	private Map<Id<Member>,Member> members = new HashMap<>();
	private int incomeGroup;
	private int size = 0;
	private boolean ifKids;
	private Coord originalCoord;
	private Double ct;
	private Double hhExFac;
	private int numOfCar;
	private int memSize = 0;
	
	public HouseHold(String id, int income, double x, double y, double ct, double hhExFac, boolean ifKids, int numofCar) {
		hhId = Id.create(id, HouseHold.class);
		originalCoord = new Coord(x,y);
		this.ct = ct;
		this.incomeGroup = income;
		this.hhExFac = hhExFac;
		this.numOfCar = numofCar;
	}
	
	public Member addMember(Member member) {
		members.put(member.getMemId(), member);
		size++;
		return member;
	}
	
	public Id<HouseHold> getHhId() {
		return hhId;
	}
	public Map<Id<Member>, Member> getMembers() {
		return members;
	}
	public int getIncomeGroup() {
		return incomeGroup;
	}
	public int getSize() {
		return size;
	}
	public boolean isIfKids() {
		return ifKids;
	}
	public Coord getOriginalCoord() {
		return originalCoord;
	}
	public Double getCt() {
		return ct;
	}
	public Double getHhExFac() {
		return hhExFac;
	}

	public int getNumOfCar() {
		return numOfCar;
	}

	public int getMemSize() {
		return memSize;
	}

	public void setMemSize(int memSize) {
		this.memSize = memSize;
	}

	public void setIfKids(boolean ifKids) {
		this.ifKids = ifKids;
	}
	
	
}
