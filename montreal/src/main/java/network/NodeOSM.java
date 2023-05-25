package network;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;

public class NodeOSM {
    private long id;
    private Coord coordinate;

    public NodeOSM(long id, Coord coordinate) {
        this.id = id;
        this.coordinate = coordinate;
    }

    // Getters and setters

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Coord getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(Coord coordinate) {
        this.coordinate = coordinate;
    }
}





