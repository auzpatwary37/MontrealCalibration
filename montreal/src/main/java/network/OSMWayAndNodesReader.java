package network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


public class OSMWayAndNodesReader extends DefaultHandler{
	
    private StringBuilder currentElement;
    private boolean isNode;
    private boolean isTaggedNode;
    private long nodeId;
    private boolean isLink;
    private Coord nodeCoordinate;
    private Map<Long, NodeOSM> generalNodes;
    private Map<Long, NodeOSM> taggedNodes;
    private Map<String, List<Way>> waysByType;
    private List<Long> wayNodeIds;
    private CoordinateTransformation tsf = TransformationFactory.getCoordinateTransformation("WSG84", "epsg:32188");
    private Map<String,String> wayTags;
    
    public OSMWayAndNodesReader() {
        generalNodes = new HashMap<>();
        taggedNodes = new HashMap<>();
        waysByType = new HashMap<>();
        wayNodeIds = new ArrayList<>();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        currentElement = new StringBuilder();

        if (qName.equalsIgnoreCase("node")) {
            isNode = true;
            nodeId = Long.parseLong(attributes.getValue("id"));
            double latitude = Double.parseDouble(attributes.getValue("lat"));
            double longitude = Double.parseDouble(attributes.getValue("lon"));
            nodeCoordinate = new Coord(latitude, longitude);
            nodeCoordinate = tsf.transform(nodeCoordinate);
        } else if (isNode && qName.equalsIgnoreCase("tag")) {
            String key = attributes.getValue("k");
            String value = attributes.getValue("v");

            if (isTaggedNode(key, value)) {
                isTaggedNode = true;
            }
        } else if (qName.equalsIgnoreCase("way")) {
            wayNodeIds.clear();
            isLink = true;
            wayTags = new HashMap<>();
        } else if (isTaggedNode && qName.equalsIgnoreCase("nd")) {
            long refNodeId = Long.parseLong(attributes.getValue("ref"));
            wayNodeIds.add(refNodeId);
        }else if(isLink && qName.equalsIgnoreCase("tag")) {
        	wayTags.put(attributes.getValue("k"),attributes.getValue("v"));
        }
    }

    private boolean isTaggedNode(String key, String value) {
        return (key.equals("public_transport") && value.equals("stop_position")) ||
                (key.equals("railway") && value.equals("stop"));
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        if (isNode) {
            if (qName.equalsIgnoreCase("node")) {
                NodeOSM node = new NodeOSM(nodeId, nodeCoordinate);
                generalNodes.put(nodeId, node);

                if (isTaggedNode) {
                    NodeOSM taggedNode = new NodeOSM(nodeId, nodeCoordinate);
                    taggedNodes.put(nodeId, taggedNode);
                    isTaggedNode = false;
                }

                isNode = false;
            }
        } else if (qName.equalsIgnoreCase("way")) {
            if (!wayNodeIds.isEmpty()) {
            	
                Way way = new Way(new ArrayList<>(wayNodeIds),wayTags);
                String railwayTag = way.getAttributes().get("railway");
                if (railwayTag != null) {
                    List<Way> waysOfType = waysByType.getOrDefault(railwayTag, new ArrayList<>());
                    waysOfType.add(way);
                    waysByType.put(railwayTag, waysOfType);
                }
            }
            isLink = false;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        currentElement.append(ch, start, length);
    }
	
	public void read(String fileLoc) {
		
		try {
			SAXParserFactory.newInstance().newSAXParser().parse(fileLoc,this);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		
		
	}
	
	public static void main(String[] args) {
		String fileLoc = "data/osm/export-2.osm";
		OSMWayAndNodesReader reader = new OSMWayAndNodesReader();
	}
	

}
