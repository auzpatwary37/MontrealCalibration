package network;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

public class ExtractChanges {
	
	public static void main(String[] args) {
		Network beforeNet = NetworkUtils.readNetwork("data/kinan/emNetworkAm.xml");
		Network afterNet = NetworkUtils.readNetwork("data/kinan/emNetworkAm2041.xml");
		Network changedNet = NetworkUtils.createNetwork();
		NetworkFactory netFac = changedNet.getFactory();	
		//Additions 
		afterNet.getLinks().entrySet().forEach(l->{
			if(!beforeNet.getLinks().containsKey(l.getKey())) {
				Node fromNode = netFac.createNode(l.getValue().getFromNode().getId(), l.getValue().getFromNode().getCoord());
				Node toNode = netFac.createNode(l.getValue().getToNode().getId(), l.getValue().getToNode().getCoord());
				if(!changedNet.getNodes().containsKey(l.getValue().getFromNode().getId()))changedNet.addNode(fromNode);
				if(!changedNet.getNodes().containsKey(l.getValue().getToNode().getId()))changedNet.addNode(toNode);
				Link ll = netFac.createLink(l.getKey(), fromNode, toNode);
				ll.setAllowedModes(l.getValue().getAllowedModes());
				ll.setCapacity(l.getValue().getCapacity());
				ll.setFreespeed(l.getValue().getFreespeed());
				ll.setLength(l.getValue().getLength());
				ll.setNumberOfLanes(l.getValue().getNumberOfLanes());
				l.getValue().getAttributes().getAsMap().entrySet().forEach(a->{
					ll.getAttributes().putAttribute(a.getKey(), a.getValue());
				});
				ll.getAttributes().putAttribute("changeType","addition");
				changedNet.addLink(ll);
			}
		});
		
		//Additions 
		beforeNet.getLinks().entrySet().forEach(l->{
			if(!afterNet.getLinks().containsKey(l.getKey())) {
				Node fromNode = netFac.createNode(l.getValue().getFromNode().getId(), l.getValue().getFromNode().getCoord());
				Node toNode = netFac.createNode(l.getValue().getToNode().getId(), l.getValue().getToNode().getCoord());
				if(!changedNet.getNodes().containsKey(l.getValue().getFromNode().getId()))changedNet.addNode(fromNode);
				if(!changedNet.getNodes().containsKey(l.getValue().getToNode().getId()))changedNet.addNode(toNode);
				Link ll = netFac.createLink(l.getKey(), fromNode, toNode);
				ll.setAllowedModes(l.getValue().getAllowedModes());
				ll.setCapacity(l.getValue().getCapacity());
				ll.setFreespeed(l.getValue().getFreespeed());
				ll.setLength(l.getValue().getLength());
				ll.setNumberOfLanes(l.getValue().getNumberOfLanes());
				l.getValue().getAttributes().getAsMap().entrySet().forEach(a->{
					ll.getAttributes().putAttribute(a.getKey(), a.getValue());
				});
				ll.getAttributes().putAttribute("changeType","deletion");
				changedNet.addLink(ll);
			}
		});
		
		//changes 
		
//		afterNet.getLinks().entrySet().forEach(l->{
//			Link la = l.getValue();
//			Link lb = beforeNet.getLinks().get(l.getKey());
//			if(lb!=null && ifChange(la, lb)) {
//				Node fromNode = netFac.createNode(l.getValue().getFromNode().getId(), l.getValue().getFromNode().getCoord());
//				Node toNode = netFac.createNode(l.getValue().getToNode().getId(), l.getValue().getToNode().getCoord());
//				if(!changedNet.getNodes().containsKey(l.getValue().getFromNode().getId()))changedNet.addNode(fromNode);
//				if(!changedNet.getNodes().containsKey(l.getValue().getToNode().getId()))changedNet.addNode(toNode);
//				Link ll = netFac.createLink(l.getKey(), fromNode, toNode);
//				ll.setAllowedModes(l.getValue().getAllowedModes());
//				ll.setCapacity(l.getValue().getCapacity());
//				ll.setFreespeed(l.getValue().getFreespeed());
//				ll.setLength(l.getValue().getLength());
//				ll.setNumberOfLanes(l.getValue().getNumberOfLanes());
//				l.getValue().getAttributes().getAsMap().entrySet().forEach(a->{
//					ll.getAttributes().putAttribute(a.getKey(), a.getValue());
//				});
//				ll.getAttributes().putAttribute("changeType","modifications");
//				changedNet.addLink(ll);
//			}
//			
//		});
		
		new NetworkWriter(changedNet).write("data/kinan/emNetChange.xml");
		
	}
	
	public static boolean ifChange(Link l1, Link l2) {
		if(Math.abs(l1.getCapacity()-l2.getCapacity())>.1)return true;
		if(Math.abs(l1.getFreespeed()-l2.getFreespeed())>.1)return true;
		if(Math.abs(l1.getLength()-l2.getLength())>.1)return true;
		if(l1.getNumberOfLanes()!=l2.getNumberOfLanes())return true;
		if(l1.getAllowedModes().toString()!=l2.getAllowedModes().toString())return true;
		return false;
	}

}
