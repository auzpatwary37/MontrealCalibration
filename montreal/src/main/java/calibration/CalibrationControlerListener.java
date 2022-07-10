package calibration;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import dynamicTransitRouter.fareCalculators.FareCalculator;
import ust.hk.praisehk.metamodelcalibration.analyticalModel.AnalyticalModel;
import ust.hk.praisehk.metamodelcalibration.measurements.Measurement;
import ust.hk.praisehk.metamodelcalibration.measurements.Measurements;
import ust.hk.praisehk.metamodelcalibration.measurements.MeasurementsWriter;

public class CalibrationControlerListener implements StartupListener,BeforeMobsimListener, AfterMobsimListener,IterationEndsListener, ShutdownListener{
	private boolean generateOD=true;

	@Inject
	private AnalyticalModel SueAssignment;
	@Inject
	private @Named("Output Measurements") Measurements outputMeasurements;
	private String fileLoc;
	@Inject
	private PCUCounter pcuVolumeCounter;

	@Inject
	private OutputDirectoryHierarchy controlerIO;
	
	@Named("odNetwork") Network odNetwork;
	private int maxIter;
	private final Map<String, FareCalculator> farecalc;
	private int AverageCountOverNoOfIteration=5;
	private boolean shouldAverageOverIteration=false;
	private Map<Id<Measurement>, Map<String, Double>> counts=null;
	
	@Inject
	public CalibrationControlerListener(AnalyticalModel sueAssignment, 
			Map<String,FareCalculator> farecalc,@Named("fileLoc") String fileLoc,@Named("generateRoutesAndOD") boolean generateRoutesAndOD){
		this.SueAssignment=sueAssignment;
		this.farecalc=farecalc;

		this.fileLoc=fileLoc;
		this.generateOD=generateRoutesAndOD;

	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		this.maxIter=event.getServices().getConfig().controler().getLastIteration();
		}
	
	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		this.pcuVolumeCounter.reset(0);
		
	}
	
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		if(this.shouldAverageOverIteration) {
			int counter=event.getIteration();
			if(counter>this.maxIter-5) {
				this.pcuVolumeCounter.geenerateLinkCounts();
				
				//this.fareHandler.getUpdatedMeasurements();
				if(this.counts==null) {
					counts=new HashMap<>();
					for(Measurement m:this.outputMeasurements.getMeasurements().values()) {
						counts.put(m.getId(), new HashMap<>());
						for(String timeId:m.getVolumes().keySet()) {
							counts.get(m.getId()).put(timeId, m.getVolumes().get(timeId));
						}
					}
				}else {
					//Map<String,Map<Id<Link>,Double>> newcounts=this.pcuVolumeCounter.geenerateLinkCounts();
					for(Measurement m:this.outputMeasurements.getMeasurements().values()) {
						for(String timeId:m.getVolumes().keySet()) {
							counts.get(m.getId()).put(timeId, counts.get(m.getId()).get(timeId)+m.getVolumes().get(timeId));
						}
					}
				}
			}
			if(counter==this.maxIter) {
				for(Measurement m:this.outputMeasurements.getMeasurements().values()) {
					for(String timeId:m.getVolumes().keySet()) {
						m.putVolume(timeId, counts.get(m.getId()).get(timeId)/this.AverageCountOverNoOfIteration);
					}
				}
				this.outputMeasurements.writeCSVMeasurements(controlerIO.getOutputFilename("outputMeasurements.csv"));
				new MeasurementsWriter(this.outputMeasurements).write(controlerIO.getOutputFilename("outputMeasurements.xml"));
				
				
			}
		}else {
		int counter=event.getIteration();
			if(counter%10==0) {
				this.outputMeasurements = this.pcuVolumeCounter.geenerateLinkCounts();
				
				this.outputMeasurements.writeCSVMeasurements(controlerIO.getIterationFilename(counter,"outputMeasurements.csv"));
				new MeasurementsWriter(this.outputMeasurements).write(controlerIO.getIterationFilename(counter,"outputMeasurements.xml"));
				
			}else if(counter==this.maxIter) {
				this.outputMeasurements = this.pcuVolumeCounter.geenerateLinkCounts();
				
				this.outputMeasurements.writeCSVMeasurements(controlerIO.getOutputFilename("outputMeasurements.csv"));
				new MeasurementsWriter(this.outputMeasurements).write(controlerIO.getOutputFilename("outputMeasurements.xml"));
			}
		}
		
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		
		
	}

	

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		if(this.generateOD) {
		this.SueAssignment.generateRoutesAndOD(event.getServices().getScenario().getPopulation(),
				odNetwork,
				event.getServices().getScenario().getTransitSchedule(),
				event.getServices().getScenario(), this.farecalc);
		}	

		
	}
}
