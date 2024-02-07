package population;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.population.PersonUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
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
	private int cloneHouseHold = 0;
	
	public HouseHold(String id, int income, double x, double y, Double ct, double hhExFac, boolean ifKids, int numofCar) {
		hhId = Id.create(id, HouseHold.class);
		originalCoord = new Coord(x,y);
		if(ct!=null && Double.compare(ct, 0.0)!=0) {
			this.ct = ct;
		}
		this.incomeGroup = income;
		this.hhExFac = hhExFac;
		this.numOfCar = numofCar;
		this.newExpFac = this.hhExFac;
		this.limitingFactor = this.hhExFac;
	}
	
	
	
	public double getNewExpFac() {
		return newExpFac;
	}
	
	public void checkAndUpdateLimitingFactors() {
		this.limitingFactor = this.hhExFac;
		for(Member member:this.members.values()) {
			if(member.getPersonExFac()<this.limitingFactor) {
				this.limitingFactor = member.getPersonExFac();
			}
			for(Trip t:member.getTrips().values()){
				if(t.getTripExpFactror()<this.limitingFactor)this.limitingFactor = t.getTripExpFactror();
			}
		}
		for(Member m:this.members.values()) {
			m.setLimitingFactor(limitingFactor);
		}
	}


	public void checkForCTConsistancy(List<Double>sortedCTList) {
		Collections.sort(sortedCTList);
		if(this.ct!=null && !sortedCTList.contains(this.ct)) {
			if(this.ct<sortedCTList.get(0)) {
				this.ct = sortedCTList.get(0);
			}else if(this.ct>sortedCTList.get(sortedCTList.size()-1)) {
				this.ct = sortedCTList.get(sortedCTList.size()-1);
			}else {
				for(int i=1;i<sortedCTList.size();i++) {
					if(this.ct<sortedCTList.get(i)&&this.ct>sortedCTList.get(i-1)) {
						this.ct = sortedCTList.get(i);
					}
				}
			}
		}
		this.members.values().forEach(m->{
			if(m.getWorkCT()!=null && !sortedCTList.contains(m.getWorkCT())) {
				if(m.getWorkCT()<sortedCTList.get(0)) {
					m.setWorkCT(sortedCTList.get(0));
				}else if(m.getWorkCT()>sortedCTList.get(sortedCTList.size()-1)) {
					m.setWorkCT(sortedCTList.get(sortedCTList.size()-1));
				}else {
					for(int i=1;i<sortedCTList.size();i++) {
						if(m.getWorkCT()<sortedCTList.get(i)&& m.getWorkCT()>sortedCTList.get(i-1)) {
							m.setWorkCT(sortedCTList.get(i));
						}
					}
				}
			}
			
			m.getTrips().values().forEach(t->{
				if(t.getOriginCT()!=null && !sortedCTList.contains(t.getOriginCT())) {
					if(t.getOriginCT()<sortedCTList.get(0)) {
						t.setOriginCT(sortedCTList.get(0));
					}else if(t.getOriginCT()>sortedCTList.get(sortedCTList.size()-1)) {
						t.setOriginCT(sortedCTList.get(sortedCTList.size()-1));
					}else {
						for(int i=1;i<sortedCTList.size();i++) {
							if(t.getOriginCT()<sortedCTList.get(i)&& t.getOriginCT()>sortedCTList.get(i-1)) {
								t.setOriginCT(sortedCTList.get(i));
							}
						}
					}
				}
				
				if(t.getDestinationCT()!=null && !sortedCTList.contains(t.getDestinationCT())) {
					if(t.getDestinationCT()<sortedCTList.get(0)) {
						t.setDestinationCT(sortedCTList.get(0));
					}else if(t.getDestinationCT()>sortedCTList.get(sortedCTList.size()-1)) {
						t.setDestinationCT(sortedCTList.get(sortedCTList.size()-1));
					}else {
						for(int i=1;i<sortedCTList.size();i++) {
							if(t.getDestinationCT()<sortedCTList.get(i)&& t.getDestinationCT()>sortedCTList.get(i-1)) {
								t.setDestinationCT(sortedCTList.get(i-1));
							}
						}
					}
				}
			});
		});
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
	
	public void loadClonedHouseHoldPersonAndVehicle(Population population, Vehicles vehicles, Households houseHolds, ActivityFacilities matsimFacilities, Map<String,Map<Double,Set<Id<ActivityFacility>>>>facilities, double scale,
			Map<String,Map<Id<HouseHold>,Double>>hhSpare,Map<String,Map<Id<Member>,Double>>memberSpare, Map<String,Map<Id<Trip>,Double>>tripSpare, double timeToSpread, Random random) {
		PopulationFactory popFac = population.getFactory();
		HouseholdsFactory hhFac = houseHolds.getFactory();
		VehiclesFactory vFac = vehicles.getFactory();
		ActivityFacilitiesFactory facilityFac = matsimFacilities.getFactory();

		VehicleType carType = vFac.createVehicleType(Id.create("car", VehicleType.class));
		carType.setNetworkMode("car");
		carType.setPcuEquivalents(1);
		if(!vehicles.getVehicleTypes().containsKey(Id.create("car",VehicleType.class)))vehicles.addVehicleType(carType);

		double maxClone = (int)Math.round((this.limitingFactor*scale));

		if(this.hhExFac-this.limitingFactor!=0) {
			hhSpare.compute(this.generateBehavioralKey(), (k,v)->v==null?new HashMap<>():v);
			hhSpare.get(this.generateBehavioralKey()).put(hhId, scale*(this.hhExFac-this.limitingFactor));			
		}

		for(int i=0;i<maxClone;i++) {
			this.cloneHouseHold++;
			Household hh = hhFac.createHousehold(Id.create(hhId.toString()+"_"+i, Household.class));
			hh.getAttributes().putAttribute("income_class", this.incomeGroup);
			hh.getAttributes().putAttribute("ifHasKid", this.isIfKids());
			hh.getAttributes().putAttribute("vehicles", this.numOfCar);
			if(this.ct!=null)hh.getAttributes().putAttribute("census tracts", this.ct);


			houseHolds.getHouseholds().put(hh.getId(), hh);

			int carCreated = 0;

			for(Member member:this.members.values()) {
				int addionalMember = (int)Math.round((member.getAdditionalMemberExpansionFactor(scale)*scale));
				if(i!=0)addionalMember = -1;

				for(int k = 0;k<=addionalMember;k++) {
					member.clonedMember++;
					Person person = popFac.createPerson(Id.createPersonId(member.getMemId().toString()+"_i"+i+"_k"+k));
					person.getAttributes().putAttribute("age", member.getAgeGroup());
					person.getAttributes().putAttribute("gender", member.getGender());
					person.getAttributes().putAttribute("occupation", member.getOccupation());
					person.getAttributes().putAttribute("hh income group", this.incomeGroup);
					if(k==0)hh.getMemberIds().add(person.getId());

					if(member.getWorkCT()!=null)person.getAttributes().putAttribute("work census tract", member.getWorkCT());

					if(this.ct!=null)person.getAttributes().putAttribute("home census tract", this.ct);
					person.getAttributes().putAttribute("license", member.isIfHaveLicense());

					if(k==0)person.getAttributes().putAttribute("household id", hh.getId().toString());

					if(member.getPersonExFac()-this.limitingFactor!=0) {
						memberSpare.compute(member.generateBehavioralKey(), (kk,v)->v==null?new HashMap<>():v);
						memberSpare.get(member.generateBehavioralKey()).put(member.getMemId(), scale*(member.getPersonExFac()-this.limitingFactor)-addionalMember);			
					}

					
					boolean ifCarRequired = false;
					if(member.getTrips().size()==0) {
						if(this.ct!=null) {
							Plan plan = popFac.createPlan();
							plan.addActivity(popFac.createActivityFromActivityFacilityId("home",drawRandomFacility(facilities, matsimFacilities, facilityFac, this.originalCoord, this.ct, "home", "home_"+this.hhId+"_"+i)));
							person.addPlan(plan);
							population.addPerson(person);
						}
					}else {
						boolean shouldAdd = true;
						Plan plan = popFac.createPlan();

						int j = 0;
						double previousCT = 0;
						Coord previousDCoord = null;

						for(Trip trip:member.getTrips().values()) {
							trip.clonedTrip++;
							if(trip.getOriginCT()!=null && trip.getDestinationCT()!=null) {
								if(j==0) {
									if(this.ct!=null && Double.compare(trip.getOriginCT(),this.ct)!=0) {
										System.out.println("First trip origin is not home. Setting as home trip nonetheless.");
									}
									Activity start = popFac.createActivityFromActivityFacilityId("home", drawRandomFacility(facilities, matsimFacilities, facilityFac, trip.getOriginalOCoord(), trip.getOriginCT(), "home", "home_"+this.hhId+"_"+i));
									double dTime = trip.getDepartureTime()+random.nextGaussian(0,timeToSpread);
									if(dTime<0)dTime=0;
									dTime = trip.getDepartureTime();
									start.setEndTime(dTime);

									plan.addActivity(start);

									plan.addLeg(popFac.createLeg(trip.getMode()));
									if(trip.getMode().equals("car") && ifCarRequired==false)ifCarRequired = true;

									plan.addActivity(popFac.createActivityFromActivityFacilityId(trip.getMotive(), drawRandomFacility(facilities, matsimFacilities, facilityFac, trip.getOriginalDCoord(), trip.getDestinationCT(), trip.getMotive(),trip.getMotive()+"_D_"+trip.getTripId())));

									if(trip.getDay()!=null && !trip.getDay().equals(""))plan.getAttributes().putAttribute("dayOfWeek", trip.getDay());

									int extraTrips = (int)Math.round((scale*trip.getTripExpFactror()-Math.round(scale*this.limitingFactor))-addionalMember);
									if(i==0 && k==0 && extraTrips>0){// check for additional trips left
										for(int l = 0;l<extraTrips;l++) {
											trip.clonedTrip++;
											Person tripPerson = popFac.createPerson(Id.createPersonId(trip.getTripId().toString()+"_"+l));
											tripPerson.getAttributes().putAttribute("age", member.getAgeGroup());
											tripPerson.getAttributes().putAttribute("gender", member.getGender());
											tripPerson.getAttributes().putAttribute("occupation", member.getOccupation());
											if(member.getWorkCT()!=null)tripPerson.getAttributes().putAttribute("work census tract", member.getWorkCT());
											if(this.ct!=null)tripPerson.getAttributes().putAttribute("home census tract", this.ct);
											tripPerson.getAttributes().putAttribute("license", member.isIfHaveLicense());
											Plan tripPlan = popFac.createPlan();
											tripPerson.addPlan(tripPlan);
											tripPerson.getAttributes().putAttribute("personTyp", "tripPerson");
											Activity startTrip = popFac.createActivityFromActivityFacilityId("home", drawRandomFacility(facilities, matsimFacilities, facilityFac, trip.getOriginalOCoord(), trip.getOriginCT(),"home", "home_"+trip.getTripId()+"_O_"+l));
											dTime = trip.getDepartureTime()+random.nextGaussian(0,timeToSpread);
											if(dTime<0)dTime=0;
											dTime = trip.getDepartureTime();
											startTrip.setEndTime(dTime);
											tripPlan.addActivity(startTrip);
											tripPlan.addLeg(popFac.createLeg(trip.getMode()));
											tripPlan.addActivity(popFac.createActivityFromActivityFacilityId(trip.getMotive(), drawRandomFacility(facilities, matsimFacilities, facilityFac,trip.getOriginalDCoord(),trip.getDestinationCT(),trip.getMotive(),trip.getMotive()+"_D_"+trip.getTripId()+"_"+l)));
											if(trip.getDay()!=null && !trip.getDay().equals(""))tripPlan.getAttributes().putAttribute("dayOfWeek", trip.getDay());
											population.addPerson(tripPerson);
											if(trip.getMode().equals("car")) {
												Vehicle vehicle = vFac.createVehicle(Id.create(tripPerson.getId().toString(),Vehicle.class), carType);
												vehicles.addVehicle(vehicle);
												Map<String,Id<Vehicle>> v = new HashMap<>();
												v.put("car",vehicle.getId());
												VehicleUtils.insertVehicleIdsIntoAttributes(tripPerson,v);
												PersonUtils.setCarAvail(tripPerson, "always");
											}else {
												PersonUtils.setCarAvail(tripPerson, "never");
											}
										}
									}

								}else {

									if(trip.getOriginCT().compareTo(previousCT)!=0) {
										System.out.println("Discontinueous chain!!!");

										Activity previousAct = ((Activity)plan.getPlanElements().get(plan.getPlanElements().size()-1));
										double dTime = trip.getDepartureTime()+random.nextGaussian(0,timeToSpread);
										if(dTime<0)dTime=0;
										dTime = trip.getDepartureTime();
										previousAct.setEndTime(dTime);
										plan.addLeg(popFac.createLeg("walk"));
//										if(member.isIfHaveLicense()) {
//											plan.addLeg(popFac.createLeg("car"));
//											if(ifCarRequired==false)ifCarRequired = true;
//										}else {
//											plan.addLeg(popFac.createLeg("pt"));
//										}
										Activity a = popFac.createActivityFromActivityFacilityId(previousAct.getType(), drawRandomFacility(facilities, matsimFacilities, facilityFac, trip.getOriginalOCoord(), trip.getOriginCT(), previousAct.getType(), previousAct.getType()+trip.getTripId()+"_O_"+i));
										a.setEndTime(dTime);
										plan.addActivity(a);

										plan.addLeg(popFac.createLeg(trip.getMode()));
										if(trip.getMode().equals("car") && ifCarRequired==false)ifCarRequired = true;
										plan.addActivity(popFac.createActivityFromActivityFacilityId(trip.getMotive(), drawRandomFacility(facilities, matsimFacilities, facilityFac,trip.getOriginalDCoord(),trip.getDestinationCT(),trip.getMotive(),trip.getMotive()+"_D_"+trip.getTripId()+"_"+i)));

									}else {
										Activity previousAct = ((Activity)plan.getPlanElements().get(plan.getPlanElements().size()-1));
										double dTime = trip.getDepartureTime()+random.nextGaussian(0,timeToSpread);
										if(dTime<0)dTime=0;
										dTime = trip.getDepartureTime();
										previousAct.setEndTime(dTime);
										plan.addLeg(popFac.createLeg(trip.getMode()));
										if(trip.getMode().equals("car") && ifCarRequired==false)ifCarRequired = true;
										plan.addActivity(popFac.createActivityFromActivityFacilityId(trip.getMotive(), drawRandomFacility(facilities, matsimFacilities, facilityFac,trip.getOriginalDCoord(),trip.getDestinationCT(),trip.getMotive(),trip.getMotive()+"_"+trip.getTripId()+"_D_"+i)));
									}
									int extraTrips = 0;
									if(i==0 && k==0 && (extraTrips = (int)Math.round((scale*trip.getTripExpFactror()-Math.round(scale*this.limitingFactor))-addionalMember))>0){// check for additional trips left
										for(int l = 0;l<extraTrips;l++) {
											trip.clonedTrip++;
											Person tripPerson = popFac.createPerson(Id.createPersonId(trip.getTripId().toString()+"_"+l));
											tripPerson.getAttributes().putAttribute("age", member.getAgeGroup());
											tripPerson.getAttributes().putAttribute("gender", member.getGender());
											tripPerson.getAttributes().putAttribute("occupation", member.getOccupation());
											if(member.getWorkCT()!=null)tripPerson.getAttributes().putAttribute("work census tract", member.getWorkCT());
											if(this.ct!=null)tripPerson.getAttributes().putAttribute("home census tract", this.ct);
											tripPerson.getAttributes().putAttribute("license", member.isIfHaveLicense());
											Plan tripPlan = popFac.createPlan();
											tripPerson.addPlan(tripPlan);
											String previousActType = ((Activity)plan.getPlanElements().get(plan.getPlanElements().size()-1)).getType();
											Activity previousAct =  popFac.createActivityFromActivityFacilityId(previousActType, drawRandomFacility(facilities, matsimFacilities, facilityFac,previousDCoord,previousCT, previousActType,trip.getMotive()+"_O_"+trip.getTripId()+"_"+l));
											double dTime = trip.getDepartureTime()+random.nextGaussian(0,timeToSpread);
											if(dTime<0)dTime=0;
											dTime = trip.getDepartureTime();
											previousAct.setEndTime(dTime);
											tripPlan.addActivity(previousAct);
											tripPlan.addLeg(popFac.createLeg(trip.getMode()));
											tripPlan.addActivity(popFac.createActivityFromActivityFacilityId(trip.getMotive(), drawRandomFacility(facilities, matsimFacilities, facilityFac,trip.getOriginalDCoord(),trip.getDestinationCT(),trip.getMotive(),trip.getMotive()+"_D_"+trip.getTripId()+"_"+l)));
											if(trip.getDay()!=null && !trip.getDay().equals(""))tripPlan.getAttributes().putAttribute("dayOfWeek", trip.getDay());
											population.addPerson(tripPerson);
											tripPerson.getAttributes().putAttribute("personTyp", "tripPerson");
											if(trip.getMode().equals("car")) {
												Vehicle vehicle = vFac.createVehicle(Id.create(tripPerson.getId().toString(),Vehicle.class), carType);
												vehicles.addVehicle(vehicle);
												Map<String,Id<Vehicle>> v = new HashMap<>();
												v.put("car",vehicle.getId());
												VehicleUtils.insertVehicleIdsIntoAttributes(tripPerson,v);
												PersonUtils.setCarAvail(tripPerson, "always");
											}else {
												PersonUtils.setCarAvail(tripPerson, "never");
											}
										}
									}

								}
								previousCT = trip.getDestinationCT();
								previousDCoord = trip.getOriginalDCoord();

							}else {
								shouldAdd = false;
								break;
							}
							j++;
							if(trip.clonedTrip>(int)Math.round((trip.getTripExpFactror()*scale))) {
								System.out.println();
							}
						}
						if(shouldAdd) {
							person.addPlan(plan);
							population.addPerson(person);
						}
						
					}
					if(k==0) {
						person.getAttributes().putAttribute("personTyp", "householdPerson");
					}else {
						person.getAttributes().putAttribute("personTyp", "soloPerson");
					}
					if(ifCarRequired==true && k==0 && (this.numOfCar-carCreated)>0) {
						Vehicle vehicle = vFac.createVehicle(Id.create(person.getId().toString(),Vehicle.class), carType);
						vehicles.addVehicle(vehicle);
						Map<String,Id<Vehicle>> v = new HashMap<>();
						v.put("car",vehicle.getId());
						VehicleUtils.insertVehicleIdsIntoAttributes(person,v);	
						hh.getVehicleIds().add(vehicle.getId());
						carCreated++;
					
						PersonUtils.setCarAvail(person, "always");
						
					}else if(ifCarRequired==true && k!=0) {
						Vehicle vehicle = vFac.createVehicle(Id.create(person.getId().toString(),Vehicle.class), carType);
						vehicles.addVehicle(vehicle);
						Map<String,Id<Vehicle>> v = new HashMap<>();
						v.put("car",vehicle.getId());
						VehicleUtils.insertVehicleIdsIntoAttributes(person,v);
						PersonUtils.setCarAvail(person, "always");
					}else if(k==0 && (ifCarRequired==true||carCreated>0||member.isIfHaveLicense())) {
						PersonUtils.setCarAvail(person, "sometimes");
					}else {
						PersonUtils.setCarAvail(person, "never");
					}
					if(member.clonedMember>(int)Math.round((member.getPersonExFac()*scale))||member.clonedMember-(int)Math.round((member.getPersonExFac()*scale))<1) {
						System.out.println();
					}
				}
				
			}

		}
		System.out.println();
	}
	
	
	
	public void setOriginalCoord(Coord originalCoord) {
		this.originalCoord = originalCoord;
	}



	public void setCt(Double ct) {
		this.ct = ct;
	}



	public static Id<ActivityFacility> generateFacilityId(String id, Coord coord,ActivityFacilities facilities,String actType,ActivityFacilitiesFactory facFac){
		ActivityFacility fac = facFac.createActivityFacility(Id.create(id, ActivityFacility.class), coord);
		fac.getActivityOptions().put(actType, facFac.createActivityOption(actType));
		facilities.addActivityFacility(fac);
		return fac.getId();
	}
	
	public static Id<ActivityFacility> drawRandomFacility(Map<String,Map<Double,Set<Id<ActivityFacility>>>> facilities, ActivityFacilities matsimFacilities, ActivityFacilitiesFactory facilityFactory, Coord originalCoord, Double originalCT, String originalActivity,String id) {
		
		if(!facilities.containsKey(originalActivity)) {
			throw new IllegalArgumentException("The activity type is not present in the facility to ctuid map!!!");
		}
		Id<ActivityFacility> out = null;
		if(facilities.get(originalActivity).get(originalCT)==null) {
			out = generateFacilityId(id, originalCoord, matsimFacilities, originalActivity, facilityFactory);
			matsimFacilities.getFacilities().get(out).getAttributes().putAttribute("CTUID", originalCT);
			facilities.get(originalActivity).put(originalCT, Set.of(out));
		}else {
			out = drawRandom(facilities.get(originalActivity).get(originalCT));
		}
		
		return out;
	}
	
	public static<T> T drawRandom(Collection<T> collection) {
		if(collection==null || collection.isEmpty()) {
			return null;
		}
		int num = (int)(Math.random() * collection.size());
	    for(T t: collection) {
	    	if (--num < 0) return t;
	    }
	    return null;
	}
	
}
