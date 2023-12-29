package population;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
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
import org.apache.mahout.math.random.Multinomial;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsWriterV10;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.Vehicles;

public class PopulationGeneratorEnhanced {
public static void main(String[] args) throws IOException{
	String facilityFileLoc = "data/output_facilities.xml.gz";
	String odFileLocation = "EOD_from_Louiselle/enhancedOd.csv";
	String facilityWriteCsvLocation = "newData/facCoord.csv";
	String facilityToCTUIDMap = "EOD_from_Louiselle/facToDAUID.csv";
	String populationWriteLocation = "data/outputPopulation.xml";
	String ctCentroidFile = "EOD_from_Louiselle/dessiminationAreaCentroid.csv";
	double timeToSpread = 5*60;
	int year = 18;
	String hhEFKey = "facmen18";
	String mmEFKey = "facper18";
	String tpEFKey = "facdep18";
	if(year!=18) {
		
		hhEFKey = "FMEN"+year;
		mmEFKey = "FPER"+year;
		tpEFKey = "FDEP"+year;
	}
	
	BufferedReader bf = new BufferedReader(new FileReader(new File(ctCentroidFile)));
	bf.readLine();
	String line = null;
	Network odNet = NetworkUtils.createNetwork();
	NetworkFactory netFac = odNet.getFactory();
	while((line=bf.readLine())!=null) {
		String[] parts = line.split(",");
		odNet.addNode(netFac.createNode(Id.createNodeId(parts[0]),new Coord(Double.parseDouble(parts[1]),Double.parseDouble(parts[2]))));
	}
	new NetworkWriter(odNet).write("EOD_from_Louiselle/EOD/CTUNet.xml");
	
	Map<String,Map<Double,Set<Id<ActivityFacility>>>> ctuidToFacilityMap = new HashMap<>();
	Config config = ConfigUtils.createConfig();
	config.facilities().setInputFile(facilityFileLoc);
	Scenario scenario = ScenarioUtils.loadScenario(config);
	
	ActivityFacilities fac = scenario.getActivityFacilities();
	
	Population population = scenario.getPopulation();
	Vehicles vehicles = scenario.getVehicles();
	Households matsimHouseholds = scenario.getHouseholds();
	
	double scale = .05;
	
	bf = new BufferedReader(new FileReader(new File(facilityToCTUIDMap)));
	bf.readLine();
	line = null;
	Set<Double> ctList = new HashSet<>();
	while((line = bf.readLine())!=null) {
		String[] part = line.split(",");
		Id<ActivityFacility> facId = Id.create(part[0], ActivityFacility.class);
		double ct = Double.parseDouble(part[1]);
		ctList.add(ct);
		
		fac.getFacilities().get(facId).getActivityOptions().put("errands", fac.getFactory().createActivityOption("errands"));
		fac.getFacilities().get(facId).getAttributes().putAttribute("CTUID", ct);
		fac.getFacilities().get(facId).getActivityOptions().values().forEach(at->{
			if(!ctuidToFacilityMap.containsKey(at.getType()))ctuidToFacilityMap.put(at.getType(), new HashMap<>());
			if(!ctuidToFacilityMap.get(at.getType()).containsKey(ct))ctuidToFacilityMap.get(at.getType()).put(ct, new HashSet<>());
			
			ctuidToFacilityMap.get(at.getType()).get(ct).add(facId);
			
		});
	}
	String h = "ipere;idu;version;media_int;provenance;date_int;deb_interv;nolog;tlog;nbper;nbveh;revenu;langue;nmodif_ass;incaplogi;xmtmlog;ymtmlog;r6log;r8log;rmrlog;arrlog;sdrlog;srlog;sdomi65;smlog;cplog;agr_domi;jour;sem;facmen;facmen18;noper;tper;clepersonne;sexe;age;grpage;incap;percond;abon_ap;abon_vls;passetc;occper;tele_trav;lieuocc;xmtmocc;ymtmocc;rmrocc;arrocc;sdrocc;srocc;smocc;cpocc;l_id_local;mobil;facper;facper18;facdep18;cledeplacement;nodep;no_rep;hredep;ghredep;motif;motif_grp;date_dpl;jour_dpl;mode1;mode2;mode3;mode4;mode5;mode6;mode7;mode8;seq_modes;covoiturage;pers_auto;auto_ap;velo_ls;station_p;pont1;pont2;mode_exclu;lig1;lig2;lig3;lig4;lig5;lig_imp;metro1;metro2;metro3;metro4;gare1;gare2;gare3;gare4;aut1;aut2;aut3;aut4;aut5;aut6;aut7;aut8;aut9;aut10;aut11;aut12;trace1;trace2;trace3;trace4;trace5;dist;agr_ori;xmtmori;ymtmori;xlonori;ylatori;r6orig;r8orig;arrori;sdrori;srori;sorig65;smori;cpori;o_id_local;inodo;loccodeo;xyo;agr_des;xmtmdes;ymtmdes;xlondes;ylatdes;r6dest;r8dest;arrdes;sdrdes;srdes;sdest65;smdes;cpdes;d_id_local;inodd;loccoded;xyd;Depl_Excl;nbjct;typjct1;typjct1_98;stajct1;agr_jct1;j_id_local1;xmtmjct1;ymtmjct1;xlonjct1;ylatjct1;r6jct1;r8jct1;arrjct1;sdrjct1;srjct1;sjonc65_1;smjct1;inodj1;loccodej1;nchangzone1;xmtmjct2;ymtmjct2;xlonjct2;ylatjct2;r6jct2;r8jct2;arrjct2;sdrjct2;srjct2;sjonc652;smjct2;inodj2;loccodej2;nchangzone2;tc;actif;autre_mot;motorise;metro;stm;rtl;stl;auto;autop;autoc;mauto;autrebus;busscol;taxi;train;a_pied;moto;velo;cit;au_mode;tr_adap;indet1;IPERE;FMEN18E;FPER18E;FPER18MCE;FDEP18E;FMEN21;FMEN26;FMEN31;FMEN36;FMEN41;FMEN46;FMEN51;FMEN56;FMEN61;FPER21;FPER21MC;FDEP21;FPER26;FPER26MC;FDEP26;FPER31;FPER31MC;FDEP31;FPER36;FPER36MC;FDEP36;FPER41;FPER41MC;FDEP41;REGDEMO;dalog;daocc;daori;daodes";
	String[] header = h.split(",");
	//String h ="ipere;idu;version;media_int;provenance;date_int;deb_interv;nolog;tlog;nbper;nbveh;revenu;langue;nmodif_ass;incaplogi;xmtmlog;ymtmlog;r6log;r8log;rmrlog;arrlog;sdrlog;srlog;sdomi65;smlog;cplog;agr_domi;jour;sem;facmen;facmen18;noper;tper;clepersonne;sexe;age;grpage;incap;percond;abon_ap;abon_vls;passetc;occper;tele_trav;lieuocc;xmtmocc;ymtmocc;rmrocc;arrocc;sdrocc;srocc;smocc;cpocc;l_id_local;mobil;facper;facper18;facdep18;cledeplacement;nodep;no_rep;hredep;ghredep;motif;motif_grp;date_dpl;jour_dpl;mode1;mode2;mode3;mode4;mode5;mode6;mode7;mode8;seq_modes;covoiturage;pers_auto;auto_ap;velo_ls;station_p;pont1;pont2;mode_exclu;lig1;lig2;lig3;lig4;lig5;lig_imp;metro1;metro2;metro3;metro4;gare1;gare2;gare3;gare4;aut1;aut2;aut3;aut4;aut5;aut6;aut7;aut8;aut9;aut10;aut11;aut12;trace1;trace2;trace3;trace4;trace5;dist;agr_ori;xmtmori;ymtmori;xlonori;ylatori;r6orig;r8orig;arrori;sdrori;srori;sorig65;smori;cpori;o_id_local;inodo;loccodeo;xyo;agr_des;xmtmdes;ymtmdes;xlondes;ylatdes;r6dest;r8dest;arrdes;sdrdes;srdes;sdest65;smdes;cpdes;d_id_local;inodd;loccoded;xyd;Depl_Excl;nbjct;typjct1;typjct1_98;stajct1;agr_jct1;j_id_local1;xmtmjct1;ymtmjct1;xlonjct1;ylatjct1;r6jct1;r8jct1;arrjct1;sdrjct1;srjct1;sjonc65_1;smjct1;inodj1;loccodej1;nchangzone1;xmtmjct2;ymtmjct2;xlonjct2;ylatjct2;r6jct2;r8jct2;arrjct2;sdrjct2;srjct2;sjonc652;smjct2;inodj2;loccodej2;nchangzone2;tc;actif;autre_mot;motorise;metro;stm;rtl;stl;auto;autop;autoc;mauto;autrebus;busscol;taxi;train;a_pied;moto;velo;cit;au_mode;tr_adap;indet1";
	//String[] header = h.split(";");
	
	Reader in = new FileReader(odFileLocation);

    @SuppressWarnings("deprecation")
	CSVFormat csvFormat = CSVFormat.newFormat(';')
        .withHeader();

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
        			record.get("dalog").equals("")?null:Double.parseDouble(record.get("dalog")), 
        			Double.parseDouble(record.get(hhEFKey).replace(",", ".")), 
        			false, Integer.parseInt(record.get("nbveh")));
        	households.put(hh.getHhId(), hh);
        	hh.setMemSize(Integer.parseInt(record.get("nbper")));
        }
        
        HouseHold hh = households.get(hhId);
        Member member = hh.getMembers().get(memId);
        if(member==null) {
        	member = new Member(memId.toString(), hh, Integer.parseInt(record.get("age")),Integer.parseInt(record.get("percond"))==1, Double.parseDouble(record.get(mmEFKey).replace(",", ".")),
        			hh.getIncomeGroup(), Integer.parseInt(record.get("sexe")), Integer.parseInt(record.get("occper")), Integer.parseInt(record.get("tele_trav"))==1, 
        			record.get("xmtmocc").equals("")?null:Double.parseDouble(record.get("xmtmocc")), record.get("ymtmocc").equals("")?null:Double.parseDouble(record.get("ymtmocc")),record.get("daocc").equals("")?null:Double.parseDouble(record.get("daocc")));
        	hh.addMember(member);
        	if(member.getAgeGroup()<15)hh.setIfKids(true);
        }
        if(tripNo!=null && !tripNo.equals("")) {
        	double timehhmm = Double.parseDouble(record.get("hredep"));
        	double time = (int)timehhmm/100*3600+timehhmm%100*60;
        	CoordinateTransformation tsf = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "epsg:32188");
        	Trip trip = new Trip(tripId.toString(), member, Double.parseDouble(record.get("xlonori").replace(",", ".")), Double.parseDouble(record.get("ylatori").replace(",", ".")), Double.parseDouble(record.get("xlondes").replace(",", ".")), 
        			Double.parseDouble(record.get("ylatdes").replace(",", ".")),
        			time,record.get(tpEFKey).equals("")?member.getPersonExFac():Double.parseDouble(record.get(tpEFKey).replace(",", ".")), 
        			record.get("daori").equals("")?null:Double.parseDouble(record.get("daori")), 
        			record.get("dades").equals("")?null:Double.parseDouble(record.get("dades")),
        			extractActivity(record),
        			Integer.parseInt(record.get("mobil")),
        			extractModes(record), record.get("jour_dpl"), tsf,odNet);
        	
