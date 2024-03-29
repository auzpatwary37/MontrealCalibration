package run;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.AccessEgressType;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.lanes.Lane;
import org.matsim.lanes.Lanes;
import org.matsim.lanes.LanesFactory;
import org.matsim.lanes.LanesToLinkAssignment;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.utils.TransitScheduleValidator;
import org.matsim.pt.utils.TransitScheduleValidator.ValidationResult;
import org.matsim.vehicles.VehicleCapacity;

import picocli.CommandLine;
import picocli.CommandLine.Option;

public final class Run implements Callable<Integer> {
	
  public static final String COLOR = "@|bold,fg(81) ";
   
  private static final Logger log = LogManager.getLogger(Run.class);
  
  @Option(names = {"--config"}, description = {"Optional Path to config file to load."}, defaultValue = "config.xml")
  private String config;
  
  @Option(names = {"--plan"}, description = {"Optional Path to plan file to load."}, defaultValue = "prepared_population.xml.gz")
  private String planFile;
  
  @Option(names = {"--network"}, description = {"Optional Path to network file to load."}, defaultValue = "montreal_network.xml.gz")
  private String networkFileLoc;
  
  @Option(names = {"--ts"}, description = {"Optional Path to transit schedule file to load."}, defaultValue = "montreal_transit_schedules.xml.gz")
  private String tsFileLoc;
  
  @Option(names = {"--tv"}, description = {"Optional Path to transit vehicle file to load."}, defaultValue = "montreal_transit_vehicles.xml.gz")
  private String tvFileLoc;
  
  @Option(names = {"--facilities"}, description = {"Optional Path to facilities file to load."}, defaultValue = "montreal_facilities.xml.gz")
  private String facilitiesFileLoc;
  
  @Option(names = {"--iterations"}, description = {"Maximum number of iteration to simulate."}, defaultValue = "250") 
  private int maxIterations;
  
  @Option(names = {"--household"}, description = {"Optional Path to household file to load."}, defaultValue = "montreal_households.xml.gz")
  private String householdFileLoc;
  
  @Option(names = {"--scale"}, description = {"Scale of simulation"}, defaultValue = "0.05")
  private Double scale;
  
  @Option(names = {"--output"}, description = {"Result output directory"}, defaultValue = "output/")
  private String output;
  
  @Option(names = {"--paramfile"}, description = {"Parameter file location"}, defaultValue = "paramReaderTrial1.csv")
  private String paramFile;
  
  @Option(names = {"--thread"}, description = {"Number of thread"}, defaultValue = "40")
  private int thread;
  
  @Option(names = {"--lanes"}, description = {"Location of the lane definition file"}, defaultValue = "null")
  private String laneFile;
  
  @Option(names = {"--vehicles"}, description = {"Location of the auto vehicle file"}, defaultValue = "null")
  private String vehiclesFile;
  
  @Option(names = {"--clearplan"}, description = {"If clear the population of routes and non selected plans"}, defaultValue = "false")
  private String ifClear;
  
  public static void main(String[] args) {
    (new CommandLine(new Run()))
      .setStopAtUnmatched(false)
      .setUnmatchedOptionsArePositionalParams(true)
      .execute(args);
  }
  
