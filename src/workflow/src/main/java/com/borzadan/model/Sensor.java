package com.borzadan.model;

import com.yahoo.ycsb.ByteIterator;
import com.yahoo.ycsb.StringByteIterator;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Sensor {

    public static final String TABLE_NAME = "sensor";

    /**
     * DB column names.
     */
    public static final String ID = "ycsb_key";
    public static final String NAME = "name";
    public static final String DEVICE_ID = "deviceId";

    public String id;
    public String name;
    public String deviceId;
    public final List<Measurement> measurementList = new LinkedList<>();

    public Map<String, ByteIterator> dbValues() {
        final Map<String, ByteIterator> values = new HashMap<>();
//        values.put(ID, new StringByteIterator(id));
        values.put(NAME, new StringByteIterator(name));
        values.put(DEVICE_ID, new StringByteIterator(deviceId));
        return values;
    }

    @Override
    public String toString() {
        return "Sensor{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", measurementList=" + measurementList +
                '}';
    }

}
