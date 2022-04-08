package run;

import java.util.concurrent.Callable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

public final class Run implements Callable<Integer> {
  public static final String COLOR = "@|bold,fg(81) ";
   
  private static final Logger log = LogManager.getLogger(Run.class);
  
  @Option(names = {"--config"}, description = {"Optional Path to config file to load."}, defaultValue = "config.xml")
  private String config;
  
  @Option(names = {"--plan"}, description = {"Optional Path to plan file to load."}, defaultValue = "prepared_plan.xml.gz")
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
  
  @Option(names = {"--scale"}, description = {"Scale of simulation"}, defaultValue = "0.5")
  private Double scale;
  
  @Option(names = {"--output"}, description = {"Result output directory"}, defaultValue = "output/")
  private String output;
  
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
    config.global().setNumberOfThreads(11);
    config.qsim().setNumberOfThreads(10);
    config.controler().setLastIteration(this.maxIterations);
    addStrategy(config, "SubtourModeChoice", null, 0.1D, 0 * this.maxIterations);
    addStrategy(config, "ReRoute", null, 0.5D, 0 * this.maxIterations);
    addStrategy(config, "ChangeExpBeta", null, 0.85D, this.maxIterations);
    config.controler().setOutputDirectory(this.output);
    config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
    config.qsim().setFlowCapFactor(this.scale.doubleValue() * 1.2D);
    config.qsim().setStorageCapFactor(this.scale.doubleValue() * 1.4D);
    Scenario scenario = ScenarioUtils.loadScenario(config);
    scenario.getTransitVehicles().getVehicleTypes().values().stream().forEach(vt -> {
        
        });
    scenario.getTransitVehicles().getVehicleTypes().values().stream().forEach(vt -> {
    	vt.setPcuEquivalents(vt.getPcuEquivalents()*this.scale.doubleValue());
        VehicleCapacity vc = vt.getCapacity();
        vc.setSeats(Integer.valueOf((int)Math.ceil(vc.getSeats().intValue() * this.scale.doubleValue())));
        vc.setStandingRoom(Integer.valueOf((int)Math.ceil(vc.getStandingRoom().intValue() * this.scale.doubleValue())));
    });
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
