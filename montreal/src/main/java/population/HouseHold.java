package population;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsFactory;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;


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
	private double newExpFac = 0;
	private double limitingFactor = 0;
	
	public HouseHold(String id, int income, double x, double y, Double ct, double hhExFac, boolean ifKids, int numofCar) {
		hhId = Id.create(id, HouseHold.class);
		originalCoord = new Coord(x,y);
		this.ct = ct;
		this.incomeGroup = income;
		this.hhExFac = hhExFac;
		this.numOfCar = numofCar;
		this.newExpFac = this.hhExFac;
		this.limitingFactor = this.hhExFac;
	}
	
	
	
	public double getNewExpFac() {
		return newExpFac;
	}



	public void setNewExpFac(double newExpFac) {
		this.newExpFac = newExpFac;
	}



	public Member addMember(Member member) {
		members.put(member.getMemId(), member);
		if(member.getPersonExFac()<this.limitingFactor)this.limitingFactor=member.getPersonExFac();
		size++;
		return member;
	}
	
	public double getLimitingFactor() {
		return limitingFactor;
	}



	public void setLimitingFactor(double limitingFactor) {
		this.limitingFactor = limitingFactor;
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
	
	public String generateBehavioralKey() {// this will determine what behaviors are taken into account
		return generateBehavioralKey(this.ct,this.incomeGroup,this.size,this.ifKids);
	}
	
	public static String generateBehavioralKey(Double ct, int incomeGroup, int size, boolean ifKid) {// this will determine what behaviors are taken into account
		String key = "";
		key = key+ct+"___";
		key = key+incomeGroup+"___";
		key = key+size+"____";
		key = key+ifKid;
		
		return key;
	}
	
	public void loadClonedHouseHoldPersonAndVehicle(Population population, Vehicles vehicles, Households houseHolds, Map<String,Map<Double,Id<ActivityFacility>>>facilities, double scale,
			Map<String,Map<Id<HouseHold>,Double>>hhSpare,Map<String,Map<Id<Member>,Double>>memberSpare, Map<String,Map<Id<Trip>,Double>>tripSpare) {
		PopulationFactory popFac = population.getFactory();
		HouseholdsFactory hhFac = houseHolds.getFactory();
		VehiclesFactory vFac = vehicles.getFactory();
		
		double maxClone = (int)this.limitingFactor*scale;
		
		if(this.hhExFac-this.limitingFactor!=0) {
			hhSpare.compute(this.generateBehavioralKey(), (k,v)->v==null?new HashMap<>():v);
			hhSpare.get(this.generateBehavioralKey()).put(hhId, scale*(this.hhExFac-this.limitingFactor));			
		}
		
		for(int i=0;i<maxClone;i++) {
			Household hh = hhFac.createHousehold(Id.create(hhId.toString()+"_"+i, Household.class));
			hh.getAttributes().putAttribute("income_class", this.incomeGroup);
			hh.getAttributes().putAttribute("ifHasKid", this.isIfKids());
			hh.getAttributes().putAttribute("vehicles", this.numOfCar);
			hh.getAttributes().putAttribute("census tracts", this.ct);
			
			
			houseHolds.getHouseholds().put(hh.getId(), hh);
			
			int carCreated = 0;
			
			for(Member member:this.members.values()) {
				
				Person person = popFac.createPerson(Id.createPersonId(member.getMemId().toString()+"_"+i));
				person.getAttributes().putAttribute("age", member.getAgeGroup());
				person.getAttributes().putAttribute("gender", member.getGender());
				person.getAttributes().putAttribute("occupation", member.getOccupation());
				hh.getMemberIds().add(person.getId());
				
				person.getAttributes().putAttribute("work census tract", member.getWorkCT());
				
				person.getAttributes().putAttribute("home census tract", this.ct);
				person.getAttributes().putAttribute("license", member.isIfHaveLicense());
				
				person.getAttributes().putAttribute("household id", hh.getId().toString());
				
				if(member.getPersonExFac()-this.limitingFactor!=0) {
					memberSpare.compute(member.generateBehavioralKey(), (k,v)->v==null?new HashMap<>():v);
					memberSpare.get(member.generateBehavioralKey()).put(member.getMemId(), scale*(member.getPersonExFac()-this.limitingFactor));			
				}
				
				population.addPerson(person);
				
				if(member.getTrips().size()==0) {
					if(this.ct!=null) {
						Plan plan = popFac.createPlan();
						plan.addActivity(popFac.createActivityFromActivityFacilityId("home",facilities.get("home").get(this.ct)));
						person.addPlan(plan);
					}
				}else {
					Plan plan = popFac.createPlan();
					
				}
				
			}
			
		}
		
	}
	
}