//        	Trip trip = new Trip(tripId.toString(), member, Double.parseDouble(record.get("xmtmori")), Double.parseDouble(record.get("ymtmori")), Double.parseDouble(record.get("xmtmdes")), 
//        			Double.parseDouble(record.get("ymtmdes")),
//        			time,record.get("facdep18").equals("")?member.getPersonExFac():Double.parseDouble(record.get("facdep18").replace(",", ".")), 
//        			record.get("srori").equals("")?null:Double.parseDouble(record.get("srori")), 
//        			record.get("srdes").equals("")?null:Double.parseDouble(record.get("srdes")),
//        			extractActivity(record),
//        			Integer.parseInt(record.get("mobil")),
//        			extractModes(record), record.get("jour_dpl"));
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
	handleNullOriginAndDestination(households);
	writeTreatedCTForTrips(households,"data/stat");
    households.values().forEach(hh->{

    	hh.loadClonedHouseHoldPersonAndVehicle(population, vehicles, matsimHouseholds, fac, ctuidToFacilityMap, scale, hhSpare, memberSpare, tripSpare, timeToSpread, MatsimRandom.getRandom());
    });
    
    
    
    config.qsim().setStartTime(0);
    config.qsim().setEndTime(27*3600);
    for(Person p:population.getPersons().values()) {
		((Activity)p.getSelectedPlan().getPlanElements().get(0)).setStartTime(config.qsim().getStartTime().seconds());
		((Activity)p.getSelectedPlan().getPlanElements().get(p.getSelectedPlan().getPlanElements().size()-1)).setEndTime(config.qsim().getEndTime().seconds());
		for(int i = 2;i<p.getSelectedPlan().getPlanElements().size();i++) {
			Activity previousAct = (Activity)p.getSelectedPlan().getPlanElements().get(0);
			if(p.getSelectedPlan().getPlanElements().get(i) instanceof Activity) {
				Activity a = (Activity) p.getSelectedPlan().getPlanElements().get(i);
				a.setStartTime(Math.min(previousAct.getEndTime().seconds()+900,a.getEndTime().seconds()));
			}
		}
	}
    
    new PopulationWriter(population).write("data/outputODPopulation_"+year+"_"+scale+".xml.gz");
    new MatsimVehicleWriter(vehicles).writeFile("data/outputODVehicle_"+year+"_"+scale+".xml.gz");
    new HouseholdsWriterV10(matsimHouseholds).writeFile("data/outputODHouseholds_"+year+"_"+scale+".xml.gz");
    new FacilitiesWriter(fac).write("data/outputODFacilities"+year+"_"+scale+".xml.gz");
    
    writeGender(population, households, scale, "data/stat");
    writeAge(population, households, scale, "data/stat");
    writeMotive(population, households, scale, "data/stat");
    writeMode(population, households, scale, "data/stat");
    writeFromToActivity(population, households, scale, "data/stat");
    writeDepartureTimeDistribution(population, households, scale, "data/stat");
    writeOriginCTDemand(population, households, fac, scale, "data/stat");
    writeDestinationCTDemand(population, households, fac, scale, "data/stat");
    writeActivitySpecificOriginCTDemand(population, households, fac, scale,"work", "data/stat");
    writeActivitySpecificDestinationCTDemand(population, households, fac, scale,"work", "data/stat");
    writeODCTDemand(population, households, fac, scale, "data/stat");
    writeOriginalOriginCTForODActivity(households, scale,"work","data/stat");
    writeOriginalDestinationCTForODActivity(households, scale,"work","data/stat");
}