  public Integer call() throws Exception {
    Config config = ConfigUtils.createConfig();
    ConfigUtils.loadConfig(config, this.config);
    config.removeModule("ev");
    config.plans().setInputFile(this.planFile);
    config.households().setInputFile(this.householdFileLoc);
    config.facilities().setInputFile(this.facilitiesFileLoc);
    config.network().setInputFile(this.networkFileLoc);
    config.transit().setTransitScheduleFile(this.tsFileLoc);
    config.transit().setVehiclesFile(this.tvFileLoc);
    config.global().setNumberOfThreads(thread);
    config.qsim().setNumberOfThreads(thread);
    if(!laneFile.equals("null"))config.network().setLaneDefinitionsFile(laneFile);
    config.vehicles().setVehiclesFile(this.vehiclesFile);
    config.controler().setLastIteration(this.maxIterations);
    addStrategy(config, "SubtourModeChoice", null, 0.05D, 0 * this.maxIterations);
    addStrategy(config, "ReRoute", null, 0.1D, 0 * this.maxIterations);
    addStrategy(config, "ChangeExpBeta", null, 0.85D, this.maxIterations);
    config.controler().setOutputDirectory(this.output);
    config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
    config.controler().setLinkToLinkRoutingEnabled(true);
    config.travelTimeCalculator().setCalculateLinkToLinkTravelTimes(true);
    config.controler().setRoutingAlgorithmType(RoutingAlgorithmType.SpeedyALT);
    //config.transit().setRoutingAlgorithmType(TransitRoutingAlgorithmType.DijkstraBased);
    config.travelTimeCalculator().setSeparateModes(false);
    config.travelTimeCalculator().setCalculateLinkTravelTimes(true);
    //config.getModules().remove("swissRailRaptor");
    config.qsim().setRemoveStuckVehicles(false);
    config.qsim().setUseLanes(true);
    config.qsim().setStuckTime(4*3600);
    //config.plansCalcRoute().setAccessEgressType(AccessEgressType.walkConstantTimeToLink);
    config.plansCalcRoute().setAccessEgressType(AccessEgressType.accessEgressModeToLink);
    ParamReader pReader = new ParamReader(paramFile);
    config = pReader.SetParamToConfig(config, pReader.getInitialParam());
    config.qsim().setFlowCapFactor(this.scale.doubleValue() * 1.2D);
    config.qsim().setStorageCapFactor(this.scale.doubleValue() * 1.4D);
    config.removeModule("swissRailRaptor");
    Scenario scenario = ScenarioUtils.loadScenario(config);
    //new NetworkCleaner().run( scenario.getNetwork());
    checkPtConsistency(scenario.getNetwork(),scenario.getTransitSchedule(), scenario.getLanes());
    Set<Id<Link>> problemLinks = runLaneBasedNetworkCleaner(scenario.getNetwork(),scenario.getLanes(), true);
    checkNetworkCapacityLengthAndLanes(scenario.getNetwork(),scenario.getLanes());
//    problemLinks.add(Id.createLinkId("34221"));//203299
//    problemLinks.add(Id.createLinkId("95049"));
     scenario.getTransitVehicles().getVehicleTypes().values().stream().forEach(vt -> {
    	vt.setPcuEquivalents(vt.getPcuEquivalents()*this.scale.doubleValue());
        VehicleCapacity vc = vt.getCapacity();
        vc.setSeats(Integer.valueOf((int)Math.ceil(vc.getSeats().intValue() * this.scale.doubleValue())));
        vc.setStandingRoom(Integer.valueOf((int)Math.ceil(vc.getStandingRoom().intValue() * this.scale.doubleValue())));
    });
    
    Set<Id<Person>> personIds = new HashSet<Id<Person>>(scenario.getPopulation().getPersons().keySet());
    personIds.stream().forEach(p->{
    	if(scenario.getPopulation().getPersons().get(p).getSelectedPlan().getPlanElements().stream().filter(pe -> pe instanceof Leg).findAny().isEmpty()) {
    		scenario.getPopulation().getPersons().remove(p);
    	}
    });
    
    Map<String,Double> actDuration = new HashMap<>();
    Map<String,Integer> actNum = new HashMap<>();
    Set<String> actList = new HashSet<>();
    scenario.getPopulation().getPersons().values().stream().forEach(p->{
    	p.getSelectedPlan().getPlanElements().stream().filter(f-> f instanceof Activity).forEach(pe->{
    		Activity act = ((Activity)pe);
    		Double actDur = 0.;
    		if(act.getEndTime().isDefined() && act.getStartTime().isDefined()) {
    			actDur = act.getEndTime().seconds() - act.getStartTime().seconds();
    		}
    		double ad = actDur;
    		if(actDur != 0.) {
    			actDuration.compute(act.getType(), (k,v)->v==null?ad:ad+v);
    			actNum.compute(act.getType(), (k,v)->v==null?1:v+1);
    		}else {
    			
    		}
    		
    	});
 
    });
    
  
    
    for(Entry<String, Double> a:actDuration.entrySet()){
    	a.setValue(a.getValue()/actNum.get(a.getKey()));
    	if(config.planCalcScore().getActivityParams(a.getKey())!=null) {
    		config.planCalcScore().getActivityParams(a.getKey()).setTypicalDuration(a.getValue());
    		config.planCalcScore().getActivityParams(a.getKey()).setMinimalDuration(a.getValue()*.25);
    		config.planCalcScore().getActivityParams(a.getKey()).setScoringThisActivityAtAll(true);

    	}else {
    		ActivityParams param = new ActivityParams(a.getKey());
    		param.setTypicalDuration(a.getValue());
    		param.setMinimalDuration(a.getValue()*.25);
    		param.setScoringThisActivityAtAll(true);
    		config.planCalcScore().addActivityParams(param);
    	}
    }
    for(String actType:actList) {
    	if(config.planCalcScore().getActivityParams(actType)==null) {
    		ActivityParams param = new ActivityParams();
    		param.setTypicalDuration(8*3600);
    		param.setMinimalDuration(8*3600*.25);
    		param.setScoringThisActivityAtAll(true);
    		config.planCalcScore().addActivityParams(param);
    		param.setScoringThisActivityAtAll(true);
    		System.out.println("No start and end time was found for activity = "+actType+ " in the base population!! Inserting 8 hour as the typical duration.");
    	}
    }
    if(ifClear.equals("true"))clearPopulationFromRouteAndNetwork(scenario.getPopulation(),scenario.getNetwork(),scenario.getActivityFacilities());
    if(ifClear.equals("true"))assignLinksToFacilities(scenario.getActivityFacilities(),scenario.getNetwork(), problemLinks);
    ValidationResult r = TransitScheduleValidator.validateAll(scenario.getTransitSchedule(), scenario.getNetwork());
	System.out.println("transit is valid? "+ r.isValid());
    Controler controler = new Controler(scenario);
    controler.run();
    return Integer.valueOf(0);
  }
  
