package population;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.FacilitiesWriter;


public class WeeklyPlanGenerator {
	
	public static void main(String[] args) {
		String singleDayPopulationLocation = "data\\population_with_locations.xml.gz";
		String singleDayPopulationLocation_linkId = "data\\population_with_locations_linkId.xml.gz";
		String weeklyPopulationLocation = "data\\population_with_locations_weekly.xml.gz";
		String weeklyPopulationLocation_scaled = "data\\population_with_locations_weekly_scaled.xml";
		Network net = NetworkUtils.readNetwork("data\\osm\\newOSM\\osmMultimodal.xml");
		new NetworkWriter(net).write("data\\osm\\newOSM\\osmMultimodal_fixedNode.xml");
		Config config = ConfigUtils.createConfig();
		config.facilities().setInputFile("data\\facilities.xml.gz");
		ActivityFacilities facs = ScenarioUtils.loadScenario(config).getActivityFacilities();
		Set<Id<Link>> problemLinks = new HashSet<>();
		net.getLinks().values().stream().forEach(l->{
			if(!l.getAllowedModes().contains("car")||l.getId().toString().contains("pt")) {
				problemLinks.add(l.getId());
			}
		});
		for(Node n:new HashSet<>(net.getNodes().values())) {
			if(n.getInLinks().isEmpty() && n.getOutLinks().isEmpty()) {
				net.removeNode(n.getId());
			}
		}
		assignLinksToFacilities(facs,net,problemLinks);
		new FacilitiesWriter(facs).writeV1("data\\facilities_loc.xml.gz");
		Population oldPop = PopulationUtils.readPopulation(singleDayPopulationLocation);
		Population newPop = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		PopulationFactory popFac = newPop.getFactory();
		
		oldPop.getPersons().values().forEach(p->{
			//Here I want to stitch the daily plan of people to create a weekly plan. 
			Person person = popFac.createPerson(p.getId());
			for(Entry<String, Object> a:p.getAttributes().getAsMap().entrySet()) {
				
				person.getAttributes().putAttribute(a.getKey(), a.getValue());
			}
			newPop.addPerson(person);
			Plan plan = popFac.createPlan();
			person.addPlan(plan);
			person.setSelectedPlan(plan);
			boolean ifAct = true;
			Activity lastAct = null;
			Leg lastLeg = null;
			boolean skipLeg = false;
			for(int day = 0;day<7;day++) {
				int i = 0;
				for(PlanElement pe:p.getSelectedPlan().getPlanElements()) {
					if(pe instanceof Activity) {
						Activity act = (Activity)pe;
						OptionalTime startTime = OptionalTime.undefined();
						OptionalTime endTime = OptionalTime.undefined();
						if(act.getStartTime().isDefined()) startTime = OptionalTime.defined(act.getStartTime().seconds()+24*3600*day);
						if(act.getEndTime().isDefined()) endTime = OptionalTime.defined(act.getEndTime().seconds()+24*3600*day);
						if(i==0 && day!=0) {//first activity for the next days, this is where stiching happens.
							if(ifSameAct(lastAct,act)) {//same activity, do not need to add the activity, just update the end time of the last activity.
								lastAct.setEndTime(endTime.seconds());
							}else {//not the same activity, need to insert a new leg here and also need to adjust the end time of the last activity.  
								double endTime1 = 24*day*3600+12*3600;
								if(lastAct.getStartTime().isDefined() && lastAct.getStartTime().seconds()>endTime1) {
									endTime1 = lastAct.getStartTime().seconds();
								}
								lastAct.setEndTime(endTime1);
								String mode = "car";
								if(p.getAttributes().getAttribute("carAvail").equals("never")) {
									mode = "pt";
								}
								Leg leg = popFac.createLeg(mode);
								plan.addLeg(leg);
								ifAct = true;
								
								Activity actNew = popFac.createActivityFromActivityFacilityId(act.getType(),act.getFacilityId());
								PopulationUtils.copyFromTo(act, actNew);
								if(startTime.isDefined())actNew.setStartTime(startTime.seconds());
								if(endTime.isDefined())actNew.setEndTime(endTime.seconds());
								if(act.getLinkId()!=null) {
									actNew.setLinkId(act.getLinkId());
								}else {
									actNew.setLinkId(facs.getFacilities().get(act.getFacilityId()).getLinkId());
									act.setLinkId(actNew.getLinkId());
								}
								if(!ifAct) {
									System.out.println("Wrong order of activity and leg. Expecting a leg not an activity!!!");
								}
								plan.addActivity(actNew);
								lastAct = actNew;
								ifAct = false;
								
							}
						}else if(day>4 && act.getType().equals("work")) {//weekends and work activity
							plan.getPlanElements().remove(lastLeg);
							ifAct = false;
							//skipLeg = true;
						}else if(day>4 && act.getType().equals("education")) {//weekends and school activity
							plan.getPlanElements().remove(lastLeg);
							ifAct = false;
							//skipLeg = true;
						}else {//normal case.
							Activity actNew = popFac.createActivityFromActivityFacilityId(act.getType(),act.getFacilityId());
							PopulationUtils.copyFromTo(act, actNew);
							if(startTime.isDefined())actNew.setStartTime(startTime.seconds());
							if(endTime.isDefined())actNew.setEndTime(endTime.seconds());
							if(act.getLinkId()!=null) {
								actNew.setLinkId(act.getLinkId());
							}else {
								actNew.setLinkId(facs.getFacilities().get(act.getFacilityId()).getLinkId());
								act.setLinkId(actNew.getLinkId());
							}
							if(!ifAct) {
								System.out.println("Wrong order of activity and leg. Expecting a leg not an activity!!!");
							}
							plan.addActivity(actNew);
							lastAct = actNew;
							ifAct = false;
						}
					}else if(pe instanceof Leg) {
						if(!skipLeg) {
							Leg oldLeg = (Leg)pe;
							Leg leg = popFac.createLeg(oldLeg.getMode());
							PopulationUtils.copyFromTo(oldLeg, leg);
							leg.setDepartureTime(lastAct.getEndTime().seconds());
							leg.setTravelTimeUndefined();
							if(ifAct) {
								System.out.println("Wrong order of activity and leg. Expecting an activity not a leg!!!");
							}
							plan.addLeg(leg);
							lastLeg = leg;
							ifAct = true;
						}
					}
					i++;
				}
			}
			
		});
		
		if(!hasLinkIds(newPop)) {
			System.out.println("activity does not have link id!!!");
		}
		if(!hasLinkIds(oldPop)) {
			System.out.println("activity does not have link id!!!");
		}
		
		new PopulationWriter(newPop).write(weeklyPopulationLocation);
		new PopulationWriter(oldPop).write(singleDayPopulationLocation_linkId);
		new PopulationWriter(samplePop(newPop,0.01)).write(weeklyPopulationLocation_scaled);
		
		new PopulationReader(ScenarioUtils.createScenario(ConfigUtils.createConfig())).readFile(weeklyPopulationLocation);
		
		
	}
	public static Population samplePop(Population pop,double scale) {
		for(Id<Person>pId:new HashSet<>(pop.getPersons().keySet())) {
			if(Math.random()>scale)pop.getPersons().remove(pId);
		}
		return pop;
	}
	public static boolean hasLinkIds(Population pop) {
		for(Person p:pop.getPersons().values()) {
			for(PlanElement pe:p.getSelectedPlan().getPlanElements()) {
				if(pe instanceof Activity) {
					Activity act = (Activity)pe;
					if(act.getLinkId()==null) {
						return false;
					}
				}
			}
		}
		return true;
	}
	
	private static boolean ifSameAct(Activity act1, Activity act2) {
		if(act1.getType()!=act2.getType()) {
			return false;
		}
		if(act1.getFacilityId()!=act2.getFacilityId()) {
			return false;
		}
		if((act1.getCoord()!=null && act2.getCoord()!=null) && 
				(Double.compare(act1.getCoord().getX(), act2.getCoord().getX())!=0 || Double.compare(act1.getCoord().getY(), act2.getCoord().getY())!=0)) {
			return false;
		}
		return true;
	}
	
	public static void assignLinksToFacilities(ActivityFacilities fac,Network net, Set<Id<Link>> blackListedLinks) {
		//new NetworkCleaner().run(net);
		  new NetworkWriter(net).write("temp.xml");
		  Network neto = NetworkUtils.readNetwork("temp.xml");
		
		  blackListedLinks.forEach(l->neto.removeLink(l));
		  new NetworkCleaner().run(neto);
		  for(ActivityFacility f:fac.getFacilities().values()){
//			  Link link1 = NetworkUtils.getNearestRightEntryLink(neto, f.getCoord());
			  Link link = NetworkUtils.getNearestLink(neto, f.getCoord());
			  FacilitiesUtils.setLinkID(f, link.getId());
		  }
	  }
	

}
