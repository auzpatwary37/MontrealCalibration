package calibration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import ust.hk.praisehk.metamodelcalibration.measurements.Measurement;
import ust.hk.praisehk.metamodelcalibration.measurements.MeasurementType;
import ust.hk.praisehk.metamodelcalibration.measurements.Measurements;


	public class PCUCounter implements LinkEnterEventHandler, TransitDriverStartsEventHandler,VehicleEntersTrafficEventHandler{
		
		private Map<String,Map<Id<Link>,Double>> linkCounts=new ConcurrentHashMap<>();
		private Map<String,Map<Id<Link>,List<Id<Vehicle>>>> Vehicles=new ConcurrentHashMap<>();
		private Map<Id<Vehicle>,Double> transitVehicles=new ConcurrentHashMap<>();
		private final Map<String, Tuple<Double,Double>> timeBean;
		private Measurements outputMeasurements;
		
		@Inject
		private Scenario scenario;
		
		@SuppressWarnings("deprecation")
		@Inject
		public PCUCounter(@Named("Output Measurements") Measurements outputMeasurements) {
			this.timeBean=outputMeasurements.getTimeBean();
			this.outputMeasurements=outputMeasurements;
			
			
			
			for(String timeBeanId:this.timeBean.keySet()) {
				linkCounts.put(timeBeanId,new ConcurrentHashMap<Id<Link>, Double>());
				Vehicles.put(timeBeanId, new ConcurrentHashMap<Id<Link>, List<Id<Vehicle>>>());
				for(Id<Link> linkId:this.outputMeasurements.getLinksToCount()) {
					linkCounts.get(timeBeanId).put(linkId, 0.0);
					Vehicles.get(timeBeanId).put(linkId, Collections.synchronizedList(new ArrayList<Id<Vehicle>>()));
				}
			}
			
		}
		
		public Measurements geenerateLinkCounts(){
			for(String timeBeanId:this.timeBean.keySet()) {
				for(Id<Link> LinkId:linkCounts.get(timeBeanId).keySet()) {
					double totalVehicle=0;
					for(Id<Vehicle> vId:Vehicles.get(timeBeanId).get(LinkId)) {
						if(this.transitVehicles.containsKey(vId)) {
							totalVehicle+=this.transitVehicles.get(vId);
						}else {
							//totalVehicle+=this.scenario.getVehicles().getVehicles().get(vId).getType().getPcuEquivalents();
							totalVehicle++;
						}
					}
					linkCounts.get(timeBeanId).put(LinkId,totalVehicle);
				}
			}
			
			for(Measurement m:this.outputMeasurements.getMeasurementsByType().get(MeasurementType.linkVolume)) {
				@SuppressWarnings("unchecked")
				ArrayList<Id<Link>> linkIds=(ArrayList<Id<Link>>)m.getAttribute(Measurement.linkListAttributeName);
				for(String timeId:m.getVolumes().keySet()) {
					double count=0;
					for(Id<Link> linkId:linkIds) {
						count+=linkCounts.get(timeId).get(linkId);
					}
					m.putVolume(timeId, count);
				}
			}
			
			return this.outputMeasurements;
		}
		
		
		@Override
		public void handleEvent(LinkEnterEvent event) {
		
			int time=(int) event.getTime();
			if(time>86400) {time=time-86400;}
			else if(time==0) {time=1;}
			String timeId=null;
			for(String s:this.timeBean.keySet()) {
				if(time>this.timeBean.get(s).getFirst() && time<=timeBean.get(s).getSecond()) {
					timeId=s;
				}
			}
			if(timeId!=null && this.linkCounts.get(timeId).containsKey(event.getLinkId())){
					this.Vehicles.get(timeId).get(event.getLinkId()).add(event.getVehicleId());			
			}
		}


		@Override
		public void handleEvent(TransitDriverStartsEvent event) {
			
		//	Id<TransitRoute> routeId=event.getTransitRouteId();
			Id<TransitLine> lineId=event.getTransitLineId();
			TransitLine tl=this.scenario.getTransitSchedule().getTransitLines().get(lineId);
			if(tl!=null) {
			//TransitRoute tr=tl.getRoutes().get(routeId);
			//String Mode=tr.getTransportMode();
			org.matsim.vehicles.Vehicles vehicles=this.scenario.getTransitVehicles();
			this.transitVehicles.put(event.getVehicleId(),vehicles.getVehicles().
					get(event.getVehicleId()).getType().getPcuEquivalents());
			}
		}
		@SuppressWarnings("deprecation")
		@Override
		public void reset(int i) {
			this.linkCounts.clear();
			this.Vehicles.clear();
			this.transitVehicles.clear();
			for(String timeBeanId:this.timeBean.keySet()) {
				linkCounts.put(timeBeanId,new ConcurrentHashMap<Id<Link>, Double>());
				Vehicles.put(timeBeanId, new ConcurrentHashMap<Id<Link>, List<Id<Vehicle>>>());
				for(Id<Link> linkId:this.outputMeasurements.getLinksToCount()) {
					linkCounts.get(timeBeanId).put(linkId, 0.0);
					Vehicles.get(timeBeanId).put(linkId, Collections.synchronizedList(new ArrayList<Id<Vehicle>>()));
				}
			}
		}

		@Override
		public void handleEvent(VehicleEntersTrafficEvent event) {
			int time=(int) event.getTime();
			if(time>=86400) {time=86400;}
			if(time==0) {
				time=1;
			}
			String timeId=null;
			for(String s:this.timeBean.keySet()) {
				
				if(time>this.timeBean.get(s).getFirst() && time<=timeBean.get(s).getSecond()) {
					timeId=s;
				}
				
			}
			if(timeId!=null && this.linkCounts.get(timeId).containsKey(event.getLinkId())){
					this.Vehicles.get(timeId).get(event.getLinkId()).add(event.getVehicleId());			
			}
			
		}
		
	}