  public static void addStrategy(Config config, String strategy, String subpopulationName, double weight, int disableAfter) {
    if (weight <= 0.0D || disableAfter < 0)
      throw new IllegalArgumentException("The parameters can't be less than or equal to 0!"); 
    StrategyConfigGroup.StrategySettings strategySettings = new StrategyConfigGroup.StrategySettings();
    strategySettings.setStrategyName(strategy);
    strategySettings.setSubpopulation(subpopulationName);
    strategySettings.setWeight(weight);
    if (disableAfter > 0)
      strategySettings.setDisableAfter(disableAfter); 
    config.strategy().addStrategySettings(strategySettings);
  }
  
  public static void clearPopulationFromRouteAndNetwork(Population pop, Network net, ActivityFacilities fac) {
	  for(Person p:pop.getPersons().values()){
		  Plan plan = p.getSelectedPlan();
		  for(Plan pl:new ArrayList<>(p.getPlans())) {
			  if(!pl.equals(plan))p.getPlans().remove(pl);
		  }
		  boolean ptTrip = false;
		  List<PlanElement> ptElements = new ArrayList<>();
		  int startingIndex = 0;
		  int legNo = 0;
		  int actNo = 0;
		  int i = 0;
		  List<PlanElement> untouched = new ArrayList<>(p.getSelectedPlan().getPlanElements());
		  for(PlanElement pe:untouched) {
			  
			  if(pe instanceof Activity) {
				((Activity)pe).setLinkId(null);
				if(((Activity)pe).getType().equals("pt interaction")) {
					if(ptTrip == false) {
						legNo--;
						ptTrip = true;
						startingIndex = actNo+legNo;
						ptElements.add(untouched.get(i-1));
						ptElements.add(pe);
					}else {
						ptElements.add(pe);
					}
					
				}else {
					if(ptTrip == true) {
						ptTrip = false;
						p.getSelectedPlan().getPlanElements().removeAll(ptElements);
						p.getSelectedPlan().getPlanElements().add(startingIndex, PopulationUtils.createLeg("pt"));
						legNo++;
						actNo++;
					}else {
						actNo++;
					}
				}
			  }else {
				  if(ptTrip == true) {
					  ptElements.add(pe);
				  }else {
					  ((Leg)pe).setRoute(null);
					  legNo++;
				  }
			  }
			  i++;
		  }
//		  if(untouched.size()!=p.getSelectedPlan().getPlanElements().size()) {
//			  System.out.println();
//		  }
		 
	  }
	 
	    fac.getFacilities().values().forEach(f->{
	    	Id<Link> lId =  NetworkUtils.getNearestRightEntryLink(net, f.getCoord()).getId();
	    	FacilitiesUtils.setLinkID(f, lId);
	    });
  }
  
