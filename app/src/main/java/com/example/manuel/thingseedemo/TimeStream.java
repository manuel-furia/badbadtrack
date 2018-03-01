package com.example.manuel.thingseedemo;

import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by manuel on 2/19/18.
 */

public class TimeStream<T extends DataWithTime> {

    private ArrayList<T> data;
    long outOfBoundMargin;

    public TimeStream(long outOfBoundMarginTime){

        data = new ArrayList<>();
        outOfBoundMargin = outOfBoundMarginTime;
    }

    public void addSample(T sample){
        data.add(sample);
        Collections.sort(data);
    }

    public void addStream(TimeStream<T> stream){
        boolean ordered = true;

        outOfBoundMargin = stream.outOfBoundMargin;

        if (data.isEmpty())
            data.addAll(stream.data);
        else {
            long last = data.get(data.size() - 1).getTime();
            for (T entry : stream.data) {
                ordered = ordered && (entry.getTime() >= last);
            }
            data.addAll(stream.data);
            if (!ordered)
                Collections.sort(data);
        }
    }

    public int sampleCount(){
        return data.size();
    }

    @Nullable
    public T getLast(){
        if (data.size() <= 0)
            return null;
        return data.get(data.size() - 1);
    }

    @Nullable
    public T getFirst(){
        if (data.size() <= 0)
            return null;
        return data.get(0);
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    public boolean isNotEmpty() {
        return !data.isEmpty();
    }

    @Nullable
    public long getLastTimestamp(){
        if (data.size() <= 0)
            return 0;
        return data.get(data.size()-1).getTime();
    }

    public TimeStream<ScalarData> integrate(Integral integral){

        double acc = 0.0;
        TimeStream<ScalarData> result = new TimeStream<>(outOfBoundMargin);

        if (data.size() <= 1)
            return result;

        T oldElem = data.get(0);
        for (int i = 1; i < data.size(); i++){
            T curElem = data.get(i);
            double dt = curElem.getTime() - oldElem.getTime();
            ScalarData integralValue = new ScalarData();
            acc = integral.integral(acc, oldElem, curElem, dt);
            integralValue.setValue(acc);
            integralValue.setTime(oldElem.getTime() + (long)(dt / 2L));
            result.addSample(integralValue);
            oldElem = curElem;
        }

        return result;
    }

    public TimeStream<ScalarData> differentiate(Derivative derivative){

        TimeStream<ScalarData> result = new TimeStream<>(outOfBoundMargin);

        if (data.size() <= 1)
            return result;

        T oldElem = data.get(0);
        for (int i = 1; i < data.size(); i++){
            T curElem = data.get(i);
            double dt = curElem.getTime() - oldElem.getTime();
            ScalarData derivativeValue = new ScalarData();
            derivativeValue.setValue(derivative.derivative(oldElem, curElem, dt));
            derivativeValue.setTime(oldElem.getTime() + (long)(dt / 2L));
            result.addSample(derivativeValue);
            oldElem = curElem;
        }

        return result;
    }

    public TimeStream<ScalarData> map(TimeStreamMapToScalar map){

        double acc = 0.0;
        TimeStream<ScalarData> result = new TimeStream<>(outOfBoundMargin);

        for (int i = 0; i < data.size(); i++){
            T curElem = data.get(i);
            ScalarData newElem = new ScalarData();

            newElem.setValue(map.map(curElem));
            newElem.setTime(curElem.getTime());
            result.addSample(newElem);
        }

        return result;
    }

    public void clear(){
        data.clear();
    }

    @Nullable
    public T getDataAtTime(long time){
        //Find the element before and the one after the specified time
        int before = -1, after = -1;
        long elemTime, timeDiff = 0;
        for (int i = 0; i < data.size(); i++){
            //Log.d("Stream", "Time: " + time + " CTime: " + data.get(i).getTime() + " index: " + i + " size: " + data.size());
            elemTime = data.get(i).getTime();
            timeDiff = elemTime - time;
            if (timeDiff >= 0){
                after = i;
                before = i - 1;
                break;
            }
        }



        if (after < 1 || after > data.size()){
            return null;
        }

        //If the requested time is after the last element, return the last element,
        //but only if the requested time is not much later
        if (time - getLastTimestamp() > outOfBoundMargin)
            return  null;
        if (before == data.size() - 1)
            return data.get(before);


        //Linearly interpolate between the two values
        //Note: The type of data is ArrayList<T>, so the cast will always be successful, since interpolate
        //      always returns the same type
        @SuppressWarnings("unchecked")
        T result = (T) data.get(before).interpolate(data.get(after), time);

        return result;
    }

}