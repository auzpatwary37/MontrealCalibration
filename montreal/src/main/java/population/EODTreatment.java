package population;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

public class EODTreatment {
	
	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		config.facilities().setInputFile("data/output_facilities.xml.gz");
		Scenario scn = ScenarioUtils.loadScenario(config);
		ActivityFacilities facs = scn.getActivityFacilities();
		try {
			FileWriter fw = new FileWriter(new File("data/facCoord.csv"));
			fw.append("facilityId,x,y\n");
			for(ActivityFacility f:facs.getFacilities().values()){
				fw.append(f.getId().toString()+","+f.getCoord().getX()+","+f.getCoord().getY()+"\n");
				fw.flush();
			}
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
