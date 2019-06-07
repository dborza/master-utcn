package com.borzadan.model;

import com.yahoo.ycsb.ByteIterator;
import com.yahoo.ycsb.StringByteIterator;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Device {

    public static final String TABLE_NAME = "device";
    /**
     * DB column names.
     */
    public static final String ID = "id";
    public static final String NAME = "name";

    public String id;
    public String name;
    public final List<Sensor> sensors = new LinkedList<>();

    public Map<String, ByteIterator> dbValues() {
        final Map<String, ByteIterator> values = new HashMap<>();
        values.put(ID, new StringByteIterator(id));
        values.put(NAME, new StringByteIterator(name));
        return values;
    }

    @Override
    public String toString() {
        return "Device{" +
                "id='" + id + '\'' +
                ", sensors=" + sensors +
                '}';
    }
}
