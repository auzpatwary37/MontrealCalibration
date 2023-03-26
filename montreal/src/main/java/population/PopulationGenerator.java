package population;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

public class PopulationGenerator {
public static void main(String[] args) throws IOException{
	String facilityFileLoc = "newData/osm_facilities.xml.gz";
	String odFileLocation = "EOD_from_Louiselle/EOD/mtl18pv2c.csv";
	String facilityWriteCsvLocation = "newData/facCoord.csv";
	String facilityToCTUIDMap = "newData/facToCTUID.csv";
	Map<Double,Map<String,Set<Id<ActivityFacility>>>> ctuidToFacilityMap = new HashMap<>();
	Config config = ConfigUtils.createConfig();
	config.facilities().setInputFile(facilityFileLoc);
	ActivityFacilities fac = ScenarioUtils.loadScenario(config).getActivityFacilities();
	BufferedReader bf = new BufferedReader(new FileReader(new File(facilityToCTUIDMap)));
	bf.readLine();
	String line = null;
	while((line = bf.readLine())!=null) {
		String[] part = line.split(",");
		Id<ActivityFacility> facId = Id.create(part[0], ActivityFacility.class);
		double ct = Double.parseDouble(part[1]);
		
		if(!ctuidToFacilityMap.containsKey(ct)){
			ctuidToFacilityMap.put(ct, new HashMap<>());
		}
		
		fac.getFacilities().get(facId).getActivityOptions().values().forEach(at->{
			if(!ctuidToFacilityMap.get(ct).containsKey(at.getType()))ctuidToFacilityMap.get(ct).put(at.getType(), new HashSet<>());
			ctuidToFacilityMap.get(ct).get(at.getType()).add(facId);
		});
	}
	
	String h ="ipere;idu;version;media_int;provenance;date_int;deb_interv;nolog;tlog;nbper;nbveh;revenu;langue;nmodif_ass;incaplogi;xmtmlog;ymtmlog;r6log;r8log;rmrlog;arrlog;sdrlog;srlog;sdomi65;smlog;cplog;agr_domi;jour;sem;facmen;facmen18;noper;tper;clepersonne;sexe;age;grpage;incap;percond;abon_ap;abon_vls;passetc;occper;tele_trav;lieuocc;xmtmocc;ymtmocc;rmrocc;arrocc;sdrocc;srocc;smocc;cpocc;l_id_local;mobil;facper;facper18;facdep18;cledeplacement;nodep;no_rep;hredep;ghredep;motif;motif_grp;date_dpl;jour_dpl;mode1;mode2;mode3;mode4;mode5;mode6;mode7;mode8;seq_modes;covoiturage;pers_auto;auto_ap;velo_ls;station_p;pont1;pont2;mode_exclu;lig1;lig2;lig3;lig4;lig5;lig_imp;metro1;metro2;metro3;metro4;gare1;gare2;gare3;gare4;aut1;aut2;aut3;aut4;aut5;aut6;aut7;aut8;aut9;aut10;aut11;aut12;trace1;trace2;trace3;trace4;trace5;dist;agr_ori;xmtmori;ymtmori;xlonori;ylatori;r6orig;r8orig;arrori;sdrori;srori;sorig65;smori;cpori;o_id_local;inodo;loccodeo;xyo;agr_des;xmtmdes;ymtmdes;xlondes;ylatdes;r6dest;r8dest;arrdes;sdrdes;srdes;sdest65;smdes;cpdes;d_id_local;inodd;loccoded;xyd;Depl_Excl;nbjct;typjct1;typjct1_98;stajct1;agr_jct1;j_id_local1;xmtmjct1;ymtmjct1;xlonjct1;ylatjct1;r6jct1;r8jct1;arrjct1;sdrjct1;srjct1;sjonc65_1;smjct1;inodj1;loccodej1;nchangzone1;xmtmjct2;ymtmjct2;xlonjct2;ylatjct2;r6jct2;r8jct2;arrjct2;sdrjct2;srjct2;sjonc652;smjct2;inodj2;loccodej2;nchangzone2;tc;actif;autre_mot;motorise;metro;stm;rtl;stl;auto;autop;autoc;mauto;autrebus;busscol;taxi;train;a_pied;moto;velo;cit;au_mode;tr_adap;indet1";
	String[] header = h.split(";");
	
	Reader in = new FileReader(odFileLocation);

    CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
        .setHeader(header)
        .setSkipHeaderRecord(true)
        .build();

    Iterable<CSVRecord> records = csvFormat.parse(in);
    Map<Id<HouseHold>,HouseHold> households = new HashMap<>();
    for (CSVRecord record : records) {
    	Id<HouseHold> hhId = Id.create(record.get("nolog"),HouseHold.class);
        Id<Member> memId = Id.create(hhId.toString()+record.get("noper"),Member.class);
        String tripNo = record.get("nodep");
        Id<Trip> tripId;
        if(tripNo!=null)tripId = Id.create(memId.toString()+tripNo, Trip.class);
        
        if(!households.containsKey(hhId)) {
        	HouseHold hh = new HouseHold(hhId.toString(), Integer.parseInt(record.get("revenu")), Double.parseDouble(record.get("xmtmlog")), Double.parseDouble(record.get("ymtmlog")), Double.parseDouble(record.get("srlog")), Double.parseDouble(record.get("facmen18")), false, Integer.parseInt(record.get("nbveh")));
        	households.put(hh.getHhId(), hh);
        }
        
        HouseHold hh = households.get(hhId);
        
        
    }
	
	 
}	
}
