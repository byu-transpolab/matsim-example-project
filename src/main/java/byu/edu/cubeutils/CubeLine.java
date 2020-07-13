package byu.edu.cubeutils;

import org.apache.log4j.Logger;

import java.util.ArrayList;

public class CubeLine {
    private static final Logger log = Logger.getLogger(CubeLine.class);


    private String name;
    private Integer color;
    private String mode;
    private Double speed;
    private Boolean oneway = false;
    private Integer[] headway = new Integer[2];
    private ArrayList<Integer> nodes;
    private ArrayList<Integer> stopNodes;

    public CubeLine(){

    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setHeadway(Integer headway, int i) {
        this.headway[i] = headway;
    }

    public void setMode(Integer mode) {
        switch(mode) {
            case 4:
                this.mode = "bus";
                this.speed = 0.8;
                break;
            case 5:
                this.mode = "art";
                this.speed = 0.85;
                break;
            case 6:
                this.mode = "exb";
                this.speed = 0.9;
                break;
            case 7:
                this.mode = "lrt";
                this.speed = 1.0;
                break;
            case 8:
                this.mode = "crt";
                this.speed = 1.0;
                break;
            case 9:
                this.mode = "brt";
                this.speed = 1.0;
                break;
        }
    }
    public String getMode(){
        return mode;
    }


    public void setColor(Integer color) {
        this.color = color;
    }

    public void setOneway(Boolean oneway) {
        this.oneway = oneway;
    }

    public void setNodes(ArrayList<Integer> nodes) {
        this.nodes = nodes;
    }

    public ArrayList<Integer> getStopNodes() {
        if(stopNodes == null){
            stopNodes = new ArrayList<>();
            for(Integer i:getNodes()){
                if(i > 0){
                    stopNodes.add(i);
                }
            }
        }
        return stopNodes;
    }

    public ArrayList<Integer> getNodes() {
        return nodes;
    }

    public boolean getOneWay() {
        return oneway;
    }

    public Integer[] getHeadway() {
        return headway;
    }

    public Double getSpeed() {
        return speed;
    }
}

