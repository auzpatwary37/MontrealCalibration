package network;

import org.matsim.api.core.v01.Coord;

public class Stop extends NodeOSM {
    private String stationName;
    private String operator;
    private String altName;

    public Stop(long id, Coord coordinate, String stationName, String operator, String altName) {
        super(id, coordinate);
        this.stationName = stationName;
        this.operator = operator;
        this.altName = altName;
    }

    // Getters and setters

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getAltName() {
        return altName;
    }

    public void setAltName(String altName) {
        this.altName = altName;
    }
}

