package montreal;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.MatsimVehicleReader;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import com.google.inject.Inject;

import measurements.Measurement;
import measurements.MeasurementType;
import measurements.Measurements;
import measurements.MeasurementsWriter;



public class TravelTimeAndVolumeCalculator implements LinkEnterEventHandler, LinkLeaveEventHandler{
	@Inject
	private Scenario scenario;
	private Map<Id<Link>,Map<String,List<Double>>>totalTime=new ConcurrentHashMap<>();
	private Map<Id<Link>,Map<String,Double>> totalPCU = new ConcurrentHashMap<>();
	//private Map<Id<Link>,Map<String,Double>>totalVehicle=new ConcurrentHashMap<>();
	private Map<Id<Link>,Map<Id<Vehicle>,Double>> vehicleBuffer=new ConcurrentHashMap<>();
	private Map<String,Tuple<Double,Double>>timeBean = new HashMap<>();
	private Measurements measurements;
	private Measurements volumeMeasurements;
	private double startTime = 0;
	private double endTime = 0;
	private Network net;
	private Vehicles v;
	private Vehicles trv;
	
	public TravelTimeAndVolumeCalculator(double startTime,double endTime, Network net, Vehicles v, Vehicles trv) {
		this.net = net;
		this.startTime = startTime;
		this.endTime = endTime;
		this.v = v;
		this.trv = trv;
		for(int i = (int)this.startTime/3600;i<(int)this.endTime/3600;i++) {
			timeBean.put(Integer.toString(i), new Tuple<>(i*3600.,(i+1)*3600.));
		}
		measurements = Measurements.createMeasurements(timeBean);
		volumeMeasurements = Measurements.createMeasurements(timeBean);
		for(Entry<Id<Link>, ? extends Link> l:this.net.getLinks().entrySet()) {
			if(l.getKey().toString().contains("pt")) {
				continue;
			}
			Id<Link> linkId=l.getKey();
			Measurement m = measurements.createAnadAddMeasurement(linkId.toString()+"_time", MeasurementType.linkTravelTime);
			Measurement mv = volumeMeasurements.createAnadAddMeasurement(linkId.toString()+"_volume", MeasurementType.linkTravelTime);
			List<Id<Link>> links = new ArrayList<>();
			links.add(linkId);
			m.setAttribute(Measurement.linkListAttributeName,links);
			mv.setAttribute(Measurement.linkListAttributeName,links);
			this.vehicleBuffer.put(linkId, new ConcurrentHashMap<>());
			//this.totalVehicle.put(linkId, new ConcurrentHashMap<>());
			this.totalTime.put(linkId, new ConcurrentHashMap<>());
			this.totalPCU.put(linkId, new ConcurrentHashMap<>());
			for(String timeId:timeBean.keySet()) {
				this.totalTime.get(linkId).put(timeId, Collections.synchronizedList(new ArrayList<Double>()));
				this.totalPCU.get(linkId).put(timeId, 0.);
				m.putVolume(timeId, 0);
				mv.putVolume(timeId, 0);
				//this.totalVehicle.get(linkId).put(timeId, 0.);
			}
		}
	}
	public TravelTimeAndVolumeCalculator(Scenario scn) {
		this.scenario = scn;
		this.net = scenario.getNetwork();
		this.startTime = scenario.getConfig().qsim().getStartTime().seconds();
		this.endTime = scenario.getConfig().qsim().getEndTime().seconds();
		this.v = scenario.getVehicles();
		this.trv = scenario.getTransitVehicles();
		for(int i = (int)this.startTime/3600;i<(int)this.endTime/3600;i++) {
			timeBean.put(Integer.toString(i), new Tuple<>(i*3600.,(i+1)*3600.));
		}
		measurements = Measurements.createMeasurements(timeBean);
		volumeMeasurements = Measurements.createMeasurements(timeBean);
		for(Entry<Id<Link>, ? extends Link> l:this.net.getLinks().entrySet()) {
			if(l.getKey().toString().contains("pt")) {
				continue;
			}
			Id<Link> linkId=l.getKey();
			Measurement m = measurements.createAnadAddMeasurement(linkId.toString()+"_time", MeasurementType.linkTravelTime);
			Measurement mv = volumeMeasurements.createAnadAddMeasurement(linkId.toString()+"_volume", MeasurementType.linkTravelTime);
			List<Id<Link>> links = new ArrayList<>();
			links.add(linkId);
			m.setAttribute(Measurement.linkListAttributeName,links);
			mv.setAttribute(Measurement.linkListAttributeName,links);
			this.vehicleBuffer.put(linkId, new ConcurrentHashMap<>());
			//this.totalVehicle.put(linkId, new ConcurrentHashMap<>());
			this.totalTime.put(linkId, new ConcurrentHashMap<>());
			this.totalPCU.put(linkId, new ConcurrentHashMap<>());
			for(String timeId:timeBean.keySet()) {
				this.totalTime.get(linkId).put(timeId, Collections.synchronizedList(new ArrayList<Double>()));
				this.totalPCU.get(linkId).put(timeId, 0.);
				m.putVolume(timeId, 0);
				mv.putVolume(timeId, 0);
				//this.totalVehicle.get(linkId).put(timeId, 0.);
			}
		}
	}
	@Inject
	TravelTimeAndVolumeCalculator() {
		this.net = scenario.getNetwork();
		this.startTime = scenario.getConfig().qsim().getStartTime().seconds();
		this.endTime = scenario.getConfig().qsim().getEndTime().seconds();
		this.v = scenario.getVehicles();
		this.trv = scenario.getTransitVehicles();
		for(int i = (int)this.startTime/3600;i<(int)this.endTime/3600;i++) {
			timeBean.put(Integer.toString(i), new Tuple<>(i*3600.,(i+1)*3600.));
		}
		measurements = Measurements.createMeasurements(timeBean);
		volumeMeasurements = Measurements.createMeasurements(timeBean);
		for(Entry<Id<Link>, ? extends Link> l:this.net.getLinks().entrySet()) {
			if(l.getKey().toString().contains("pt")) {
				continue;
			}
			Id<Link> linkId=l.getKey();
			Measurement m = measurements.createAnadAddMeasurement(linkId.toString()+"_time", MeasurementType.linkTravelTime);
			Measurement mv = volumeMeasurements.createAnadAddMeasurement(linkId.toString()+"_volume", MeasurementType.linkTravelTime);
			List<Id<Link>> links = new ArrayList<>();
			links.add(linkId);
			m.setAttribute(Measurement.linkListAttributeName,links);
			mv.setAttribute(Measurement.linkListAttributeName,links);
			this.vehicleBuffer.put(linkId, new ConcurrentHashMap<>());
			//this.totalVehicle.put(linkId, new ConcurrentHashMap<>());
			this.totalTime.put(linkId, new ConcurrentHashMap<>());
			this.totalPCU.put(linkId, new ConcurrentHashMap<>());
			for(String timeId:timeBean.keySet()) {
				this.totalTime.get(linkId).put(timeId, Collections.synchronizedList(new ArrayList<Double>()));
				this.totalPCU.get(linkId).put(timeId, 0.);
				m.putVolume(timeId, 0);
				mv.putVolume(timeId, 0);
				//this.totalVehicle.get(linkId).put(timeId, 0.);
			}
		}
	}
	
