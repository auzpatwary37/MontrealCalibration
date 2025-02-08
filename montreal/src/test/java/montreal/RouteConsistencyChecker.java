package montreal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.Lane;
import org.matsim.lanes.LanesToLinkAssignment;

import com.google.inject.Inject;

public class RouteConsistencyChecker implements BeforeMobsimListener{

	@Inject
	private Scenario scneario;
	private Network net = this.scneario.getNetwork();
	private Map<Id<Link>,Set<Id<Link>>> prohibitedTurns = new HashMap<>();
	private int tripsWithProhibitedTurns = 0;

	@Inject
	RouteConsistencyChecker() {
		for(LanesToLinkAssignment l2l:this.scneario.getLanes().getLanesToLinkAssignments().values()) {
			prohibitedTurns.put(l2l.getLinkId(), new HashSet<>());
			Set<Id<Link>> permittedLinks = new HashSet<>();
			for(Lane lane: l2l.getLanes().values()) {
				permittedLinks.addAll(lane.getToLinkIds());
			}
			for(Id<Link> l:net.getLinks().get(l2l.getLinkId()).getToNode().getOutLinks().keySet()) {
				if(!permittedLinks.contains(l))prohibitedTurns.get(l2l.getLinkId()).add(l);
			}
		}
	}
	
	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		tripsWithProhibitedTurns = 0;
		this.scneario.getPopulation().getPersons().values().forEach(p->{
			p.getSelectedPlan().getPlanElements().forEach(pl->{
				if(pl instanceof Leg) {
					Leg l = (Leg)pl;
					if(l.getRoute() instanceof NetworkRoute) {
						NetworkRoute r = (NetworkRoute) l.getRoute();
						List<Id<Link>> allTurns = new ArrayList<>();
						allTurns.add(r.getStartLinkId());
						allTurns.addAll(r.getLinkIds());
						allTurns.add(r.getEndLinkId());
						
						for(int i = 0; i<allTurns.size()-1;i++) {
							if(this.prohibitedTurns.get(allTurns.get(i)).contains(allTurns.get(i+1))) {
								System.out.println("Route contains prohibited turn!!!");
								tripsWithProhibitedTurns++;
							}
						}
					}
				}
			});
		});
		System.out.println("Total trips with prohibited turns = " + this.tripsWithProhibitedTurns);
	}
	
	public static void main(String[] args) {
		//sample connection
		
		Config config = ConfigUtils.createConfig();
		Scenario scn = ScenarioUtils.loadScenario(config);
		Controler c = new Controler(scn);
		c.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
				this.addControlerListenerBinding().to(RouteConsistencyChecker.class);
				this.bind(RouteConsistencyChecker.class).asEagerSingleton();
			}
			
		});
		}

}
