package calibration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleCapacity;

import picocli.CommandLine.Option;
import ust.hk.praisehk.metamodelcalibration.analyticalModel.AnalyticalModel;
import ust.hk.praisehk.metamodelcalibration.calibrator.ParamReader;
import ust.hk.praisehk.metamodelcalibration.measurements.Measurements;

public class SimRunMontreal {

	private Config config;
	private ParamReader pReader;
	private Measurements calibrationMeasurements;
	private String fileLoc;
	
	@Option(names = {"--plan"}, description = {"Optional Path to plan file to load."}, defaultValue = "prepared_population.xml.gz")
	private String planFile;

	@Option(names = {"--network"}, description = {"Optional Path to network file to load."}, defaultValue = "montreal_network.xml.gz")
	private String networkFileLoc;

	@Option(names = {"--ts"}, description = {"Optional Path to transit schedule file to load."}, defaultValue = "montreal_transit_schedules.xml.gz")
	private String tsFileLoc;

	@Option(names = {"--tv"}, description = {"Optional Path to transit vehicle file to load."}, defaultValue = "montreal_transit_vehicles.xml.gz")
	private String tvFileLoc;
	
	@Option(names = {"--v"}, description = {"Optional Path to vehicle file to load."}, defaultValue = "vehicle.xml")
	private String vehicleFileLoc;

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

	@Option(names = {"--thread"}, description = {"Number of thread"}, defaultValue = "12")
	private int thread;

	/**
	 * The measurements is cloned the output measurements will be the cloned the measurements 
	 * @param config
	 * @param pReader
	 * @param calibrationMeasurements
	 */
	public SimRunMontreal(Config config, ParamReader pReader,Measurements calibrationMeasurements,String fileLoc) {
		this.config = config;
		this.pReader = pReader;
		this.calibrationMeasurements = calibrationMeasurements.clone();
		this.fileLoc = fileLoc;
		
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

	public Measurements run(AnalyticalModel sue, LinkedHashMap<String, Double> params, boolean generateOd,String counterNo) {
		config.removeModule("ev");
		config.plans().setInputFile(this.planFile);
		config.households().setInputFile(this.householdFileLoc);
		config.facilities().setInputFile(this.facilitiesFileLoc);
		config.network().setInputFile(this.networkFileLoc);
		config.transit().setTransitScheduleFile(this.tsFileLoc);
		config.transit().setVehiclesFile(this.tvFileLoc);
		config.vehicles().setVehiclesFile(vehicleFileLoc);
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


		config = pReader.SetParamToConfig(config, params);
		//this.scale = config.qsim().getFlowCapFactor()/1.2;
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
		//AnalyticalModel model  = new CNLSUEModel(calibrationMeasurements.getTimeBean());
		controler.addOverridingModule(new CalibrationModule(sue, calibrationMeasurements, generateOd, this.fileLoc+counterNo));
		controler.run();
		return this.calibrationMeasurements;
	}


	public String getPlanFile() {
		return planFile;
	}


	public void setPlanFile(String planFile) {
		this.planFile = planFile;
	}


	public String getNetworkFileLoc() {
		return networkFileLoc;
	}


	public void setNetworkFileLoc(String networkFileLoc) {
		this.networkFileLoc = networkFileLoc;
	}


	public String getTsFileLoc() {
		return tsFileLoc;
	}


	public void setTsFileLoc(String tsFileLoc) {
		this.tsFileLoc = tsFileLoc;
	}


	public String getTvFileLoc() {
		return tvFileLoc;
	}


	public void setTvFileLoc(String tvFileLoc) {
		this.tvFileLoc = tvFileLoc;
	}


	public String getFacilitiesFileLoc() {
		return facilitiesFileLoc;
	}


	public void setFacilitiesFileLoc(String facilitiesFileLoc) {
		this.facilitiesFileLoc = facilitiesFileLoc;
	}


	public int getMaxIterations() {
		return maxIterations;
	}


	public void setMaxIterations(int maxIterations) {
		this.maxIterations = maxIterations;
	}


	public String getHouseholdFileLoc() {
		return householdFileLoc;
	}


	public void setHouseholdFileLoc(String householdFileLoc) {
		this.householdFileLoc = householdFileLoc;
	}


	public Double getScale() {
		return scale;
	}


	public void setScale(Double scale) {
		this.scale = scale;
	}


	public String getOutput() {
		return output;
	}


	public void setOutput(String output) {
		this.output = output;
	}


	public int getThread() {
		return thread;
	}


	public void setThread(int thread) {
		this.thread = thread;
	}


	public Config getConfig() {
		return config;
	}


	public ParamReader getpReader() {
		return pReader;
	}


	public Measurements getCalibrationMeasurements() {
		return calibrationMeasurements;
	}
	public String getVehicleFileLoc() {
		return vehicleFileLoc;
	}
	public void setVehicleFileLoc(String vehicleFileLoc) {
		this.vehicleFileLoc = vehicleFileLoc;
	}
	
}