  public static void checkPtConsistency(Network net,TransitSchedule ts, Lanes lanes) {
	  ValidationResult res = TransitScheduleValidator.validateAll(ts, net);
	  res.getErrors().forEach(e->System.out.println(e));
	  res.getWarnings().forEach(w->System.out.println(w));
	  //res.getIssues().forEach(i->System.out.println(i));
	  ts.getTransitLines().values().forEach(tl->{
		  tl.getRoutes().values().forEach(tr->{
			  List<Id<Link>> links = new ArrayList<>();
			  
			  links.add(tr.getRoute().getStartLinkId());
			  links.addAll(tr.getRoute().getLinkIds());
			  links.add(tr.getRoute().getEndLinkId());
			  
			  for(int i = 1;i<links.size();i++) {
				  if(net.getLinks().get(links.get(i-1)).getToNode().getId()!=net.getLinks().get(links.get(i)).getFromNode().getId()) {
					  throw new IllegalArgumentException("Inconsistent route!!!");
				  }
				  if(lanes.getLanesToLinkAssignments().get(links.get(i-1))!=null) {
					  LanesToLinkAssignment l2l  = lanes.getLanesToLinkAssignments().get(links.get(i-1));
					  boolean hasLane = false;
					  for(Lane lane:l2l.getLanes().values()) {
						  if(lane.getToLinkIds().contains(links.get(i))) {
							  hasLane = true;
							  break;
						  }
					  }
					  if(!hasLane) {
						  System.out.println("Route is not connected.");
					  }
				  }
			  }
			  int lastInd = -1;
			  for(TransitRouteStop st:tr.getStops()){
				 int ind = 1+ lastInd + links.subList(lastInd+1, links.size()).indexOf(st.getStopFacility().getLinkId());
				 
				 if(ind<0 || ind<lastInd) {
					 System.out.println("Route is not consistant with stop order.");
				 }else {
					 lastInd = ind;
				 }
			  }
			  
		  });
	  });
  }
  
  
  
  public static void assignLinksToFacilities(ActivityFacilities fac,Network net) {
	  new NetworkWriter(net).write("temp.xml");
	  Network neto = NetworkUtils.readNetwork("temp.xml");
	  new NetworkCleaner().run(neto);
	  new HashSet<>(neto.getLinks().keySet()).stream().forEach(l->{
		  if(l.toString().contains("pt"))neto.removeLink(l);
		  if(!neto.getLinks().get(l).getAllowedModes().contains("car"))neto.removeLink(l);
	  });
	  fac.getFacilities().values().forEach(f->{
		  Link link = NetworkUtils.getNearestLink(neto, f.getCoord());
		  if(link==null) {
			  System.out.println("Debug!!!");
		  }
		  FacilitiesUtils.setLinkID(f, link.getId());
	  });
  }
  
  public static void assignLinksToFacilities(ActivityFacilities fac,Network net, Set<Id<Link>> blackListedLinks) {
	  new NetworkWriter(net).write("temp.xml");
	  Network neto = NetworkUtils.readNetwork("temp.xml");
	  new NetworkCleaner().run(neto);
	  blackListedLinks.forEach(l->neto.removeLink(l));
	  fac.getFacilities().values().forEach(f->{
		  Link link = NetworkUtils.getNearestLink(neto, f.getCoord());
		  FacilitiesUtils.setLinkID(f, link.getId());
	  });
  }
  
