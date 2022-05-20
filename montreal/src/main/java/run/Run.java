package run;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
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
    config.controler().setLastIteration(this.maxIterations);
    addStrategy(config, "SubtourModeChoice", null, 0.1D, 0 * this.maxIterations);
    addStrategy(config, "ReRoute", null, 0.5D, 0 * this.maxIterations);
    addStrategy(config, "ChangeExpBeta", null, 0.85D, this.maxIterations);
    config.controler().setOutputDirectory(this.output);
    config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
    config.qsim().setFlowCapFactor(this.scale.doubleValue() * 1.2D);
    config.qsim().setStorageCapFactor(this.scale.doubleValue() * 1.4D);
    
    ParamReader pReader = new ParamReader(paramFile);
    config = pReader.SetParamToConfig(config, pReader.getInitialParam());
    
    Scenario scenario = ScenarioUtils.loadScenario(config);
   
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
    		ActivityParams param = new ActivityParams();
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
}
