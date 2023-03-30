package population;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsWriterV10;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.Vehicles;

public class PopulationGenerator {
public static void main(String[] args) throws IOException{
	String facilityFileLoc = "data/output_facilities.xml.gz";
	String odFileLocation = "data/mtl18pv2c.csv";
	String facilityWriteCsvLocation = "newData/facCoord.csv";
	String facilityToCTUIDMap = "data/facToCTUID.csv";
	String populationWriteLocation = "data/outputPopulation.xml";
	Map<String,Map<Double,Set<Id<ActivityFacility>>>> ctuidToFacilityMap = new HashMap<>();
	Config config = ConfigUtils.createConfig();
	config.facilities().setInputFile(facilityFileLoc);
	Scenario scenario = ScenarioUtils.loadScenario(config);
	
	ActivityFacilities fac = scenario.getActivityFacilities();
	
	Population population = scenario.getPopulation();
	Vehicles vehicles = scenario.getVehicles();
	Households matsimHouseholds = scenario.getHouseholds();
	
	double scale = .1;
	
	BufferedReader bf = new BufferedReader(new FileReader(new File(facilityToCTUIDMap)));
	bf.readLine();
	String line = null;
	Set<Double> ctList = new HashSet<>();
	while((line = bf.readLine())!=null) {
		String[] part = line.split(",");
		Id<ActivityFacility> facId = Id.create(part[0], ActivityFacility.class);
		double ct = Double.parseDouble(part[1]);
		ctList.add(ct);
		fac.getFacilities().get(facId).getActivityOptions().put("errands", fac.getFactory().createActivityOption("errands"));
		
		fac.getFacilities().get(facId).getActivityOptions().values().forEach(at->{
			if(!ctuidToFacilityMap.containsKey(at.getType()))ctuidToFacilityMap.put(at.getType(), new HashMap<>());
			if(!ctuidToFacilityMap.get(at.getType()).containsKey(ct))ctuidToFacilityMap.get(at.getType()).put(ct, new HashSet<>());
			
			ctuidToFacilityMap.get(at.getType()).get(ct).add(facId);
			
		});
	}
	
	String h ="ipere;idu;version;media_int;provenance;date_int;deb_interv;nolog;tlog;nbper;nbveh;revenu;langue;nmodif_ass;incaplogi;xmtmlog;ymtmlog;r6log;r8log;rmrlog;arrlog;sdrlog;srlog;sdomi65;smlog;cplog;agr_domi;jour;sem;facmen;facmen18;noper;tper;clepersonne;sexe;age;grpage;incap;percond;abon_ap;abon_vls;passetc;occper;tele_trav;lieuocc;xmtmocc;ymtmocc;rmrocc;arrocc;sdrocc;srocc;smocc;cpocc;l_id_local;mobil;facper;facper18;facdep18;cledeplacement;nodep;no_rep;hredep;ghredep;motif;motif_grp;date_dpl;jour_dpl;mode1;mode2;mode3;mode4;mode5;mode6;mode7;mode8;seq_modes;covoiturage;pers_auto;auto_ap;velo_ls;station_p;pont1;pont2;mode_exclu;lig1;lig2;lig3;lig4;lig5;lig_imp;metro1;metro2;metro3;metro4;gare1;gare2;gare3;gare4;aut1;aut2;aut3;aut4;aut5;aut6;aut7;aut8;aut9;aut10;aut11;aut12;trace1;trace2;trace3;trace4;trace5;dist;agr_ori;xmtmori;ymtmori;xlonori;ylatori;r6orig;r8orig;arrori;sdrori;srori;sorig65;smori;cpori;o_id_local;inodo;loccodeo;xyo;agr_des;xmtmdes;ymtmdes;xlondes;ylatdes;r6dest;r8dest;arrdes;sdrdes;srdes;sdest65;smdes;cpdes;d_id_local;inodd;loccoded;xyd;Depl_Excl;nbjct;typjct1;typjct1_98;stajct1;agr_jct1;j_id_local1;xmtmjct1;ymtmjct1;xlonjct1;ylatjct1;r6jct1;r8jct1;arrjct1;sdrjct1;srjct1;sjonc65_1;smjct1;inodj1;loccodej1;nchangzone1;xmtmjct2;ymtmjct2;xlonjct2;ylatjct2;r6jct2;r8jct2;arrjct2;sdrjct2;srjct2;sjonc652;smjct2;inodj2;loccodej2;nchangzone2;tc;actif;autre_mot;motorise;metro;stm;rtl;stl;auto;autop;autoc;mauto;autrebus;busscol;taxi;train;a_pied;moto;velo;cit;au_mode;tr_adap;indet1";
	String[] header = h.split(";");
	
	Reader in = new FileReader(odFileLocation);

    CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
        .setHeader(header)
        .setDelimiter(";")
        .setSkipHeaderRecord(true)
        .build();

    Iterable<CSVRecord> records = csvFormat.parse(in);
    Map<Id<HouseHold>,HouseHold> households = new HashMap<>();
    
    for (CSVRecord record : records) {
    	//try{
    	Id<HouseHold> hhId = Id.create(record.get("nolog"),HouseHold.class);
        Id<Member> memId = Id.create(hhId.toString()+"_"+record.get("noper"),Member.class);
        String tripNo = record.get("nodep");
        Id<Trip> tripId = null;
        if(tripNo!=null && !tripNo.equals(""))tripId = Id.create(memId.toString()+"_"+tripNo, Trip.class);
        
        if(!households.containsKey(hhId)) {
        	HouseHold hh = new HouseHold(hhId.toString(), Integer.parseInt(record.get("revenu")), 
        			record.get("xmtmlog").equals("")?null:Double.parseDouble(record.get("xmtmlog")), 
        			record.get("ymtmlog").equals("")?null:Double.parseDouble(record.get("ymtmlog")), 
        			record.get("srlog").equals("")?null:Double.parseDouble(record.get("srlog")), 
        			Double.parseDouble(record.get("facmen18").replace(",", ".")), 
        			false, Integer.parseInt(record.get("nbveh")));
        	households.put(hh.getHhId(), hh);
        	hh.setMemSize(Integer.parseInt(record.get("nbper")));
        }
        
        HouseHold hh = households.get(hhId);
        Member member = hh.getMembers().get(memId);
        if(member==null) {
        	member = new Member(memId.toString(), hh, Integer.parseInt(record.get("age")),Integer.parseInt(record.get("percond"))==1, Double.parseDouble(record.get("facper18").replace(",", ".")),
        			hh.getIncomeGroup(), Integer.parseInt(record.get("sexe")), Integer.parseInt(record.get("occper")), Integer.parseInt(record.get("tele_trav"))==1, 
        			record.get("xmtmocc").equals("")?null:Double.parseDouble(record.get("xmtmocc")), record.get("ymtmocc").equals("")?null:Double.parseDouble(record.get("ymtmocc")),record.get("srocc").equals("")?null:Double.parseDouble(record.get("srocc")));
        	hh.addMember(member);
        	if(member.getAgeGroup()<15)hh.setIfKids(true);
        }
        if(tripNo!=null && !tripNo.equals("")) {
        	double timehhmm = Double.parseDouble(record.get("hredep"));
        	double time = (int)timehhmm/100*3600+timehhmm%100*60;
        	
        	
        	Trip trip = new Trip(tripId.toString(), member, Double.parseDouble(record.get("xmtmori")), Double.parseDouble(record.get("ymtmori")), Double.parseDouble(record.get("xmtmdes")), 
        			Double.parseDouble(record.get("ymtmdes")),
        			time,record.get("facdep18").equals("")?member.getPersonExFac():Double.parseDouble(record.get("facdep18").replace(",", ".")), 
        			record.get("srori").equals("")?null:Double.parseDouble(record.get("srori")), 
        			record.get("srdes").equals("")?null:Double.parseDouble(record.get("srdes")),
        			extractActivity(record),
        			Integer.parseInt(record.get("mobil")),
        			extractModes(record), record.get("jour_dpl"));
        	member.addTrip(trip);
        }
        
    }
	Map<String,Map<Id<HouseHold>,Double>> hhSpare = new HashMap<>();
	Map<String,Map<Id<Member>,Double>> memberSpare = new HashMap<>();
	Map<String,Map<Id<Trip>,Double>> tripSpare = new HashMap<>();
	
	households.values().forEach(hh->{
		hh.checkAndUpdateLimitingFactors();
		hh.checkForCTConsistancy(new ArrayList<>(ctList));
	});
	
    households.values().forEach(hh->{

    	hh.loadClonedHouseHoldPersonAndVehicle(population, vehicles, matsimHouseholds, fac, ctuidToFacilityMap, scale, hhSpare, memberSpare, tripSpare);
    });
    
    new PopulationWriter(population).write("data/outputODPopulation_"+scale+".xml.gz");
    new MatsimVehicleWriter(vehicles).writeFile("data/outputODVehicle_"+scale+".xml.gz");
    new HouseholdsWriterV10(matsimHouseholds).writeFile("data/outputODHouseholds_"+scale+".xml.gz");
    new FacilitiesWriter(fac).write("data/outputODFacilities.xml.gz");
}
public static String[] extractModes(CSVRecord record) {
	List<String> modes = new ArrayList<>();
	for(int i = 1;i<9;i++) {
		String modeString = "mode"+i;
		if(record.get(modeString)!=null && !record.get(modeString).equals("")) {
			switch(Integer.parseInt(record.get(modeString))) {
			case 1: //its a car
				modes.add("car");
				break;
			case 2: // car passenger
				modes.add("car_passenger");
				break;
			case 3: // stm bus 
				modes.add("pt");
				break;
			case 4: //metro
				modes.add("pt");
				break;
			case 5: // RTL
				modes.add("pt");
				break;
			case 6: // STL
				modes.add("pt");
				break;
			case 7: //exo/mrc
				modes.add("pt");
				break;
			case 8: //train
				modes.add("pt");
				break;
			case 9: //Schoolbus for now just kept it as car passenger
				modes.add("car_passenger");
				break;
			case 10://other bus, for now just kept it as car passenger
				modes.add("car_passenger");
				break;
			case 11://taxi
				modes.add("car");
				break;
			case 12:// motorcycle
				modes.add("car");
				break;
			case 13:// bike
				modes.add("bike");
				break;
			case 14: // walking
				modes.add("walk");
				break;
			case 15://paratransit
				modes.add("pt");
				break;
			case 16://outside entry
				modes.add("pt");
				break;
			case 17://junction point
				modes.add("car");
				break;
			case 18://indeterminant 
				modes.add("car");
				break;
			default:
				modes.add("car");
			}
		}
	}
	String[] modesArray = new String[modes.size()];
	int i = 0;
	for(String s:modes) {
		modesArray[i]=s;
		i++;
	}
	
	return modesArray;
}

public static String extractActivity(CSVRecord record) {
	if(record.get("nodep")!=null && !record.get("nodep").equals("")) {
		int motive = Integer.parseInt(record.get("motif"));
		
		if(motive==0) {//no movment this should not happen when nodep is not null
			
		}else if(motive == 1||motive ==2) {
			return "work";
		}else if(motive==4) {
			return "education";
		}else if(motive == 5) {
			return "shop";
		}else if(motive == 6||motive == 7) {
			return "leisure";
		}else if(motive == 11) {
			return "home";
		}else if(motive == 8|| motive == 9 || motive==10) {
			return "errands";
		}else if(motive == 3 || motive==12|| motive==13) {
			return "other";
		}
					
			
	}
	return null;
}

//Chat gpt code 

//public static void redistributeExpansions(Map<Id<HouseHold>, HouseHold> hhMap) {
//    // create a map to hold the new expansion factors for each household
//    Map<Id<HouseHold>, Double> newHhExpansions = new HashMap<>();
//
//    // create maps to hold the new expansion factors for members and trips
//    Map<Id<Member>, Double> newMemberExpansions = new HashMap<>();
//    Map<Id<Trip>, Double> newTripExpansions = new HashMap<>();
//    Map<Id<Member>,Member> members = new HashMap<>();
//
//    // iterate over each household and calculate the new expansion factor
//    for (Id<HouseHold> hhId : hhMap.keySet()) {
//        double hhExpFactor = 0.0;
//        Map<String, Double> hhAttributes = new HashMap<>();
//
//        HouseHold hh = hhMap.get(hhId);
//        Map<Id<Member>, Member> memberMap = hh.getMembers();
//
//        // iterate over each member in the household and sum their expansion factors
//        for (Id<Member> memberId : memberMap.keySet()) {
//            Member member = memberMap.get(memberId);
//            double memberExpFactor = member.getPersonExFac();
//            String memberAttrKey = member.generateBehavioralKey();
//            newMemberExpansions.put(memberId, 0.0);
//            members.put(memberId, member);
//            hhAttributes.merge(memberAttrKey, memberExpFactor, Double::sum);
//
//            Map<Id<Trip>, Trip> tripMap = member.getTrips();
//
//            // iterate over each trip for the member and sum their expansion factors
//            for (Id<Trip> tripId : tripMap.keySet()) {
//                Trip trip = tripMap.get(tripId);
//                double tripExpFactor = trip.getTripExpFactror();
//                String tripAttrKey = trip.generateBehavioralKey();
//                newTripExpansions.put(tripId, 0.0);
//                hhAttributes.merge(tripAttrKey, tripExpFactor, Double::sum);
//            }
//        }
//
//        // sum the expansion factors for all members and trips in the household
//        for (double expFactor : hhAttributes.values()) {
//            hhExpFactor += expFactor;
//        }
//
//        // update the new expansion factor for the household
//        newHhExpansions.put(hhId, hhExpFactor);
//    }
//
//    // redistribute the new expansion factors for members and trips
//    for (Id<Member> memberId : newMemberExpansions.keySet()) {
//        Member member = members.get(memberId);
//        member.setNewExpFac(newHhExpansions.get(member.getHouseHold().getHhId()) * newMemberExpansions.get(memberId));
//    }
//
//    for (Id<Trip> tripId : newTripExpansions.keySet()) {
//        Trip trip = hhMap.get(tripId.getHouseHoldId()).getMember().get(tripId.getMemberId()).getTrip().get(tripId);
//        trip.setExpansionFactor(newHhExpansions.get(tripId.getHouseHoldId()) * newTripExpansions.get(tripId));
//    }
//}
}
