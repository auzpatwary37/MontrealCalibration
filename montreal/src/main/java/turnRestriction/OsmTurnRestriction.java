package turnRestriction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.lanes.Lane;
import org.matsim.lanes.Lanes;
import org.matsim.lanes.LanesFactory;
import org.matsim.lanes.LanesToLinkAssignment;
import org.matsim.pt2matsim.osm.lib.Osm;
import org.matsim.pt2matsim.osm.lib.Osm.Element;
import org.matsim.pt2matsim.osm.lib.Osm.Relation;
import org.matsim.pt2matsim.osm.lib.Osm.Way;

public class OsmTurnRestriction {
	private Id<OsmTurnRestriction> restrictionId;
	private String fromWayId;
	private String toWayId;
	private List<String> viaWayIds;
	
	private Id<Link> fromLinkId;
	private Id<Link> toLinkId;
	private List<Id<Link>> viaLinkIds = new ArrayList<>();;
	private String attribute;
	boolean active = false;
	
	public OsmTurnRestriction(Relation relation) {
		if(!relation.getTags().get(Osm.Key.TYPE).equals(Osm.Key.RESTRICTION))throw new IllegalArgumentException("Unrecognized relation");
		String fromWay = null;
		String toWay = null;
		List<String> viaWays = new ArrayList<>();
		for(Element m:relation.getMembers()){
			if(m instanceof Way) {
				String s = relation.getMemberRole(m);
				if(s.equals(Osm.Key.FROM))fromWay = ((Way)m).getId().toString();
				if(s.equals(Osm.Key.TO))toWay = ((Way)m).getId().toString();
				if(s.equals(Osm.Key.VIA))viaWays.add(((Way)m).getId().toString());
			}
		}
		if(relation.getTags().get(Osm.Key.RESTRICTION)!=null && relation.getTags().get(Osm.Key.RESTRICTION).contains("no_u_turn")||
				relation.getTags().get(Osm.Key.combinedKey(Osm.Key.RESTRICTION,Osm.Key.CONDITIONAL))!=null && relation.getTags().get(Osm.Key.combinedKey(Osm.Key.RESTRICTION,Osm.Key.CONDITIONAL)).contains("no_u_turn")) {
			if(fromWay == null && toWay != null) {
				fromWay = toWay;
			}else if(fromWay!=null && toWay==null) {
				toWay = fromWay;
			}
		}
		
		String id = fromWay+"_"+toWay;
		for(String l:viaWays)id=id+"_"+l;
		this.restrictionId = Id.create(id, OsmTurnRestriction.class);
		this.fromWayId = fromWay;
		this.toWayId = toWay;
		this.viaWayIds = viaWays;
		this.attribute = relation.getTags().get(Osm.Key.RESTRICTION);
	}
	
	public OsmTurnRestriction(String fromWayId, String toWayId, List<String> viaWayIds, String attribute) {
		String id = fromWayId+"_"+toWayId;
		for(String l:viaWayIds)id=id+"_"+l;
		this.restrictionId = Id.create(id, OsmTurnRestriction.class);
		this.fromWayId = fromWayId;
		this.toWayId = toWayId;
		this.viaWayIds = viaWayIds;
		this.attribute = attribute;
	}
	
	public OsmTurnRestriction(String fromWayId, String toWayId, List<String> viaWayIds, Id<Link> fromLinkId, Id<Link> toLinkId, List<Id<Link>> viaLinkIds, String attribute) {
		String id = fromWayId+"_"+toWayId;
		for(String l:viaWayIds)id=id+"_"+l;
		this.restrictionId = Id.create(id, OsmTurnRestriction.class);
		this.fromWayId = fromWayId;
		this.toWayId = toWayId;
		this.viaWayIds = viaWayIds;
		this.fromLinkId = fromLinkId;
		this.toLinkId = toLinkId;
		this.viaLinkIds = viaLinkIds;
		this.attribute = attribute;
	}
	
	@Override
	public String toString() {
		String out = "";
		out = out+fromWayId+","+toWayId+",";
		for(String s:this.viaWayIds)out=out+s+",";
		out=out+"___";
		out = out+","+fromLinkId.toString()+","+toLinkId.toString();
		for(Id<Link>l:this.viaLinkIds) out = out+","+l.toString();
		out = out+"____"+attribute;
		return out;
	}
	