  public static Set<Id<Link>> runLaneBasedNetworkCleaner(Network net, Lanes lanes, boolean ifAddelseDelete) {
	  LanesFactory lFac = lanes.getFactory();
	  int problematicLink = 0;
	  int problematicLinkDueToLane = 0;
	  int addedLanes = 0; 
	  int deletedLinks = 0;
	  Set<Id<Link>> problemLinks = new HashSet<>();
	  
	  for(Link l:net.getLinks().values()){
		  // First check the forward connection, i.e., people can get out
		  LanesToLinkAssignment l2l = lanes.getLanesToLinkAssignments().get(l.getId());
		  if(!l.getId().toString().contains("pt") && l.getToNode().getOutLinks().isEmpty()) {
			  problematicLink++;
			  problemLinks.add(l.getId());
		  }else if(!l.getId().toString().contains("pt") && !l.getToNode().getOutLinks().isEmpty() && l2l!=null) {
			  if(l2l.getLanes().size()==0 || ifOnlyConnectedToSelf(l, l2l, net)) {
				  problematicLinkDueToLane++;
				  for(Link ol:l.getToNode().getOutLinks().values()){
					  Lane lane = lFac.createLane(Id.create(l.getId()+"_"+ol.getId(),Lane.class));
					  lane.addToLinkId(ol.getId());
					  lane.setCapacityVehiclesPerHour(1800);
					  lane.setNumberOfRepresentedLanes(1);
					  lane.setStartsAtMeterFromLinkEnd(50);
					  l2l.addLane(lane);
					  addedLanes++;
					
				  }
			  }else if(l2l.getLanes().size()==1) {
				  l2l.getLanes().values().forEach(lane->{
					
				  });
			  }
		  }
		  
		  //Lets check for backward connection
		  Map<Id<Link>, LanesToLinkAssignment> incomingL2l = new HashMap<>();
		  if(!l.getId().toString().contains("pt") && l.getFromNode().getInLinks().size()==0) {
			  problematicLink++;
			  problemLinks.add(l.getId());
		  }
		  boolean hasLane = false;
		  for(Link il:l.getFromNode().getInLinks().values()) {
			  if(ifInverseLink(il,l))continue;
			  LanesToLinkAssignment l2lin = lanes.getLanesToLinkAssignments().get(il.getId());
			  if(l.getId().toString().contains("pt") || l2lin==null) {
				  hasLane = true;
				  break;
			  }
			  for(Lane lane:l2lin.getLanes().values()) {
				  if(lane.getToLinkIds().contains(l.getId())) {
					  hasLane = true;
					  break;
				  } 
			  }
			  incomingL2l.put(il.getId(), l2lin);
			  if(hasLane) break;
			  
		  }
		  if(!hasLane) {
			  problematicLinkDueToLane++;
			  for(LanesToLinkAssignment ll2in:incomingL2l.values()) {
				  Lane lane = lFac.createLane(Id.create(ll2in.getLinkId()+"_"+l.getId(),Lane.class));
				  lane.setCapacityVehiclesPerHour(1800);
				  lane.addToLinkId(l.getId());
				  lane.setNumberOfRepresentedLanes(1);
				  lane.setStartsAtMeterFromLinkEnd(50);
				  ll2in.addLane(lane);
				  addedLanes++;
				  
			  }
		  }
	  }
	  
	  System.out.println("Problematic links = " + problematicLink );
	  System.out.println("Problematic links due to lanes = " + problematicLinkDueToLane );
	  System.out.println("Added lanes = " + addedLanes );
	  return problemLinks;
  }
  
  public static boolean ifInverseLink(Id<Link> link1, Id<Link> link2, Network net) {
	  if(Math.abs(getAngle(net.getLinks().get(link1), net.getLinks().get(link2)))==180)return true;
	  else return false;
  }
  
  public static boolean ifInverseLink(Link link1, Link link2) {
	  if(Math.abs(getAngle(link1,link2))==180)return true;
	  else return false;
  }
  public static double getAngle(Link l1, Link l2) {
		
		Tuple<Double,Double> vec1 = new Tuple<>(l1.getToNode().getCoord().getX()-l1.getFromNode().getCoord().getX(),l1.getToNode().getCoord().getY()-l1.getFromNode().getCoord().getY());
		Tuple<Double,Double> vec2 = new Tuple<>(l2.getToNode().getCoord().getX()-l2.getFromNode().getCoord().getX(),l2.getToNode().getCoord().getY()-l2.getFromNode().getCoord().getY());
		
		double dot = vec1.getFirst()*vec2.getFirst()+vec1.getSecond()*vec2.getSecond();
		
		double det = vec1.getFirst()*vec2.getSecond() - vec1.getSecond()*vec2.getFirst();    
		double angle = Math.toDegrees(Math.atan2(det, dot));  
		
		return angle;
	}

  
  public static boolean ifOnlyConnectedToSelf(Link link,LanesToLinkAssignment l2l, Network net) {
	  Set<Id<Link>> outLinks = new HashSet<>();
	  l2l.getLanes().values().forEach(lane->{
		  outLinks.addAll(lane.getToLinkIds());
	  });
	  if(outLinks.size()==1 && ifInverseLink(link.getId(),outLinks.stream().findFirst().get(),net))return true;
	  return false;
  }
  
  public static void checkNetworkCapacityLengthAndLanes(Network net, Lanes lanes) {
	  net.getLinks().values().forEach(l->{
		  if(l.getCapacity()<300) {
			  l.setCapacity(300);
			  
		  }
		  if(l.getNumberOfLanes()==0) {
			  l.setNumberOfLanes(1);
		  }
		  if(l.getFreespeed()<8.33) {
			  l.setFreespeed(8.33);
			  
		  }
		  
		  if(l.getLength()==0) {
			  l.setLength(10);
		  }
		  LanesToLinkAssignment l2l = lanes.getLanesToLinkAssignments().get(l.getId());
		  if(l2l!=null) {
			  l2l.getLanes().values().forEach(lane->lane.setStartsAtMeterFromLinkEnd(l.getLength()));
		  }
	  });
	  
  }
  
}
