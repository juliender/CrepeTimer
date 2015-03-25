package fr.julienderenty.crepes;

import java.util.ArrayList;


/**
 * A list that gives the mean and last variation
 */
public class SensorBuffer extends ArrayList<Float>{

    int sizeMax;

    public SensorBuffer(int sizeMax){
        this.sizeMax = sizeMax;
    }

    public void put(Float value){
        this.add(0, value);
        if(this.size() >= sizeMax){
            this.remove(this.size()-1);
        }
    }

    public Float variation(){
        if(this.size()>1){
            return Math.abs(this.get(0) - this.get(1));
        }
        return 0f;
    }

    public Float mean(){
        if(this.size()>1){

            Float sum=0f;
            for(int i=1;i<this.size();i++){
                sum+=this.get(i)-this.get(i-1);
            }

            return Math.abs(sum/this.size());
        }
        return 0f;

    }
}