	public static OsmTurnRestriction parse(String line) {
		String[] parts = line.split(",");
		List<String> viaways = new ArrayList<>();
		List<Id<Link>> viaLinks = new ArrayList<>();
		String fromWay = null;
		String toWay = null;
		Id<Link> fromLink = null;
		Id<Link> toLink = null;
		String attribute = null;
		boolean isway = true;
		boolean isAtt = false;
		int i = 0;
		for(String s:parts) {
			if(s.equals("___")) {
				i=0;
				isway = false;
				continue;
			}else if(s.equals("____")) {
				isAtt = true;
				continue;
			}
			if(i == 0 && isway && !isAtt)fromWay = s;
			else if(i==1 && isway && !isAtt)toWay = s;
			else if(i>1 && isway && !isAtt) viaways.add(s);
			else if(i==0 && !isway && !isAtt) fromLink = Id.createLinkId(s);
			else if(i==1 && !isway && !isAtt) toLink = Id.createLinkId(s);
			else if(i>1 && !isway && !isAtt) viaLinks.add(Id.createLinkId(s));
			else if(isAtt) attribute = s;
			i++;
		}
		OsmTurnRestriction r = new OsmTurnRestriction(fromWay,toWay, viaways, fromLink,toLink, viaLinks, attribute);
		return r;
	}
	
	public void extractLinkIds(Network network) {
		if(fromWayId!=null && toWayId!=null) {
		List<String> ways = new ArrayList<>();
		ways.add(fromWayId);
		ways.addAll(viaWayIds);
		ways.add(toWayId);
		Link lastLink = null;
		for(Link l:network.getLinks().values()) {
			if(l.getAttributes().getAttribute("osm:way:id")!=null && Long.toString((long)l.getAttributes().getAttribute("osm:way:id")).equals(ways.get(0))) {
				for(Link ll:l.getToNode().getOutLinks().values()) {
					if(Long.toString((long)ll.getAttributes().getAttribute("osm:way:id")).equals(ways.get(1))) {
						this.fromLinkId = l.getId();
						this.viaLinkIds.add(ll.getId());
						lastLink = ll;
					}
				}
			}
		}
		
		for(int i = 2;i<ways.size();i++) {
			for(Link l:lastLink.getToNode().getOutLinks().values()) {
				if(Long.toString((long)l.getAttributes().getAttribute("osm:way:id")).equals(ways.get(i))) {
					if(i==ways.size()-1) {
						this.toLinkId = l.getId();
					}else {
						this.viaLinkIds.add(l.getId());
						lastLink = l;
					}
				}
			}
		}
		}
	}
	
	public boolean extractLinkIds(Network network,Map<Id<Link>,Id<Way>>mapping) {
		if(fromWayId!=null && toWayId!=null) {
			for(Id<Link>l:new HashSet<>(mapping.keySet())) {
				if(!network.getLinks().containsKey(l)) {
					mapping.remove(l);
				}
			}
			
			
			List<String> ways = new ArrayList<>();
			ways.add(fromWayId);
			ways.addAll(viaWayIds);
			ways.add(toWayId);
			
			Map<String,Set<Id<Link>>> waytoLink = new HashMap<>();
			for(Entry<Id<Link>, Id<Way>> d:mapping.entrySet()) {
				if(network.getLinks().containsKey(d.getKey()) && ways.contains(d.getValue().toString())) {
					if(!waytoLink.containsKey(d.getValue().toString()))waytoLink.put(d.getValue().toString(), new HashSet<>());
					waytoLink.get(d.getValue().toString()).add(d.getKey());
				}
			}
			boolean haveAllWay = true;
			for(String way:ways) {
				if(!waytoLink.containsKey(way)) {
					haveAllWay = false;
					break;
				}
			}
			if(!haveAllWay) {
				active = false;
				return false;
			}
			Link lastLink = null;
			for(Id<Link> lId:waytoLink.get(ways.get(0))) {
					Link l = network.getLinks().get(lId);
					for(Link ll:l.getToNode().getOutLinks().values()) {
						if(waytoLink.get(ways.get(1)).contains(ll.getId())) {
							this.fromLinkId = l.getId();
							if(ways.size()<=2)this.toLinkId = ll.getId();
							else this.viaLinkIds.add(ll.getId());
							lastLink = ll;
						}
					}
				}
			
			if(fromLinkId==null) {
				active = false;
				return false;
			}
			for(int i = 2;i<ways.size();i++) {
				for(Link l:lastLink.getToNode().getOutLinks().values()) {
					if(waytoLink.get(ways.get(i)).contains(l.getId())) {
						if(i==ways.size()-1) {
							this.toLinkId = l.getId();
						}else {
							this.viaLinkIds.add(l.getId());
							lastLink = l;
						}
					}
				}
			}
			if(toLinkId==null) {
				active = false;
				return false;
			}
			this.active = true;
			if(fromLinkId==null || toLinkId==null) {
				this.active = false;
				return false;
			}
			return true;
		}else {
			active = false;
			return false;
		}
	}
	