	public Measurements getUpdatedTimeMeasurements() {
		
		for(Entry<Id<Measurement>, Measurement> m:this.measurements.getMeasurements().entrySet()) {
			Id<Link>linkId = ((List<Id<Link>>)m.getValue().getAttributes().get(Measurement.linkListAttributeName)).get(0);
			for(String timeId:m.getValue().getVolumes().keySet()) {
				if(this.totalTime.get(linkId).get(timeId).size()!=0) {
					m.getValue().putVolume(timeId, calcAverage(this.totalTime.get(linkId).get(timeId)));
				}else {
					Link link=this.net.getLinks().get(linkId);
					m.getValue().putVolume(timeId, link.getLength()/link.getFreespeed());
				}
			}
		}
		return this.measurements;
	}
	
	public Measurements getUpdatedVolumeMeasurements() {
		
		for(Entry<Id<Measurement>, Measurement> m:this.volumeMeasurements.getMeasurements().entrySet()) {
			Id<Link>linkId = ((List<Id<Link>>)m.getValue().getAttributes().get(Measurement.linkListAttributeName)).get(0);
			for(String timeId:m.getValue().getVolumes().keySet()) {
				if(this.totalTime.get(linkId).get(timeId).size()!=0) {
					m.getValue().putVolume(timeId, this.totalPCU.get(linkId).get(timeId));
				}
			}
		}
		return this.volumeMeasurements;
	}
	
