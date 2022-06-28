package calibration;

import org.matsim.core.controler.AbstractModule;

import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;

import dynamicTransitRouter.fareCalculators.FareCalculator;
import dynamicTransitRouter.fareCalculators.UniformFareCalculator;
import ust.hk.praisehk.metamodelcalibration.analyticalModel.AnalyticalModel;
import ust.hk.praisehk.metamodelcalibration.measurements.Measurements;

public class CalibrationModule extends AbstractModule{
	private Measurements countData;
	private boolean generateOD;
	private String fileLoc;
	private AnalyticalModel model;

	
	public CalibrationModule(AnalyticalModel model,Measurements countData, boolean generateOD,String fileLoc) {
		this.countData = countData;
		this.generateOD = generateOD;
		this.fileLoc = fileLoc;
		this.model = model;
	}
	
	@Override
	public void install() {
		PCUCounter lc=new PCUCounter(countData);
		bind(Measurements.class).annotatedWith(Names.named("Output Measurements")).toInstance(this.countData);
		bind(AnalyticalModel.class).toInstance(this.model);
		bind(String.class).annotatedWith(Names.named("fileLoc")).toInstance(this.fileLoc);
		this.addControlerListenerBinding().to(CalibrationControlerListener.class).asEagerSingleton();
		this.addEventHandlerBinding().toInstance(lc);
		bind(PCUCounter.class).toInstance(lc);
		bind(boolean.class).annotatedWith(Names.named("generateRoutesAndOD")).toInstance(this.generateOD);
		MapBinder<String,FareCalculator> fareCalc = MapBinder.newMapBinder(binder(), String.class,
				FareCalculator.class);
		fareCalc.addBinding("bus").toInstance(new UniformFareCalculator(0.));
		fareCalc.addBinding("subway").toInstance(new UniformFareCalculator(0.));
		
	}
	
//	public static void main(String[] args) {
//		Config config = ConfigUtils.createConfig();
//		config.transit().setTransitScheduleFile("src\\main\\resources\\montreal_transit_schedules.xml.gz");
//		TransitSchedule ts = ScenarioUtils.loadScenario(config).getTransitSchedule();
//		Set<String> modes = new HashSet<>();
//		ts.getTransitLines().values().forEach(tl->{
//			tl.getRoutes().values().forEach(tr->{
//				modes.add(tr.getTransportMode());
//			});
//		});
//		System.out.println(modes);
//	}
}

