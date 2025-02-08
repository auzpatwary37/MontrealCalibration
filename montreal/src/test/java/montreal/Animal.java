package montreal;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.MatsimFacilitiesReader;

public class Animal {
	public static void main(String[] args) {
		Population pop = PopulationUtils.readPopulation("data\\outputODPopulation_41_1.0.xml.gz");
		System.out.println(pop.getPersons().size());
//		Scenario scn = ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		new MatsimFacilitiesReader(scn).readFile("output_facilities.xml 1.gz");
//		ActivityFacilities fac = scn.getActivityFacilities();
//		new FacilitiesWriterV1(TransformationFactory.getCoordinateTransformation("epsg:32188", "epsg:32188"), fac).write("output_facilitiesV1_.xml.gz");
		
	}

}
