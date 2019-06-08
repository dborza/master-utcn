package com.borzadan.model;

import com.yahoo.ycsb.ByteIterator;
import com.yahoo.ycsb.StringByteIterator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Represents different kinds of sensor measurements.
 */
public class Measurement {

    interface Factory {
        Measurement generate(String sensorId);
    }

    public static final String TABLE_NAME = "measurement";

    /**
     * DB column names.
     */
    public static final String ID = "ycsb_key";
    public static final String TYPE = "type";
    public static final String SENSOR_ID = "sensorId";
    public static final String VALUES = "values";
    public static final String TIMESTAMP = "create_time";

    public String id;
    public Type type;
    public String sensorId;
    public String[] values;
    public String timestamp;

    public Map<String, ByteIterator> dbValues() {
        final Map<String, ByteIterator> values = new HashMap<>();
//        values.put(ID, new StringByteIterator(id));
        values.put(TYPE, new StringByteIterator(type.toString()));
        values.put(SENSOR_ID, new StringByteIterator(sensorId));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.values.length; i ++) {
            sb.append(this.values[i]).append("-");
        }
        values.put(VALUES, new StringByteIterator(sb.toString()));
        values.put(TIMESTAMP, new StringByteIterator(this.timestamp));
        return values;
    }

    @Override
    public String toString() {
        return "Measurement{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", sensorId='" + sensorId + '\'' +
                ", values=" + Arrays.toString(values) +
                ", timestamp=" + timestamp +
                '}';
    }

    /**
     * Represents different kinds of measurement type.
     */
    public enum Type implements Factory {

        TEMPERATURE() {
            @Override
            public Measurement generate(String sensorId) {
                Measurement m = newMeasurement(sensorId);
                m.type = Type.TEMPERATURE;
                m.values = new String[1];
                m.values[0] = String.valueOf(r.nextInt(100));
                return m;
            }
        },
        SPEED() {
            @Override
            public Measurement generate(String sensorId) {
                Measurement m = newMeasurement(sensorId);
                m.type = Type.SPEED;
                m.values = new String[1];
                m.values[0] = String.valueOf(r.nextDouble());
                return m;
            }
        },
        DISTANCE() {
            @Override
            public Measurement generate(String sensorId) {
                Measurement m = newMeasurement(sensorId);
                m.type = Type.DISTANCE;
                m.values = new String[1];
                m.values[0] = String.valueOf(r.nextDouble());
                return m;
            }
        },
        GEO() {
            @Override
            public Measurement generate(String sensorId) {
                Measurement m = newMeasurement(sensorId);
                m.type = Type.GEO;
                m.values = new String[2];
                m.values[0] = String.valueOf(r.nextDouble());
                m.values[1] = String.valueOf(r.nextDouble());
                return m;
            }
        };

        private static Measurement newMeasurement(String sensorId) {
            final Measurement m = new Measurement();
            m.id = m.sensorId + "-" + r.nextLong();
            m.sensorId = sensorId;
            m.timestamp = String.valueOf(System.currentTimeMillis());
            return m;
        }

        /**
         * Used for generating measurement values.
         */
        private static final Random r = new Random();

    }

}