	private Double calcAverage(List<Double> list) {
		
		double sum=0;
		double num=list.size();
		for(Double d:list) {
			sum+=d;
		}
		if(num==0) {
			return 0.;
		}
		return sum/num;
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id<Link>linkId=event.getLinkId();
		if(this.vehicleBuffer.containsKey(linkId) && this.vehicleBuffer.get(linkId).containsKey(event.getVehicleId())) {
			double timeLength=event.getTime()-this.vehicleBuffer.get(linkId).get(event.getVehicleId());
			double middleTime=event.getTime()-timeLength/2;
			String timeId=this.getTimeId(middleTime);
			if(timeId!=null && this.totalTime.get(linkId).get(timeId)!=null) {
				this.totalTime.get(linkId).get(timeId).add(timeLength);
				Vehicle v = null;
				if(this.v.getVehicles().containsKey(event.getVehicleId()))v = this.v.getVehicles().get(event.getVehicleId());
				else if(this.trv.getVehicles().containsKey(event.getVehicleId()))v = this.trv.getVehicles().get(event.getVehicleId());
				this.totalPCU.get(linkId).put(timeId,this.totalPCU.get(linkId).get(timeId)+v.getType().getPcuEquivalents());
				//this.totalVehicle.get(linkId).put(timeId, this.totalVehicle.get(linkId).get(timeId)+1);
			}
			this.vehicleBuffer.get(linkId).remove(event.getVehicleId());
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if(this.vehicleBuffer.containsKey(event.getLinkId())){
			this.vehicleBuffer.get(event.getLinkId()).put(event.getVehicleId(), event.getTime());
		}
	}
	
	private String getTimeId(double time) {
		if(time==0) {
			time=1;
		}else if(time>24*3600) {
			time=time-24*3600;
		}
		for(Entry<String, Tuple<Double, Double>> timeBean:this.timeBean.entrySet()) {
			if(time>timeBean.getValue().getFirst() && time<=timeBean.getValue().getSecond()) {
				return timeBean.getKey();
			}
		}
		return null;
	}
	public void reset() {
		for(Entry<Id<Link>, ? extends Link> l:this.net.getLinks().entrySet()) {
			if(l.getKey().toString().contains("pt")) {
				continue;
			}
			Id<Link> linkId=l.getKey();
			this.vehicleBuffer.put(linkId, new ConcurrentHashMap<>());
			//this.totalVehicle.put(linkId, new ConcurrentHashMap<>());
			this.totalTime.put(linkId, new ConcurrentHashMap<>());
			for(String timeId:timeBean.keySet()) {
				this.totalTime.get(linkId).put(timeId, Collections.synchronizedList(new ArrayList<Double>()));
				this.totalPCU.get(linkId).put(timeId, 0.);
				//this.totalVehicle.get(linkId).put(timeId, 0.);
				this.measurements.getMeasurements().get(Id.create(linkId.toString()+"_time",Measurement.class)).putVolume(timeId, 0);
			}
		}
	}
	
	public static void main(String[] args) {
		String year = "";
		String eventFileLoc = "outputEm"+year+"BasePop/output_events.xml.gz";
		String networkFileLoc = "outputEm"+year+"BasePop/output_network.xml.gz";
		String configFileLoc = "outputEm"+year+"BasePop/output_config.xml";
		String measurementsFileName = "outputEm"+year+"BasePop/measurements";
		String networkCsvFileLoc = "outputEm"+year+"BasePop/output_network.csv";
		String vehicleFile = "outputEm"+year+"BasePop/output_vehicles.xml.gz";
		String transitVehicleFile = "outputEm"+year+"BasePop/output_transitVehicles.xml.gz";
		Config config = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(config, configFileLoc);
		
		Vehicles v = VehicleUtils.createVehiclesContainer();
		Vehicles trv = VehicleUtils.createVehiclesContainer();
		
		new MatsimVehicleReader(v).readFile(vehicleFile);
		new MatsimVehicleReader(trv).readFile(transitVehicleFile);
		
		EventsManager em = new EventsManagerImpl();
		TravelTimeAndVolumeCalculator lpc= new TravelTimeAndVolumeCalculator(0,config.qsim().getEndTime().seconds(),NetworkUtils.readNetwork(networkFileLoc),v,trv);
		em.addHandler(lpc);
		new EventsReaderXMLv1(em).readFile(eventFileLoc);
		Measurements timeM = lpc.getUpdatedTimeMeasurements();
		Measurements volM = lpc.getUpdatedVolumeMeasurements();
		new MeasurementsWriter(timeM).write(measurementsFileName + "_time.xml");
		new MeasurementsWriter(volM).write(measurementsFileName + "_volume.xml");
		timeM.writeCSVMeasurements(measurementsFileName+"_time.csv");
		volM.writeCSVMeasurements(measurementsFileName+"_volume.csv");
		writeToCsv(NetworkUtils.readNetwork(networkFileLoc),networkCsvFileLoc);
	}
	
	public static void writeToCsv(Network net, String out) {
		FileWriter fw;
		try {
			fw = new FileWriter(new File(out));
			fw.append("Id,fromNode,toNode,fromX,fromY,toX,toY,speed,length,capacity,lanes\n");
			for(Entry<Id<Link>, ? extends Link> l:net.getLinks().entrySet()) {
				fw.append(l.getKey().toString()+","+l.getValue().getFromNode().getId().toString()+","+l.getValue().getToNode().toString()+","+l.getValue().getFromNode().getCoord().getX()+","+
			l.getValue().getFromNode().getCoord().getY()+","+l.getValue().getToNode().getCoord().getX()+","+l.getValue().getToNode().getCoord().getY()+","+l.getValue().getFreespeed()+","
						+l.getValue().getLength()+","+l.getValue().getCapacity()+","+l.getValue().getNumberOfLanes()+"\n");
				fw.flush();
			}
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
}
