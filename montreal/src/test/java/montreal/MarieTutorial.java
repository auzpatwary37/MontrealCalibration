package montreal;

import java.util.ArrayList;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationUtils;

public class MarieTutorial {
public static void main(String[] args) {
	
	Population pop = PopulationUtils.readPopulation("5_percent/newData/output_plans.xml.gz");
	//reduceCarAvailability(pop,.4);
	
	
	System.out.println(pop.getPersons().size());
	cloneAndAddPerson(pop,1000);
	System.out.println(pop.getPersons().size());
	new PopulationWriter(pop).write("5_percent/newData/output_plans_1000MorePerson.xml.gz");
}

public static void cloneAndAddPerson(Population pop,int numberToAdd) {
	PopulationFactory popFac = pop.getFactory();
	Random r = MatsimRandom.getRandom();
	for(int i = 0;i<numberToAdd;i++) {
	
		Person person = pop.getPersons().get(new ArrayList<>(pop.getPersons().keySet()).get(r.nextInt(pop.getPersons().size())));
		Person newPerson = popFac.createPerson(Id.createPersonId(person.getId().toString()+"_clone_"+i));
		Plan plan = popFac.createPlan();
		PopulationUtils.copyFromTo(person.getSelectedPlan(), plan, false);
		newPerson.addPlan(plan);
		person.getAttributes().getAsMap().entrySet().forEach(e->{
			newPerson.getAttributes().putAttribute(e.getKey(), e.getValue());
		});
		pop.addPerson(newPerson);
	}
}

public static void reduceCarAvailability(Population pop, double reduceBy) {
	double proportionToReduce = reduceBy;
	Random random = MatsimRandom.getRandom();
	pop.getPersons().entrySet().forEach(p->{
		//PopulationUtils.putSubpopulation(p.getValue(),"withCar");
		String s = (String)p.getValue().getAttributes().getAttribute("carAvail");
		if(s.equals("sometimes")||s.equals("always")) {
			double d = random.nextDouble();
			if(d<proportionToReduce) {
				p.getValue().getAttributes().putAttribute("carAvail","never");
				//PopulationUtils.putSubpopulation(p.getValue(),"withoutCar");
				p.getValue().getSelectedPlan().getPlanElements().forEach(pl->{
					if(pl instanceof Leg && ((Leg)pl).getMode().equals("car")) {
						((Leg)pl).setMode("pt");
						((Leg)pl).setRoute(null);
					}
				});
			}
		}
	});
}

}