public static void writeTreatedCTForTrips(Map<Id<HouseHold>,HouseHold> hhs, String folderLocation) {
	FileWriter fw;
	try {
		fw = new FileWriter(new File(folderLocation+"/treatedTripCT.csv"));
		fw.append("tripId,oCT,dCT\n");
		hhs.entrySet().stream().forEach(h->{
			h.getValue().getMembers().entrySet().forEach(m->{
				m.getValue().getTrips().entrySet().forEach(t->{
					try {
						fw.append(t.getKey().toString()+","+t.getValue().getOriginCT()+","+t.getValue().getDestinationCT()+"\n");
						fw.flush();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				});
			});
		});
		fw.close();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	
}

public static String[] extractModes(CSVRecord record) {
	List<String> modes = new ArrayList<>();
	for(int i = 1;i<9;i++) {
		String modeString = "mode"+i;
		if(record.get(modeString)!=null && !record.get(modeString).equals("")) {
			int mode = 0;
			if(record.get(modeString).contains(".")) {
				mode = (int)Double.parseDouble(record.get(modeString));
			}else {
				mode =Integer.parseInt(record.get(modeString));
			}
			switch(mode) {
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
		int motive = 0;
		if(record.get("motif").contains(".")) {
			motive = (int)Double.parseDouble(record.get("motif"));
		}else {
			motive = Integer.parseInt(record.get("motif"));
		}
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


public static void writeGender(Population population, Map<Id<HouseHold>,HouseHold> hhs,double scale,String outFile) {
	try {
		FileWriter fw = new FileWriter(new File(outFile+"/gender.csv"));
		fw.append("Source,Male,Female\n");
		Map<Integer,Integer> fromPopulation = new HashMap<>();
		Map<Integer,Double> fromOD = new HashMap<>();
		for(Person p:population.getPersons().values()) {
			if(!p.getAttributes().getAttribute("personTyp").equals("tripPerson")) {
				int gender = (int) p.getAttributes().getAttribute("gender");
				if(!fromPopulation.containsKey(gender))fromPopulation.put(gender, 0);
				fromPopulation.put(gender,fromPopulation.get(gender)+1);
			}
		}
		hhs.values().forEach(h->{
			h.getMembers().values().forEach(m->{
				if(!fromOD.containsKey(m.getGender()))fromOD.put(m.getGender(), 0.);
				fromOD.put(m.getGender(), fromOD.get(m.getGender())+m.getPersonExFac()*scale);
			});
		});
		fw.append("synthetic,"+fromPopulation.get(1)+","+fromPopulation.get(2)+"\n");
		fw.append("OD,"+fromOD.get(1)+","+fromOD.get(2)+"\n");
		fw.flush();
		fw.close();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}

public static void writeAge(Population population, Map<Id<HouseHold>,HouseHold> hhs,double scale,String outFile) {
	try {
		FileWriter fw = new FileWriter(new File(outFile+"/age.csv"));
		fw.append("Source,5-15,16-25,26-35,36-45,46-55,56-65,66-75,75-100\n");
		Map<String,Integer> fromPopulation = new HashMap<>();
		Map<String,Double> fromOD = new HashMap<>();
		String[] ageBin = new String[] {"5-15","16-25","26-35","36-45","46-55","56-65","66-75","75-100"};
		for(Person p:population.getPersons().values()) {
			if(!p.getAttributes().getAttribute("personTyp").equals("tripPerson")) {
				int age = (int) p.getAttributes().getAttribute("age");
				for(String s:ageBin) {
					int ageLower = Integer.parseInt(s.split("-")[0]);
					int ageUpper = Integer.parseInt(s.split("-")[1]);
					if(age>=ageLower && age<=ageUpper) {
						if(!fromPopulation.containsKey(s))fromPopulation.put(s, 0);
						fromPopulation.put(s,fromPopulation.get(s)+1);
					}
				}
			}
		}
		hhs.values().forEach(h->{
			h.getMembers().values().forEach(m->{
				for(String s:ageBin) {
					int ageLower = Integer.parseInt(s.split("-")[0]);
					int ageUpper = Integer.parseInt(s.split("-")[1]);
					if(m.getAgeGroup()>=ageLower && m.getAgeGroup()<=ageUpper) {
						if(!fromOD.containsKey(s))fromOD.put(s, 0.);
						fromOD.put(s,fromOD.get(s)+m.getPersonExFac()*scale);
					}
				}
			});
		});
		fw.append("synthetic");
		for(String s:ageBin)fw.append(","+fromPopulation.get(s));
		fw.append("\n");
		fw.append("OD");
		for(String s:ageBin)fw.append(","+fromOD.get(s));
		fw.append("\n");
		fw.flush();
		fw.close();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}

public static void writeMotive(Population population, Map<Id<HouseHold>,HouseHold> hhs,double scale,String outFile) {
	try {
		FileWriter fw = new FileWriter(new File(outFile+"/activities.csv"));
		fw.append("Source,work,education,shop,errands,home,leisure,other\n");
		
		Map<String,Integer> fromPopulation = new HashMap<>();
		Map<String,Double> fromOD = new HashMap<>();
		String[] acts = new String[] {"work","education","shop","errands","home","leisure","other"};
		
		for(Person p:population.getPersons().values()) {
			int i = 0;
			for(PlanElement pe: p.getSelectedPlan().getPlanElements()) {
				if(i!=0 && pe instanceof Activity) {
					fromPopulation.compute(((Activity)pe).getType(),(k,v)->v==null?1:v+1);
				}
				i++;
			}
		}
		hhs.values().forEach(h->{
			h.getMembers().values().forEach(m->{
				m.getTrips().values().forEach(t->{
					fromOD.compute(t.getMotive(), (k,v)->v==null?t.getTripExpFactror()*scale:v+t.getTripExpFactror()*scale);
				});
			});
		});
		fw.append("synthetic");
		for(String s:acts)fw.append(","+fromPopulation.get(s));
		fw.append("\n");
		fw.append("OD");
		for(String s:acts)fw.append(","+fromOD.get(s));
		fw.append("\n");
		fw.flush();
		fw.close();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}

public static void writeMode(Population population, Map<Id<HouseHold>,HouseHold> hhs,double scale,String outFile) {
	try {
		FileWriter fw = new FileWriter(new File(outFile+"/modes.csv"));
		fw.append("Source,car,car_passenger,pt,bike,walk\n");
		
		Map<String,Integer> fromPopulation = new HashMap<>();
		Map<String,Double> fromOD = new HashMap<>();
		String[] acts = new String[] {"car","car_passenger","pt","bike","walk"};
		
		for(Person p:population.getPersons().values()) {
			int i = 0;
			for(PlanElement pe: p.getSelectedPlan().getPlanElements()) {
				if(pe instanceof Leg) {
					fromPopulation.compute(((Leg)pe).getMode(),(k,v)->v==null?1:v+1);
				}
				i++;
			}
		}
		hhs.values().forEach(h->{
			h.getMembers().values().forEach(m->{
				m.getTrips().values().forEach(t->{
					fromOD.compute(t.getMode(), (k,v)->v==null?t.getTripExpFactror()*scale:v+t.getTripExpFactror()*scale);
				});
			});
		});
		fw.append("synthetic");
		for(String s:acts)fw.append(","+fromPopulation.get(s));
		fw.append("\n");
		fw.append("OD");
		for(String s:acts)fw.append(","+fromOD.get(s));
		fw.append("\n");
		fw.flush();
		fw.close();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}

public static void writeFromToActivity(Population population, Map<Id<HouseHold>,HouseHold> hhs,double scale,String outFile) {
	try {
		FileWriter fw = new FileWriter(new File(outFile+"/fromToActs.csv"));
		fw.append("type,synthetic,OD\n");
		Map<String,Double> fromOD = new HashMap<>();
		Map<String,Integer>fromPopulation = new HashMap<>();
		for(Person p:population.getPersons().values()) {
			int i = 0;
			for(PlanElement pe:p.getSelectedPlan().getPlanElements()) {
				if(pe instanceof Leg) {
					Activity a = (Activity)p.getSelectedPlan().getPlanElements().get(i-1);
					Activity b = (Activity)p.getSelectedPlan().getPlanElements().get(i+1);
					String fromTo = a.getType()+"_"+b.getType();
					fromPopulation.compute(fromTo, (k,v)->v==null?1:v+1);
				}
				i++;
			}
		}
		for(HouseHold h:hhs.values()) {
			for(Member m:h.getMembers().values()) {
				String from = "home";
				for(Trip t:m.getTrips().values()) {
					String fromTo = from+"_"+t.getMotive();
					fromOD.compute(fromTo, (k,v)->v==null?t.getTripExpFactror()*scale:v+t.getTripExpFactror()*scale);
					from = t.getMotive();
				}
			}
		}
		
		Set<String> cts = new HashSet<>(fromOD.keySet());
		cts.addAll(fromPopulation.keySet());
		
		for(String d:cts){
			if(!fromOD.containsKey(d))fromOD.put(d, 0.);
			if(!fromPopulation.containsKey(d))fromPopulation.put(d, 0);
			fw.append(d+","+fromPopulation.get(d)+","+fromOD.get(d)+"\n");
			fw.flush();
		}
		fw.close();
		
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}

public static void writeDepartureTimeDistribution(Population population, Map<Id<HouseHold>,HouseHold> hhs,double scale,String outFile) {
	try {
		FileWriter fw = new FileWriter(new File(outFile+"/departureTime.csv"));
		fw.append("time,synthetic,od\n");
		int[] timeBin = new int[] {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24};
		Map<Integer,Double> fromOD = new HashMap<>();
		Map<Integer,Integer>fromPopulation = new HashMap<>();
		for(Person p:population.getPersons().values()) {
			int i = 0;
			for(PlanElement pe:p.getSelectedPlan().getPlanElements()) {
				if(pe instanceof Activity && i!=p.getSelectedPlan().getPlanElements().size()-1) {
					Activity a = (Activity)pe;
					int time = 0;
					for(int j = 0;j<timeBin.length;j++) {
						double t = a.getEndTime().seconds();
						if(t==0)t=1;
						if(t/3600>timeBin[j]-1 && t/3600<=timeBin[j]) {
							time = timeBin[j];
							break;
						}
					}
					fromPopulation.compute(time, (k,v)->v==null?1:v+1);
				}
				i++;
			}
		}
		for(HouseHold h:hhs.values()) {
			for(Member m:h.getMembers().values()) {
				for(Trip t:m.getTrips().values()) {
					int time = 0;
					for(int j = 0;j<timeBin.length;j++) {
						double a = t.getDepartureTime();
						if(a==0)a=1;
						if(a/3600>timeBin[j]-1 && a/3600<=timeBin[j]) {
							time = timeBin[j];
							break;
						}
					}
					fromOD.compute(time, (k,v)->v==null?t.getTripExpFactror()*scale:v+t.getTripExpFactror()*scale);
				}
			}
		}
		
		
		for(int d:timeBin){
			
			fw.append(d+","+fromPopulation.get(d)+","+fromOD.get(d)+"\n");
			fw.flush();
		}
		fw.close();
		
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}


public static void writeOriginCTDemand(Population population, Map<Id<HouseHold>,HouseHold> hhs,ActivityFacilities facilities,double scale,String outFile) {
	try {
		FileWriter fw = new FileWriter(new File(outFile+"/originCT.csv"));
		fw.append("CTUID,Synthetic,OD\n");
		Map<Double,Double> fromOD = new HashMap<>();
		Map<Double,Integer>fromPopulation = new HashMap<>();
		for(Person p:population.getPersons().values()) {
			int i = 0;
			for(PlanElement pe:p.getSelectedPlan().getPlanElements()) {
				if(pe instanceof Leg) {
					Activity a = (Activity)p.getSelectedPlan().getPlanElements().get(i-1);
					double ct = (double) facilities.getFacilities().get(a.getFacilityId()).getAttributes().getAttribute("CTUID");
					fromPopulation.compute(ct, (k,v)->v==null?1:v+1);
				}
				i++;
			}
		}
		for(HouseHold h:hhs.values()) {
			for(Member m:h.getMembers().values()) {
				for(Trip t:m.getTrips().values()) {
					fromOD.compute(t.getOriginCT(), (k,v)->v==null?t.getTripExpFactror()*scale:v+t.getTripExpFactror()*scale);
				}
			}
		}
		
		Set<Double> cts = new HashSet<>(fromOD.keySet());
		cts.addAll(fromPopulation.keySet());
		
		for(Double d:cts){
			if(!fromOD.containsKey(d))fromOD.put(d, 0.);
			if(!fromPopulation.containsKey(d))fromPopulation.put(d, 0);
			fw.append(d+","+fromPopulation.get(d)+","+fromOD.get(d)+"\n");
			fw.flush();
		}
		fw.close();
		
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}

public static void writeActivitySpecificOriginCTDemand(Population population, Map<Id<HouseHold>,HouseHold> hhs,ActivityFacilities facilities,double scale,String activity, String outFile) {
	try {
		FileWriter fw = new FileWriter(new File(outFile+"/originCT_"+activity+".csv"));
		fw.append("CTUID,Synthetic,OD\n");
		Map<Double,Double> fromOD = new HashMap<>();
		Map<Double,Integer>fromPopulation = new HashMap<>();
		for(Person p:population.getPersons().values()) {
			int i = 0;
			for(PlanElement pe:p.getSelectedPlan().getPlanElements()) {
				if(pe instanceof Leg) {
					Activity a = (Activity)p.getSelectedPlan().getPlanElements().get(i-1);
					if(a.getType().equals(activity)) {
						double ct = (double) facilities.getFacilities().get(a.getFacilityId()).getAttributes().getAttribute("CTUID");
						fromPopulation.compute(ct, (k,v)->v==null?1:v+1);
					}
				}
				i++;
			}
		}
		for(HouseHold h:hhs.values()) {
			for(Member m:h.getMembers().values()) {
				for(Trip t:m.getTrips().values()) {
					if(t.getMotive().equals(activity)) {
						fromOD.compute(t.getOriginCT(), (k,v)->v==null?t.getTripExpFactror()*scale:v+t.getTripExpFactror()*scale);
					}
				}
			}
		}
		
		Set<Double> cts = new HashSet<>(fromOD.keySet());
		cts.addAll(fromPopulation.keySet());
		
		for(Double d:cts){
			if(!fromOD.containsKey(d))fromOD.put(d, 0.);
			if(!fromPopulation.containsKey(d))fromPopulation.put(d, 0);
			fw.append(d+","+fromPopulation.get(d)+","+fromOD.get(d)+"\n");
			fw.flush();
		}
		fw.close();
		
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}

public static void writeDestinationCTDemand(Population population, Map<Id<HouseHold>,HouseHold> hhs,ActivityFacilities facilities, double scale, String outFile) {
	try {
		FileWriter fw = new FileWriter(new File(outFile+"/destinationCT.csv"));
		fw.append("CTUID,Synthetic,OD\n");
		Map<Double,Double> fromOD = new HashMap<>();
		Map<Double,Integer>fromPopulation = new HashMap<>();
		for(Person p:population.getPersons().values()) {
			int i = 0;
			for(PlanElement pe:p.getSelectedPlan().getPlanElements()) {
				if(pe instanceof Leg) {
					Activity a = (Activity)p.getSelectedPlan().getPlanElements().get(i+1);
					double ct = (double) facilities.getFacilities().get(a.getFacilityId()).getAttributes().getAttribute("CTUID");
					fromPopulation.compute(ct, (k,v)->v==null?1:v+1);
				}
				i++;
			}
		}
		for(HouseHold h:hhs.values()) {
			for(Member m:h.getMembers().values()) {
				for(Trip t:m.getTrips().values()) {
					fromOD.compute(t.getDestinationCT(), (k,v)->v==null?t.getTripExpFactror()*scale:v+t.getTripExpFactror()*scale);
				}
			}
		}
		
		Set<Double> cts = new HashSet<>(fromOD.keySet());
		cts.addAll(fromPopulation.keySet());
		
		for(Double d:cts){
			if(!fromOD.containsKey(d))fromOD.put(d, 0.);
			if(!fromPopulation.containsKey(d))fromPopulation.put(d, 0);
			fw.append(d+","+fromPopulation.get(d)+","+fromOD.get(d)+"\n");
			fw.flush();
		}
		fw.close();
		
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}

public static void writeActivitySpecificDestinationCTDemand(Population population, Map<Id<HouseHold>,HouseHold> hhs,ActivityFacilities facilities, double scale,String activity, String outFile) {
	try {
		FileWriter fw = new FileWriter(new File(outFile+"/destinationCT_"+activity+".csv"));
		fw.append("CTUID,Synthetic,OD\n");
		Map<Double,Double> fromOD = new HashMap<>();
		Map<Double,Integer>fromPopulation = new HashMap<>();
		for(Person p:population.getPersons().values()) {
			int i = 0;
			for(PlanElement pe:p.getSelectedPlan().getPlanElements()) {
				if(pe instanceof Leg) {
					Activity a = (Activity)p.getSelectedPlan().getPlanElements().get(i+1);
					if(a.getType().equals(activity)) {
						double ct = (double) facilities.getFacilities().get(a.getFacilityId()).getAttributes().getAttribute("CTUID");
						fromPopulation.compute(ct, (k,v)->v==null?1:v+1);
					}
				}
				i++;
			}
		}
		for(HouseHold h:hhs.values()) {
			for(Member m:h.getMembers().values()) {
				for(Trip t:m.getTrips().values()) {
					if(t.getMotive().equals(activity)) {
						fromOD.compute(t.getDestinationCT(), (k,v)->v==null?t.getTripExpFactror()*scale:v+t.getTripExpFactror()*scale);
					}
				}
			}
		}
		
		Set<Double> cts = new HashSet<>(fromOD.keySet());
		cts.addAll(fromPopulation.keySet());
		
		for(Double d:cts){
			if(!fromOD.containsKey(d))fromOD.put(d, 0.);
			if(!fromPopulation.containsKey(d))fromPopulation.put(d, 0);
			fw.append(d+","+fromPopulation.get(d)+","+fromOD.get(d)+"\n");
			fw.flush();
		}
		fw.close();
		
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}
public static void writeODCTDemand(Population population, Map<Id<HouseHold>,HouseHold> hhs,ActivityFacilities facilities, double scale, String outFile) {
	try {
		FileWriter fw = new FileWriter(new File(outFile+"/odCT.csv"));
		fw.append("oCTUID_dCTUID,Synthetic,OD\n");
		Map<String,Double> fromOD = new HashMap<>();
		Map<String,Integer>fromPopulation = new HashMap<>();
		for(Person p:population.getPersons().values()) {
			int i = 0;
			for(PlanElement pe:p.getSelectedPlan().getPlanElements()) {
				if(pe instanceof Leg) {
					Activity a = (Activity)p.getSelectedPlan().getPlanElements().get(i-1);
					Activity b = (Activity)p.getSelectedPlan().getPlanElements().get(i+1);
					double oct = (double) facilities.getFacilities().get(a.getFacilityId()).getAttributes().getAttribute("CTUID");
					double bct = (double) facilities.getFacilities().get(b.getFacilityId()).getAttributes().getAttribute("CTUID");
					fromPopulation.compute(oct+"_"+bct, (k,v)->v==null?1:v+1);
				}
				i++;
			}
		}
		for(HouseHold h:hhs.values()) {
			for(Member m:h.getMembers().values()) {
				for(Trip t:m.getTrips().values()) {
					fromOD.compute(t.getOriginCT()+"_"+t.getDestinationCT(), (k,v)->v==null?t.getTripExpFactror()*scale:v+t.getTripExpFactror()*scale);
				}
			}
		}
		
		Set<String> cts = new HashSet<>(fromOD.keySet());
		cts.addAll(fromPopulation.keySet());
		
		for(String d:cts){
			if(!fromOD.containsKey(d))fromOD.put(d, 0.);
			if(!fromPopulation.containsKey(d))fromPopulation.put(d, 0);
			fw.append(d+","+fromPopulation.get(d)+","+fromOD.get(d)+"\n");
			fw.flush();
		}
		fw.close();
		
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}
public static void writeOriginalOriginCTForOD(Map<Id<HouseHold>,HouseHold> hhs,double scale, String outFile) {
	try {
		FileWriter fw = new FileWriter(new File(outFile+"/originalOCTdemandOD.csv"));
		fw.append("oCTUID,OD\n");
		Map<Double,Double> fromOD = new HashMap<>();
		for(HouseHold h:hhs.values()) {
			for(Member m:h.getMembers().values()) {
				for(Trip t:m.getTrips().values()) {
					fromOD.compute(t.getOriginalOCT(), (k,v)->v==null?t.getTripExpFactror()*scale:v+t.getTripExpFactror()*scale);
				}
			}
		}
	
	for(Double d:fromOD.keySet()){
		
		fw.append(d+","+fromOD.get(d)+"\n");
		fw.flush();
	}
	fw.close();
	
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}

public static void writeOriginalDestinationCTForOD(Map<Id<HouseHold>,HouseHold> hhs,double scale, String outFile) {
	try {
		FileWriter fw = new FileWriter(new File(outFile+"/originalDCTdemandOD.csv"));
		fw.append("oCTUID,OD\n");
		Map<Double,Double> fromOD = new HashMap<>();
		for(HouseHold h:hhs.values()) {
			for(Member m:h.getMembers().values()) {
				for(Trip t:m.getTrips().values()) {
					fromOD.compute(t.getOriginalDCT(), (k,v)->v==null?t.getTripExpFactror()*scale:v+t.getTripExpFactror()*scale);
				}
			}
		}
	
	for(Double d:fromOD.keySet()){
		
		fw.append(d+","+fromOD.get(d)+"\n");
		fw.flush();
	}
	fw.close();
	
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}

public static void writeOriginalOriginCTForODActivity(Map<Id<HouseHold>,HouseHold> hhs,double scale,String act, String outFile) {
	try {
		FileWriter fw = new FileWriter(new File(outFile+"/originalOCTdemandOD_"+act+".csv"));
		fw.append("oCTUID,OD\n");
		Map<Double,Double> fromOD = new HashMap<>();
		for(HouseHold h:hhs.values()) {
			for(Member m:h.getMembers().values()) {
				for(Trip t:m.getTrips().values()) {
					if(t.getMotive().equals(act)) {
					fromOD.compute(t.getOriginalOCT(), (k,v)->v==null?t.getTripExpFactror()*scale:v+t.getTripExpFactror()*scale);
					}
				}
			}
		}
	
	for(Double d:fromOD.keySet()){
		
		fw.append(d+","+fromOD.get(d)+"\n");
		fw.flush();
	}
	fw.close();
	
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}

public static void writeOriginalDestinationCTForODActivity(Map<Id<HouseHold>,HouseHold> hhs,double scale,String act, String outFile) {
	try {
		FileWriter fw = new FileWriter(new File(outFile+"/originalDCTdemandOD_"+act+".csv"));
		fw.append("oCTUID,OD\n");
		Map<Double,Double> fromOD = new HashMap<>();
		for(HouseHold h:hhs.values()) {
			for(Member m:h.getMembers().values()) {
				for(Trip t:m.getTrips().values()) {
					if(t.getMotive().equals(act)) {
						fromOD.compute(t.getOriginalDCT(), (k,v)->v==null?t.getTripExpFactror()*scale:v+t.getTripExpFactror()*scale);
					}
				}
			}
		}
	
	for(Double d:fromOD.keySet()){
		
		fw.append(d+","+fromOD.get(d)+"\n");
		fw.flush();
	}
	fw.close();
	
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}

public static void handleNullOriginAndDestination(Map<Id<HouseHold>,HouseHold> hhs) {
	Map<Double,Multinomial<Double>> originToDestinationDistribution = new HashMap<>();
	Map<Double,Map<Double,Double>> otodDistribution = new HashMap<>();
	
	Map<Double,Multinomial<Double>> destinationToOriginDistribution = new HashMap<>();
	Map<Double,Map<Double,Double>> dtooDistribution = new HashMap<>();
	
	Map<String,Double> odDistribution = new HashMap<>();
	Multinomial<String> originDestinationDistribution = new Multinomial<String>();
	
	hhs.values().forEach(h->{
		h.getMembers().values().forEach(m->{
			m.getTrips().values().forEach(t->{
				if(t.getOriginCT()!=null && t.getDestinationCT()!=null) {
					if(!otodDistribution.containsKey(t.getOriginCT())) {
						otodDistribution.put(t.getOriginCT(), new HashMap<>());	
					}
					
					if(!dtooDistribution.containsKey(t.getDestinationCT())) {
						dtooDistribution.put(t.getDestinationCT(), new HashMap<>());	
					}
					
					if(!odDistribution.containsKey(Double.toString(t.getOriginCT())+"_"+Double.toString(t.getDestinationCT()))) {
						odDistribution.put(Double.toString(t.getOriginCT())+"_"+Double.toString(t.getDestinationCT()),0.);
					};
					otodDistribution.get(t.getOriginCT()).compute(t.getDestinationCT(),(k,v)->v==null?t.getTripExpFactror():v+t.getTripExpFactror());
					dtooDistribution.get(t.getDestinationCT()).compute(t.getOriginCT(), (k,v)->v==null?t.getTripExpFactror():v+t.getTripExpFactror());
					odDistribution.compute(Double.toString(t.getOriginCT())+"_"+Double.toString(t.getDestinationCT()),(k,v)->v==null?t.getTripExpFactror():v+t.getTripExpFactror());
				}
			});
		});
	});
	
	otodDistribution.entrySet().forEach(otod->{
		originToDestinationDistribution.put(otod.getKey(), new Multinomial<Double>());
		otod.getValue().entrySet().forEach(a->originToDestinationDistribution.get(otod.getKey()).add(a.getKey(), a.getValue()));
	});
	
	dtooDistribution.entrySet().forEach(dtoo->{
		destinationToOriginDistribution.put(dtoo.getKey(), new Multinomial<Double>());
		dtoo.getValue().entrySet().forEach(a->destinationToOriginDistribution.get(dtoo.getKey()).add(a.getKey(), a.getValue()));
	});
	
	odDistribution.entrySet().forEach(a->{
		originDestinationDistribution.add(a.getKey(), a.getValue());
	});
	
	hhs.values().forEach(h->{
		h.getMembers().values().forEach(m->{
			m.getTrips().values().forEach(t->{
				if(t.getOriginCT()==null && t.getDestinationCT()!=null) {
					t.setOriginCT(destinationToOriginDistribution.get(t.getDestinationCT()).sample());
				}else if(t.getOriginCT()!=null && t.getDestinationCT()==null) {
					t.setDestinationCT(originToDestinationDistribution.get(t.getOriginCT()).sample());
				}else if(t.getOriginCT()==null && t.getDestinationCT()==null) {
					String od = originDestinationDistribution.sample();
					t.setOriginCT(Double.parseDouble(od.split("_")[0]));
					t.setDestinationCT(Double.parseDouble(od.split("_")[1]));
				}
			});
		});
	});
}



}
