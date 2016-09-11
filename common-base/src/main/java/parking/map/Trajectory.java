package parking.map;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.List;
import java.util.ArrayList;

import parking.map.Position;

public class Trajectory {

    private List<Position> positions;
    private List<Double> timeList;  // in seconds
    private int maxSize;
    private long initTime;
    private boolean allowLimit;
    private final Double toSec = 1000000000.0;

    public Trajectory(int maxSize) {
        this.maxSize = maxSize;
        allowLimit = true;
    }

    public Trajectory() {
        positions = new ArrayList<Position>();
        timeList = new ArrayList<Double>();
        allowLimit = true;
    }

    public Trajectory(JSONObject jObj) throws JSONException {
        JSONArray jsonArray = jObj.optJSONArray("positions");
        if (jsonArray != null) {
            positions = new ArrayList<Position>();
            for ( int i=0 ; i<jsonArray.length() ; i++ ) {
                positions.add(new Position(jsonArray.getJSONObject(i)));
            }
        }
        jsonArray = jObj.optJSONArray("timeList");
        if (jsonArray != null) {
            timeList = new ArrayList<Double>();
            for ( int i=0 ; i<jsonArray.length() ; i++ ) {
                timeList.add(jsonArray.getDouble(i));
            }
        }
        allowLimit = true;
    }

    public void add(Position p) {
        if (positions == null) {
            positions = new ArrayList<Position>();
            timeList = new ArrayList<Double>();
            initTime = System.nanoTime();
            timeList.add(0.0);
        }
        else {
            long currTime = System.nanoTime();
            double elapsed = (currTime - initTime)/toSec;
            timeList.add(elapsed);
        }

        if (allowLimit && positions.size() == maxSize) {
            positions.remove(0);
            timeList.remove(0);
        }
        positions.add(p);
    }

    public void add(Position p, double t) {
        positions.add(p);
        timeList.add(t);
    }

    public int size() {
        if (positions != null) {
            return positions.size();
        }
        return 0;
    }

    public void blockLimit() {
        allowLimit = false;
    }

    public void allowLimit() {
        allowLimit = true;
    }


    public List<Position> getPositions() {
        return positions;
    }

    public List<Double> getTimeList() {
        return timeList;
    }

    // get position backwards in time
    public Position getPosition(int reverseIndex) {
        if (reverseIndex < positions.size()) {
            int index = positions.size()-reverseIndex-1;
            return positions.get(index);
        }
        return null;
    }

    // get time in sec between samples, backwards in time
    public Double getTime(int reverseIndex) {  
        if (reverseIndex < timeList.size()) {
            int index = timeList.size()-reverseIndex-1;
            return timeList.get(index);
        }
        return 0.0;
    }

    public Double getElapsedTime() {
        long currTime = System.nanoTime();
        double elapsed = (currTime - initTime)/toSec;
        return elapsed;
    }

    public JSONObject serialize() throws JSONException {
        JSONObject jObj = new JSONObject();
        if (positions != null) {
            JSONArray jsonArray = new JSONArray();
            jObj.put("positions", jsonArray);
            for (Position p: positions) {
                jObj.accumulate("positions", p.serialize());
            }
        }
        if (positions != null) {
            JSONArray jsonArray = new JSONArray();
            jObj.put("timeList", jsonArray);
            for (Double t: timeList) {
                jObj.accumulate("timeList", t);
            }
        }
        return jObj;
    }
}
