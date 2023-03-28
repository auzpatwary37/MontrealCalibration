package population;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsFactory;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
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
	
	public void loadClonedHouseHoldPersonAndVehicle(Population population, Vehicles vehicles, Households houseHolds, Map<String,Map<Double,Set<Id<ActivityFacility>>>>facilities, double scale,
			Map<String,Map<Id<HouseHold>,Double>>hhSpare,Map<String,Map<Id<Member>,Double>>memberSpare, Map<String,Map<Id<Trip>,Double>>tripSpare) {
		PopulationFactory popFac = population.getFactory();
		HouseholdsFactory hhFac = houseHolds.getFactory();
		VehiclesFactory vFac = vehicles.getFactory();
		
		VehicleType carType = vFac.createVehicleType(Id.create("car", VehicleType.class));
		carType.setNetworkMode("car");
		carType.setPcuEquivalents(1);
		
		
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
				int addionalMember = (int)(member.getAdditionalMemberExpansionFactor()*scale);
				
				for(int k = 0;k<=addionalMember;k++) {
				
				Person person = popFac.createPerson(Id.createPersonId(member.getMemId().toString()+"_"+i));
				person.getAttributes().putAttribute("age", member.getAgeGroup());
				person.getAttributes().putAttribute("gender", member.getGender());
				person.getAttributes().putAttribute("occupation", member.getOccupation());
				if(k==0)hh.getMemberIds().add(person.getId());
				
				person.getAttributes().putAttribute("work census tract", member.getWorkCT());
				
				person.getAttributes().putAttribute("home census tract", this.ct);
				person.getAttributes().putAttribute("license", member.isIfHaveLicense());
				
				if(k==0)person.getAttributes().putAttribute("household id", hh.getId().toString());
				
				if(member.getPersonExFac()-this.limitingFactor!=0) {
					memberSpare.compute(member.generateBehavioralKey(), (kk,v)->v==null?new HashMap<>():v);
					memberSpare.get(member.generateBehavioralKey()).put(member.getMemId(), scale*(member.getPersonExFac()-this.limitingFactor-addionalMember));			
				}
				
				population.addPerson(person);
				boolean ifCarRequired = false;
				if(member.getTrips().size()==0) {
					if(this.ct!=null) {
						Plan plan = popFac.createPlan();
						plan.addActivity(popFac.createActivityFromActivityFacilityId("home",drawRandom(facilities.get("home").get(this.ct))));
						person.addPlan(plan);
					}
				}else {
					Plan plan = popFac.createPlan();
					person.addPlan(plan);
					int j = 0;
					double previousCT = 0;
					
					for(Trip trip:member.getTrips().values()) {
						
						if(trip.getOriginCT()!=null && trip.getDestinationCT()!=null) {
							if(j==0) {
								if(this.ct!=null && Double.compare(trip.getOriginCT(),this.ct)!=0) {
									System.out.println("First trip origin is not home. Setting as home trip nonetheless.");
								}
								Activity start = popFac.createActivityFromActivityFacilityId("home", drawRandom(facilities.get("home").get(trip.getOriginCT())));
								start.setEndTime(trip.getDepartureTime());
								
								plan.addActivity(start);
								
								plan.addLeg(popFac.createLeg(trip.getModes()[0]));
								if(trip.getModes()[0].equals("car") && ifCarRequired==false)ifCarRequired = true;
								
								plan.addActivity(popFac.createActivityFromActivityFacilityId(trip.getMotive(), drawRandom(facilities.get(trip.getMotive()).get(trip.getDestinationCT()))));
								
								previousCT = trip.getDestinationCT();
								
								int extraTrips = 0;
								if(k==0 && (extraTrips = (int) (scale*(trip.getTripExpFactror()-this.limitingFactor-addionalMember)))>0){// check for additional trips left
									for(int l = 0;l<extraTrips;l++) {
										Person tripPerson = popFac.createPerson(Id.createPersonId(trip.getTripId().toString()+"_"+l));
										Plan tripPlan = popFac.createPlan();
										tripPerson.addPlan(tripPlan);
										Activity startTrip = popFac.createActivityFromActivityFacilityId("home", drawRandom(facilities.get("home").get(trip.getOriginCT())));
										startTrip.setEndTime(trip.getDepartureTime());
										tripPlan.addActivity(startTrip);
										tripPlan.addLeg(popFac.createLeg(trip.getModes()[0]));
										tripPlan.addActivity(popFac.createActivityFromActivityFacilityId(trip.getMotive(), drawRandom(facilities.get(trip.getMotive()).get(trip.getDestinationCT()))));
										population.addPerson(tripPerson);
										if(trip.getModes()[0].equals("car")) {
											Vehicle vehicle = vFac.createVehicle(Id.create(tripPerson.getId().toString(),Vehicle.class), carType);
											vehicles.addVehicle(vehicle);
											carCreated++;
											Map<String,Id<Vehicle>> v = new HashMap<>();
											v.put("car",vehicle.getId());
											VehicleUtils.insertVehicleIdsIntoAttributes(tripPerson,v);
											
										}
									}
								}
								
							}else {
									
								if(trip.getOriginCT().compareTo(previousCT)!=0) {
									System.out.println("Discontinueous chain!!!");
									
									Activity previousAct = ((Activity)plan.getPlanElements().get(plan.getPlanElements().size()-1));
									previousAct.setEndTime(trip.getDepartureTime());
									if(member.isIfHaveLicense()) {
										plan.addLeg(popFac.createLeg("car"));
										if(ifCarRequired==false)ifCarRequired = true;
									}else {
										plan.addLeg(popFac.createLeg("pt"));
									}
									Activity a = popFac.createActivityFromActivityFacilityId(previousAct.getType(), drawRandom(facilities.get(previousAct.getType()).get(trip.getOriginCT())));
									a.setEndTime(trip.getDepartureTime());
									plan.addActivity(a);
									
									plan.addLeg(popFac.createLeg(trip.getModes()[0]));
									if(trip.getModes()[0].equals("car") && ifCarRequired==false)ifCarRequired = true;
									plan.addActivity(popFac.createActivityFromActivityFacilityId(trip.getMotive(), drawRandom(facilities.get(trip.getMotive()).get(trip.getDestinationCT()))));
									
								}else {
									Activity previousAct = ((Activity)plan.getPlanElements().get(plan.getPlanElements().size()-1));
									previousAct.setEndTime(trip.getDepartureTime());
									plan.addLeg(popFac.createLeg(trip.getModes()[0]));
									plan.addActivity(popFac.createActivityFromActivityFacilityId(trip.getMotive(), drawRandom(facilities.get(trip.getMotive()).get(trip.getDestinationCT()))));
								}
								int extraTrips = 0;
								if(k==0 && (extraTrips = (int) (scale*(trip.getTripExpFactror()-this.limitingFactor-addionalMember)))>0){// check for additional trips left
									for(int l = 0;l<extraTrips;l++) {
										Person tripPerson = popFac.createPerson(Id.createPersonId(trip.getTripId().toString()+"_"+l));
										Plan tripPlan = popFac.createPlan();
										tripPerson.addPlan(tripPlan);
										String previousActType = ((Activity)plan.getPlanElements().get(plan.getPlanElements().size()-1)).getType();
										Activity previousAct =  popFac.createActivityFromActivityFacilityId(previousActType, drawRandom(facilities.get(previousActType).get(previousCT)));
										previousAct.setEndTime(trip.getDepartureTime());
										tripPlan.addActivity(previousAct);
										tripPlan.addLeg(popFac.createLeg(trip.getModes()[0]));
										tripPlan.addActivity(popFac.createActivityFromActivityFacilityId(trip.getMotive(), drawRandom(facilities.get(trip.getMotive()).get(trip.getDestinationCT()))));
										population.addPerson(tripPerson);
										if(trip.getModes()[0].equals("car")) {
											Vehicle vehicle = vFac.createVehicle(Id.create(tripPerson.getId().toString(),Vehicle.class), carType);
											vehicles.addVehicle(vehicle);
											carCreated++;
											Map<String,Id<Vehicle>> v = new HashMap<>();
											v.put("car",vehicle.getId());
											VehicleUtils.insertVehicleIdsIntoAttributes(tripPerson,v);
											
										}
									}
								}
								
							}
									
						}
						j++;
					}
				}
				
				if(ifCarRequired==true && (carCreated-this.numOfCar)>0) {
					Vehicle vehicle = vFac.createVehicle(Id.create(person.getId().toString(),Vehicle.class), carType);
					vehicles.addVehicle(vehicle);
					carCreated++;
					Map<String,Id<Vehicle>> v = new HashMap<>();
					v.put("car",vehicle.getId());
					VehicleUtils.insertVehicleIdsIntoAttributes(person,v);
					if(k==0)hh.getVehicleIds().add(vehicle.getId());
				}
				
			}
			}
			
		}
		
	}
	
	public static<T> T drawRandom(Collection<T> collection) {
		if(collection.isEmpty())throw new IllegalArgumentException("Collection is empty. Cannot draw!!!");
		int num = (int) (Math.random() * collection.size());
	    for(T t: collection) if (--num < 0) return t;
	    throw new AssertionError();
	}
	
}