	public void applyRestriction(Network net, Lanes lanes, LanesFactory lFac) {
		if(active == true) {
			List<Id<Link>> chain = new ArrayList<>();
			chain.add(fromLinkId);
			chain.addAll(viaLinkIds);
			chain.add(toLinkId);
			for(int i = 0;i<chain.size()-1;i++) {
				if(i==chain.size()-2) {//The restriction
					LanesToLinkAssignment l2l = lanes.getLanesToLinkAssignments().get(chain.get(i));
					if(l2l==null) {
						l2l = lFac.createLanesToLinkAssignment(chain.get(i));
						Link fromLink = net.getLinks().get(chain.get(i));
						Link toLink = net.getLinks().get(chain.get(i+1));
						if(fromLink.getToNode().getOutLinks()!=null) {
						for(Link l:fromLink.getToNode().getOutLinks().values()){
							if(!l.getId().equals(toLink.getId())) {
								Lane lane = lFac.createLane(Id.create(fromLink.getId().toString()+"_"+l.getId().toString(), Lane.class));
								lane.setCapacityVehiclesPerHour(1800);
								lane.setStartsAtMeterFromLinkEnd(50);
								lane.addToLinkId(l.getId());
								l2l.addLane(lane);
							}
						}
						}
						if(l2l.getLanes()!=null) {
							lanes.addLanesToLinkAssignment(l2l);
						}
					}else {
						Id<Link> toLink = chain.get(i+1);
						for(Lane l:new HashSet<>(l2l.getLanes().values())){
							if(l.getToLinkIds().contains(toLink))l.getToLinkIds().remove(toLink);
							if(l.getToLinkIds().isEmpty())l2l.getLanes().remove(l.getId());
						}
					}
				}else{//the continuation
					LanesToLinkAssignment l2l = lanes.getLanesToLinkAssignments().get(chain.get(i));
					if(l2l!=null){
						boolean connected = false;
						for(Lane l:l2l.getLanes().values()){
							if(l.getToLinkIds().contains(chain.get(i+1))) {
								connected = true;
							}
						}
						if(!connected) {
							Lane lane = lFac.createLane(Id.create(chain.get(i).toString()+"_"+chain.get(i+1).toString(), Lane.class));
							lane.setCapacityVehiclesPerHour(1800);
							lane.setStartsAtMeterFromLinkEnd(50);
							lane.addToLinkId(chain.get(i+1));
							l2l.addLane(lane);
						}
					}
				}
			}
		}
	}

	public Id<OsmTurnRestriction> getRestrictionId() {
		return restrictionId;
	}

	public void setRestrictionId(Id<OsmTurnRestriction> restrictionId) {
		this.restrictionId = restrictionId;
	}

	public String getFromWayId() {
		return fromWayId;
	}

	public void setFromWayId(String fromWayId) {
		this.fromWayId = fromWayId;
	}

	public String getToWayId() {
		return toWayId;
	}

	public void setToWayId(String toWayId) {
		this.toWayId = toWayId;
	}

	public List<String> getViaWayIds() {
		return viaWayIds;
	}

	public void setViaWayIds(List<String> viaWayIds) {
		this.viaWayIds = viaWayIds;
	}

	public Id<Link> getFromLinkId() {
		return fromLinkId;
	}

	public void setFromLinkId(Id<Link> fromLinkId) {
		this.fromLinkId = fromLinkId;
	}

	public Id<Link> getToLinkId() {
		return toLinkId;
	}

	public void setToLinkId(Id<Link> toLinkId) {
		this.toLinkId = toLinkId;
	}

	public List<Id<Link>> getViaLinkIds() {
		return viaLinkIds;
	}

	public void setViaLinkIds(List<Id<Link>> viaLinkIds) {
		this.viaLinkIds = viaLinkIds;
	}
	
}
