package calibration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import ust.hk.praisehk.metamodelcalibration.analyticalModel.AnalyticalModel;
import ust.hk.praisehk.metamodelcalibration.analyticalModelImpl.CNLSUEModel;
import ust.hk.praisehk.metamodelcalibration.calibrator.Calibrator;
import ust.hk.praisehk.metamodelcalibration.calibrator.CalibratorImpl;
import ust.hk.praisehk.metamodelcalibration.calibrator.ParamReader;
import ust.hk.praisehk.metamodelcalibration.matamodels.MetaModel;
import ust.hk.praisehk.metamodelcalibration.measurements.Measurements;
import ust.hk.praisehk.metamodelcalibration.measurements.MeasurementsReader;

public class CalibrationRun {
public static void main(String[] args) {
	String measurementsFileLoc = "src\\main\\resources\\montrealMeasurements_2020_2022.xml";
	String paramReaderFileLoc = "src\\main\\resources\\paramReaderTrial1.csv";
	String configFileLoc = "5_percent\\config.xml";
	String writeFileLoc = "CalibrationOutput\\";
	int maxIterSim = 50;
	int maxIter = 20;
	int thread = 10;
	double scale = 0.05;
	double initialTRRadius = 100;
	double maxTRRadius = 125;
	int maxSuccessiveRejection = 3;
	
	
	
	
	String populationFileLoc = "5_percent\\prepared_population.xml.gz";
	String networkFileLoc = "5_percent\\montreal_network.xml.gz";
	String transitScheduleFileLoc = "5_percent\\montreal_transit_schedules.xml.gz";
	String transitVehcileFileLoc = "5_percent\\montreal_transit_vehicles.xml.gz";
	String vehiclesFileLoc = "5_percent\\vehicle.xml";
	String facilityFileLoc = "5_percent\\montreal_facilities.xml.gz";
	String householdFileLoc = "5_percent\\montreal_households.xml.gz";
	
	Config config = ConfigUtils.createConfig();
	ConfigUtils.loadConfig(config,configFileLoc);
	Measurements countData = new MeasurementsReader().readMeasurements(measurementsFileLoc);
	countData.applyFactor(scale);
	ParamReader pReader = new ParamReader(paramReaderFileLoc);
	
	SimRunMontreal simrun = new SimRunMontreal(config, pReader, countData,writeFileLoc); 
	
	simrun.setMaxIterations(maxIterSim);
	simrun.setThread(thread);
	simrun.setScale(scale);
	simrun.setOutput(writeFileLoc+"output");
	simrun.setNetworkFileLoc(networkFileLoc);
	simrun.setPlanFile(populationFileLoc);
	simrun.setFacilitiesFileLoc(facilityFileLoc);
	simrun.setTsFileLoc(transitScheduleFileLoc);
	simrun.setTvFileLoc(transitVehcileFileLoc);
	simrun.setHouseholdFileLoc(householdFileLoc);
	simrun.setVehicleFileLoc(vehiclesFileLoc);
	LinkedHashMap<String,Double>params=pReader.getInitialParam();
	
	Calibrator calibrator = new CalibratorImpl(countData,writeFileLoc,false,pReader,initialTRRadius,maxSuccessiveRejection);
	writeRunParam(calibrator, writeFileLoc, params, pReader);
	AnalyticalModel sue=new CNLSUEModel(countData.getTimeBean());
	calibrator.setMaxTrRadius(maxTRRadius);
	
	
	for(int i=0;i<maxIter;i++) {
		sue.setDefaultParameters(pReader.ScaleUp(pReader.getDefaultParam()));
		sue.setFileLoc("CalibrationOutput/");
		Measurements m = simrun.run(sue, params, true, Integer.toString(i));
		params=calibrator.generateNewParam(sue, m, null, MetaModel.AnalyticalLinearMetaModelName);			
	}
}

public static void writeRunParam(Calibrator calibrator,String fileWriteLoc,LinkedHashMap<String,Double>params,ParamReader pReader) {
	try {
		FileWriter fw=new FileWriter(new File(fileWriteLoc+"RunParam.csv"),true);
		fw.append("ParameterName,initialParam ,upperLimit,lowerLimit\n");
		for(String s:params.keySet()) {
			fw.append(s+","+params.get(s)+","+pReader.getParamLimit().get(s).getFirst()+","+pReader.getParamLimit().get(s).getSecond()+"\n");
		}
		fw.append("initialTrustRegionRadius,"+calibrator.getTrRadius()+"\n");
		fw.append("TrInc,"+calibrator.getTrusRegionIncreamentRatio()+"\n");
		fw.append("TrDecreasing,"+calibrator.getTrustRegionDecreamentRatio()+"\n");
		fw.append("MaxTr,"+calibrator.getMaxTrRadius()+"\n");
		fw.append("MinTr,"+calibrator.getMinTrRadius()+"\n");
		fw.append("rou,"+calibrator.getThresholdErrorRatio()+"\n");
		fw.append("AnalyticalModelInternalParamterCalibration,"+calibrator.isShouldPerformInternalParamCalibration()+"\n");
		fw.append("MaxSuccesiveRejection,"+calibrator.getMaxSuccesiveRejection()+"\n");
		fw.append("MinMetaChangeReq,"+calibrator.getMinMetaParamChange()+"\n");
		fw.append("ObjectiveType,"+calibrator.getObjectiveType()+"\n");
		
		fw.append("StrtingTime,"+LocalDateTime.now().toString()+"\n");
		
		fw.flush();
		fw.close();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}
}
