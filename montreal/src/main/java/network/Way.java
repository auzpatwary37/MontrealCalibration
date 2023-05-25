package network;

import java.util.List;
import java.util.Map;

public class Way {
    private List<Long> nodeIds;
    private Map<String, String> attributes;

    public Way(List<Long> nodeIds, Map<String, String> attributes) {
        this.nodeIds = nodeIds;
        this.attributes = attributes;
    }

    // Getters and setters

    public List<Long> getNodeIds() {
        return nodeIds;
    }

    public void setNodeIds(List<Long> nodeIds) {
        this.nodeIds = nodeIds;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }
}

