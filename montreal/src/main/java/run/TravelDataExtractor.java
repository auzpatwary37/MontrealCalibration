package run;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.vehicles.Vehicle;

public class TravelDataExtractor {

    static class TravelDataHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler, LinkEnterEventHandler, ActivityStartEventHandler,ActivityEndEventHandler,  PersonEntersVehicleEventHandler {
        private final Map<Id<Person>, TripData> ongoingTrips = new HashMap<>();
        private final List<TripData> completedTrips = new ArrayList<>();
        private final Map<Id<Person>, String> arrivalActivities = new HashMap<>();
        private final Map<Id<Vehicle>,Id<Person>> vehicleOwnership = new HashMap<>();
        private final Map<Id<Person>,String> activityEnd = new HashMap<>();
        private final Network network;

        public TravelDataHandler(Network network) {
            this.network = network;
        }

        @Override
        public void handleEvent(PersonDepartureEvent event) {
            Id<Person> personId = event.getPersonId();
            String actType = this.activityEnd.get(event.getPersonId());
            
            if(actType.contains("interaction")) {
            	return;
            }

//            // If a trip is already ongoing, finalize it before starting a new one
//            if (!ongoingTrips.containsKey(personId)) {
//            	ongoingTrips.put(personId, new ArrayList<>());
//            }
            
            
//            TripData trip = ongoingTrips.remove(personId);
//            trip.setArrivalTime(event.getTime());
//            trip.calculateTravelTime();
//            trip.setArrivalActivity("unknown"); // Activity might be missing
//            completedTrips.add(trip);
            

            // Start a new trip after finalizing the previous one
            ongoingTrips.put(personId, new TripData(personId.toString(), event.getRoutingMode(), event.getTime()));
        }


        @Override
        public void handleEvent(PersonArrivalEvent event) {
            // Arrival alone does not end a trip, wait for a valid activity
        }

        @Override
        public void handleEvent(LinkEnterEvent event) {
            Id<Person> personId = this.vehicleOwnership.get(event.getVehicleId());
            Id<Link> linkId = event.getLinkId();
            
            if (ongoingTrips.containsKey(personId) && network.getLinks().containsKey(linkId)) {
            	TripData trip = ongoingTrips.get(personId);
                Link link = network.getLinks().get(linkId);
                trip.addDistance(link.getLength());
            }
        }

        @Override
        public void handleEvent(ActivityStartEvent event) {
            Id<Person> personId = event.getPersonId();
            String activityType = event.getActType();

            // Ignore interaction activities (e.g., "pt interaction", "car interaction")
            if (activityType.contains("interaction")) {
                return;
            }

            // If a valid activity starts, finalize the previous trip
            if (ongoingTrips.containsKey(personId)) {
                TripData trip = ongoingTrips.remove(personId);
                trip.setArrivalTime(event.getTime());
                trip.calculateTravelTime();
                trip.setArrivalActivity(activityType);
                completedTrips.add(trip);
            }
        }

        public List<TripData> getCompletedTrips() {
            return completedTrips;
        }

		@Override
		public void handleEvent(PersonEntersVehicleEvent event) {
			this.ongoingTrips.get(event.getPersonId()).setVehicleId(event.getVehicleId().toString());
			this.vehicleOwnership.put(event.getVehicleId(), event.getPersonId());
		}

		@Override
		public void handleEvent(ActivityEndEvent event) {
			this.activityEnd.put(event.getPersonId(), event.getActType());
		}
    }

    static class TripData {
        private final String personId;
        private final String mode;
        private final double departureTime;
        private double arrivalTime;
        private double travelTime;
        private double travelDistance = 0.0;
        private String arrivalActivity = "unknown";
        private String vehicleId;

        public TripData(String personId, String mode, double departureTime) {
            this.personId = personId;
            this.mode = mode;
            this.departureTime = departureTime;
        }

        public void setVehicleId(String id) {
        	this.vehicleId = id;
        }
        
        public void setArrivalTime(double arrivalTime) {
            this.arrivalTime = arrivalTime;
        }

        public void addDistance(double distance) {
            this.travelDistance += distance;
        }

        public void calculateTravelTime() {
            this.travelTime = arrivalTime - departureTime;
        }

        public void setArrivalActivity(String activity) {
            this.arrivalActivity = activity;
        }

        @Override
        public String toString() {
            return personId + "," + mode + "," + departureTime + "," + arrivalTime + "," + travelTime + "," + travelDistance + "," + arrivalActivity;
        }
    }

    public static void extractTravelData(String eventsFilePath, String networkFilePath, String outputFolder) {
        // Ensure the output directory exists
        File folder = new File(outputFolder);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        String travelDataCsv = outputFolder + "/travel_data.csv";

        // Read network file
        Network network = NetworkUtils.readNetwork(networkFilePath);

        EventsManagerImpl eventsManager = new EventsManagerImpl();
        TravelDataHandler handler = new TravelDataHandler(network);
        eventsManager.addHandler(handler);

        // Read MATSim events file
        new MatsimEventsReader(eventsManager).readFile(eventsFilePath);

        // Write travel data to CSV
        try (FileWriter writer = new FileWriter(travelDataCsv)) {
            writer.write("PersonID,Mode,DepartureTime,ArrivalTime,TravelTime,TravelDistance,ArrivalActivity\n");
            for (TripData trip : handler.getCompletedTrips()) {
                writer.write(trip.toString() + "\n");
            }
            System.out.println("Travel data CSV file created: " + travelDataCsv);
        } catch (IOException e) {
            System.err.println("Error writing travel data CSV: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        // Define input event file, network file, and output folder
        String eventsFile = "osorio\\output_events.xml.gz";  // Change this to your actual file path
        String networkFile = "osorio\\output_network.xml.gz";       // Change to your network file path
        String outputFolder = "osorio";           // Files will be saved in "osorio" folder

        extractTravelData(eventsFile, networkFile, outputFolder);
    }
}
