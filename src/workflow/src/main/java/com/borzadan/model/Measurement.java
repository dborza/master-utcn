package com.borzadan.model;

import java.util.Random;

/**
 * Represents different kinds of sensor measurements.
 */
public class Measurement {

    interface Factory {
        Measurement generate(Long sensorId);
    }

    /**
     * Represents different kinds of measurement type.
     */
    enum Type implements Factory {

        TEMPERATURE() {
            @Override
            public Measurement generate(Long sensorId) {
                Measurement m = newMeasurement(sensorId);
                m.type = Type.TEMPERATURE;
                m.values = new String[1];
                m.values[0] = String.valueOf(r.nextInt(100));
                return m;
            }
        },
        SPEED() {
            @Override
            public Measurement generate(Long sensorId) {
                Measurement m = newMeasurement(sensorId);
                m.type = Type.SPEED;
                m.values = new String[1];
                m.values[0] = String.valueOf(r.nextDouble());
                return m;
            }
        },
        DISTANCE() {
            @Override
            public Measurement generate(Long sensorId) {
                Measurement m = newMeasurement(sensorId);
                m.type = Type.DISTANCE;
                m.values = new String[1];
                m.values[0] = String.valueOf(r.nextDouble());
                return m;
            }
        },
        GEO() {
            @Override
            public Measurement generate(Long sensorId) {
                Measurement m = newMeasurement(sensorId);
                m.type = Type.GEO;
                m.values = new String[2];
                m.values[0] = String.valueOf(r.nextDouble());
                m.values[1] = String.valueOf(r.nextDouble());
                return m;
            }
        };

        private static Measurement newMeasurement(Long sensorId) {
            final Measurement m = new Measurement();
            m.sensorId = sensorId;
            return m;
        }

        /**
         * Used for generating measurement values.
         */
        private static final Random r = new Random();

    }

    Type type;
    Long sensorId;
    String[] values;

}