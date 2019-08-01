/**
 * Copyright (c) 2010 Yahoo! Inc., Copyright (c) 2016-2017 YCSB contributors. All rights reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You
 * may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License. See accompanying
 * LICENSE file.
 */

package com.borzadan.workload;

import com.borzadan.model.Device;
import com.borzadan.model.Measurement;
import com.borzadan.model.Sensor;
import com.yahoo.ycsb.*;
import com.yahoo.ycsb.generator.*;
import com.yahoo.ycsb.measurements.Measurements;
import com.yahoo.ycsb.workloads.CoreWorkload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.yahoo.ycsb.Client.*;

/**
 * Dan Borza's modification of {@link CoreWorkload}
 *
 *
 * The core benchmark scenario. Represents a set of clients doing simple CRUD operations. The
 * relative proportion of different kinds of operations, and other properties of the workload,
 * are controlled by parameters specified at runtime.
 * <p>
 * Properties to control the client:
 * <UL>
 * <LI><b>fieldcount</b>: the number of fields in a record (default: 10)
 * <LI><b>fieldlength</b>: the size of each field (default: 100)
 * <LI><b>minfieldlength</b>: the minimum size of each field (default: 1)
 * <LI><b>readallfields</b>: should reads read all fields (true) or just one (false) (default: true)
 * <LI><b>writeallfields</b>: should updates and read/modify/writes update all fields (true) or just
 * one (false) (default: false)
 * <LI><b>readproportion</b>: what proportion of operations should be reads (default: 0.95)
 * <LI><b>updateproportion</b>: what proportion of operations should be updates (default: 0.05)
 * <LI><b>insertproportion</b>: what proportion of operations should be inserts (default: 0)
 * <LI><b>scanproportion</b>: what proportion of operations should be scans (default: 0)
 * <LI><b>readmodifywriteproportion</b>: what proportion of operations should be read a record,
 * modify it, write it back (default: 0)
 * <LI><b>requestdistribution</b>: what distribution should be used to select the records to operate
 * on - uniform, zipfian, hotspot, sequential, exponential or latest (default: uniform)
 * <LI><b>minscanlength</b>: for scans, what is the minimum number of records to scan (default: 1)
 * <LI><b>maxscanlength</b>: for scans, what is the maximum number of records to scan (default: 1000)
 * <LI><b>scanlengthdistribution</b>: for scans, what distribution should be used to choose the
 * number of records to scan, for each scan, between 1 and maxscanlength (default: uniform)
 * <LI><b>insertstart</b>: for parallel loads and runs, defines the starting record for this
 * YCSB instance (default: 0)
 * <LI><b>insertcount</b>: for parallel loads and runs, defines the number of records for this
 * YCSB instance (default: recordcount)
 * <LI><b>zeropadding</b>: for generating a record sequence compatible with string sort order by
 * 0 padding the record number. Controls the number of 0s to use for padding. (default: 1)
 * For example for row 5, with zeropadding=1 you get 'user5' key and with zeropading=8 you get
 * 'user00000005' key. In order to see its impact, zeropadding needs to be bigger than number of
 * digits in the record number.
 * <LI><b>insertorder</b>: should records be inserted in order by key ("ordered"), or in hashed
 * order ("hashed") (default: hashed)
 * <LI><b>fieldnameprefix</b>: what should be a prefix for field names, the shorter may decrease the
 * required storage size (default: "field")
 * </ul>
 */
public class DanWorkload extends Workload {

  private static final Logger LOG = LoggerFactory.getLogger("DanWorkload");

  private static final Random RANDOM = new Random();

  /**
   * The name of the property for the field length distribution. Options are "uniform", "zipfian"
   * (favouring short records), "constant", and "histogram".
   * <p>
   * If "uniform", "zipfian" or "constant", the maximum field length will be that specified by the
   * fieldlength property. If "histogram", then the histogram will be read from the filename
   * specified in the "fieldlengthhistogram" property.
   */
  private static final String FIELD_LENGTH_DISTRIBUTION_PROPERTY = "fieldlengthdistribution";

  /**
   * The default field length distribution.
   */
  private static final String FIELD_LENGTH_DISTRIBUTION_PROPERTY_DEFAULT = "constant";

  /**
   * The name of the property for the length of a field in bytes.
   */
  private static final String FIELD_LENGTH_PROPERTY = "fieldlength";

  /**
   * The default maximum length of a field in bytes.
   */
  private static final String FIELD_LENGTH_PROPERTY_DEFAULT = "100";

  /**
   * The name of the property for the minimum length of a field in bytes.
   * The name of the property for the minimum length of a field in bytes.
   */
  private static final String MIN_FIELD_LENGTH_PROPERTY = "minfieldlength";

  /**
   * The default minimum length of a field in bytes.
   */
  private static final String MIN_FIELD_LENGTH_PROPERTY_DEFAULT = "1";

  /**
   * The name of a property that specifies the filename containing the field length histogram (only
   * used if fieldlengthdistribution is "histogram").
   */
  private static final String FIELD_LENGTH_HISTOGRAM_FILE_PROPERTY = "fieldlengthhistogram";

  /**
   * The default filename containing a field length histogram.
   */
  private static final String FIELD_LENGTH_HISTOGRAM_FILE_PROPERTY_DEFAULT = "hist.txt";

  /**
   * Generator object that produces field lengths.  The value of this depends on the properties that
   * start with "FIELD_LENGTH_".
   */
  private NumberGenerator fieldlengthgenerator;

  /**
   * The name of the property for deciding whether to read one field (false) or all fields (true) of
   * a record.
   */
  private static final String READ_ALL_FIELDS_PROPERTY = "readallfields";

  /**
   * The default value for the readallfields property.
   */
  private static final String READ_ALL_FIELDS_PROPERTY_DEFAULT = "true";

  /**
   * The name of the property for deciding whether to write one field (false) or all fields (true)
   * of a record.
   */
  private static final String WRITE_ALL_FIELDS_PROPERTY = "writeallfields";

  /**
   * The default value for the writeallfields property.
   */
  private static final String WRITE_ALL_FIELDS_PROPERTY_DEFAULT = "false";

  /**
   * The name of the property for deciding whether to check all returned
   * data against the formation template to ensure data integrity.
   */
  private static final String DATA_INTEGRITY_PROPERTY = "dataintegrity";

  /**
   * The default value for the dataintegrity property.
   */
  private static final String DATA_INTEGRITY_PROPERTY_DEFAULT = "false";

  /**
   * Set to true if want to check correctness of reads. Must also
   * be set to true during loading phase to function.
   */
  private boolean dataintegrity;

  /**
   * The name of the property for the proportion of transactions that are reads.
   */
  private static final String READ_PROPORTION_PROPERTY = "readproportion";

  /**
   * The default proportion of transactions that are reads.
   */
  private static final String READ_PROPORTION_PROPERTY_DEFAULT = "0.95";

  /**
   * The name of the property for the proportion of transactions that are updates.
   */
  private static final String UPDATE_PROPORTION_PROPERTY = "updateproportion";

  /**
   * The default proportion of transactions that are updates.
   */
  private static final String UPDATE_PROPORTION_PROPERTY_DEFAULT = "0.05";

  /**
   * The name of the property for the proportion of transactions that are inserts.
   */
  private static final String INSERT_PROPORTION_PROPERTY = "insertproportion";

  /**
   * The default proportion of transactions that are inserts.
   */
  private static final String INSERT_PROPORTION_PROPERTY_DEFAULT = "0.0";

  /**
   * The name of the property for the proportion of transactions that are scans.
   */
  private static final String SCAN_PROPORTION_PROPERTY = "scanproportion";

  /**
   * The default proportion of transactions that are scans.
   */
  private static final String SCAN_PROPORTION_PROPERTY_DEFAULT = "0.0";

  /**
   * The name of the property for the proportion of transactions that are read-modify-write.
   */
  private static final String READMODIFYWRITE_PROPORTION_PROPERTY = "readmodifywriteproportion";

  /**
   * The default proportion of transactions that are scans.
   */
  private static final String READMODIFYWRITE_PROPORTION_PROPERTY_DEFAULT = "0.0";

  /**
   * The name of the property for the the distribution of requests across the keyspace. Options are
   * "uniform", "zipfian" and "latest"
   */
  private static final String REQUEST_DISTRIBUTION_PROPERTY = "requestdistribution";

  /**
   * The default distribution of requests across the keyspace.
   */
  private static final String REQUEST_DISTRIBUTION_PROPERTY_DEFAULT = "uniform";

  /**
   * The name of the property for adding zero padding to record numbers in order to match
   * string sort order. Controls the number of 0s to left pad with.
   */
  private static final String ZERO_PADDING_PROPERTY = "zeropadding";

  /**
   * The default zero padding value. Matches integer sort order
   */
  private static final String ZERO_PADDING_PROPERTY_DEFAULT = "1";

  /**
   * The name of the property for the min scan length (number of records).
   */
  private static final String MIN_SCAN_LENGTH_PROPERTY = "minscanlength";

  /**
   * The default min scan length.
   */
  private static final String MIN_SCAN_LENGTH_PROPERTY_DEFAULT = "1";

  /**
   * The name of the property for the max scan length (number of records).
   */
  private static final String MAX_SCAN_LENGTH_PROPERTY = "maxscanlength";

  /**
   * The default max scan length.
   */
  private static final String MAX_SCAN_LENGTH_PROPERTY_DEFAULT = "1000";

  /**
   * The name of the property for the scan length distribution. Options are "uniform" and "zipfian"
   * (favoring short scans)
   */
  private static final String SCAN_LENGTH_DISTRIBUTION_PROPERTY = "scanlengthdistribution";

  /**
   * The default max scan length.
   */
  private static final String SCAN_LENGTH_DISTRIBUTION_PROPERTY_DEFAULT = "uniform";

  /**
   * The name of the property for the order to insert records. Options are "ordered" or "hashed"
   */
  private static final String INSERT_ORDER_PROPERTY = "insertorder";

  /**
   * Default insert order.
   */
  private static final String INSERT_ORDER_PROPERTY_DEFAULT = "hashed";

  /**
   * Percentage data items that constitute the hot set.
   */
  private static final String HOTSPOT_DATA_FRACTION = "hotspotdatafraction";

  /**
   * Default value of the size of the hot set.
   */
  private static final String HOTSPOT_DATA_FRACTION_DEFAULT = "0.2";

  /**
   * Percentage operations that access the hot set.
   */
  private static final String HOTSPOT_OPN_FRACTION = "hotspotopnfraction";

  /**
   * Default value of the percentage operations accessing the hot set.
   */
  private static final String HOTSPOT_OPN_FRACTION_DEFAULT = "0.8";

  /**
   * How many times to retry when insertion of a single item to a DB fails.
   */
  private static final String INSERTION_RETRY_LIMIT = "core_workload_insertion_retry_limit";

  private static final String INSERTION_RETRY_LIMIT_DEFAULT = "0";

  /**
   * On average, how long to wait between the retries, in seconds.
   */
  private static final String INSERTION_RETRY_INTERVAL = "core_workload_insertion_retry_interval";

  private static final String INSERTION_RETRY_INTERVAL_DEFAULT = "3";

  private static final String DEVICE_ROWS = "device_rows";

  private static final String SENSOR_ROWS = "sensor_rows";

  private static final String MEASUREMENT_ROWS = "measurement_rows";

  private static final int NUM_SENSORS = 10;

  private NumberGenerator keysequence;
  private DiscreteGenerator operationchooser;
  private NumberGenerator deviceKeyChooser;
  private NumberGenerator sensorKeyChooser;
  private NumberGenerator measurementKeyChooser;
  private NumberGenerator fieldchooser;
  private AcknowledgedCounterGenerator transactioninsertkeysequence;
  private NumberGenerator scanlength;
  private boolean orderedinserts;
  private long fieldcount;
  protected long recordcount;
  private int zeropadding;
  private int insertionRetryLimit;
  private int insertionRetryInterval;

  private final List<String> fieldnames = Arrays.asList(Measurement.VALUES, Measurement.TIMESTAMP, Measurement.TYPE, Measurement.SENSOR_ID);;

  private final Set<String> fieldnamesSet = new HashSet<>(fieldnames);

  private Measurements measurements = Measurements.getMeasurements();

  /**
   * Keep track of the total number of sensors in order to be able to randomly select one.
   */
  private final AtomicInteger SENSOR_NUM = new AtomicInteger(0);

  /**
   * Keep track of total number of measurements;
   */
  private final AtomicInteger MEASUREMENT_NUM = new AtomicInteger(0);

  /**
   * Keep track of total number of devices
   */
  private final AtomicInteger DEVICE_NUM = new AtomicInteger(0);

  private void readExistingValue(final Properties p, final String propertyName, final AtomicInteger value) {
    final int intValue = Integer.parseInt(p.getProperty(propertyName, "0"));
    value.set(intValue);
  }

  private static NumberGenerator getFieldLengthGenerator(Properties p) throws WorkloadException {
    NumberGenerator fieldlengthgenerator;
    String fieldlengthdistribution = p.getProperty(
        FIELD_LENGTH_DISTRIBUTION_PROPERTY, FIELD_LENGTH_DISTRIBUTION_PROPERTY_DEFAULT);
    int fieldlength =
        Integer.parseInt(p.getProperty(FIELD_LENGTH_PROPERTY, FIELD_LENGTH_PROPERTY_DEFAULT));
    int minfieldlength =
        Integer.parseInt(p.getProperty(MIN_FIELD_LENGTH_PROPERTY, MIN_FIELD_LENGTH_PROPERTY_DEFAULT));
    String fieldlengthhistogram = p.getProperty(
        FIELD_LENGTH_HISTOGRAM_FILE_PROPERTY, FIELD_LENGTH_HISTOGRAM_FILE_PROPERTY_DEFAULT);
    if (fieldlengthdistribution.compareTo("constant") == 0) {
      fieldlengthgenerator = new ConstantIntegerGenerator(fieldlength);
    } else if (fieldlengthdistribution.compareTo("uniform") == 0) {
      fieldlengthgenerator = new UniformLongGenerator(minfieldlength, fieldlength);
    } else if (fieldlengthdistribution.compareTo("zipfian") == 0) {
      fieldlengthgenerator = new ZipfianGenerator(minfieldlength, fieldlength);
    } else if (fieldlengthdistribution.compareTo("histogram") == 0) {
      try {
        fieldlengthgenerator = new HistogramGenerator(fieldlengthhistogram);
      } catch (IOException e) {
        throw new WorkloadException(
            "Couldn't read field length histogram file: " + fieldlengthhistogram, e);
      }
    } else {
      throw new WorkloadException(
          "Unknown field length distribution \"" + fieldlengthdistribution + "\"");
    }
    return fieldlengthgenerator;
  }

  /**
   * Initialize the scenario.
   * Called once, in the main client thread, before any operations are started.
   */
  @Override
  public void init(Properties p) throws WorkloadException {

    readExistingValue(p, DEVICE_ROWS, DEVICE_NUM);
    readExistingValue(p, SENSOR_ROWS, SENSOR_NUM);
    readExistingValue(p, MEASUREMENT_ROWS, MEASUREMENT_NUM);

    fieldcount = fieldnames.size() - 1;

    fieldlengthgenerator = DanWorkload.getFieldLengthGenerator(p);

    recordcount =
        Long.parseLong(p.getProperty(RECORD_COUNT_PROPERTY, Client.DEFAULT_RECORD_COUNT));
    if (recordcount == 0) {
      recordcount = Integer.MAX_VALUE;
    }
    String requestdistrib =
        p.getProperty(REQUEST_DISTRIBUTION_PROPERTY, REQUEST_DISTRIBUTION_PROPERTY_DEFAULT);
    int minscanlength =
        Integer.parseInt(p.getProperty(MIN_SCAN_LENGTH_PROPERTY, MIN_SCAN_LENGTH_PROPERTY_DEFAULT));
    int maxscanlength =
        Integer.parseInt(p.getProperty(MAX_SCAN_LENGTH_PROPERTY, MAX_SCAN_LENGTH_PROPERTY_DEFAULT));
    String scanlengthdistrib =
        p.getProperty(SCAN_LENGTH_DISTRIBUTION_PROPERTY, SCAN_LENGTH_DISTRIBUTION_PROPERTY_DEFAULT);

    long insertstart =
        Long.parseLong(p.getProperty(INSERT_START_PROPERTY, INSERT_START_PROPERTY_DEFAULT));
    long insertcount=
        Integer.parseInt(p.getProperty(INSERT_COUNT_PROPERTY, String.valueOf(recordcount - insertstart)));
    // Confirm valid values for insertstart and insertcount in relation to recordcount
    if (recordcount < (insertstart + insertcount)) {
      System.err.println("Invalid combination of insertstart, insertcount and recordcount.");
      System.err.println("recordcount must be bigger than insertstart + insertcount.");
      System.exit(-1);
    }
    zeropadding =
        Integer.parseInt(p.getProperty(ZERO_PADDING_PROPERTY, ZERO_PADDING_PROPERTY_DEFAULT));

    dataintegrity = Boolean.parseBoolean(
        p.getProperty(DATA_INTEGRITY_PROPERTY, DATA_INTEGRITY_PROPERTY_DEFAULT));
    // Confirm that fieldlengthgenerator returns a constant if data
    // integrity check requested.
    if (dataintegrity && !(p.getProperty(
        FIELD_LENGTH_DISTRIBUTION_PROPERTY,
        FIELD_LENGTH_DISTRIBUTION_PROPERTY_DEFAULT)).equals("constant")) {
      System.err.println("Must have constant field size to check data integrity.");
      System.exit(-1);
    }

    if (p.getProperty(INSERT_ORDER_PROPERTY, INSERT_ORDER_PROPERTY_DEFAULT).compareTo("hashed") == 0) {
      orderedinserts = false;
    } else if (requestdistrib.compareTo("exponential") == 0) {
      double percentile = Double.parseDouble(p.getProperty(
          ExponentialGenerator.EXPONENTIAL_PERCENTILE_PROPERTY,
          ExponentialGenerator.EXPONENTIAL_PERCENTILE_DEFAULT));
      double frac = Double.parseDouble(p.getProperty(
          ExponentialGenerator.EXPONENTIAL_FRAC_PROPERTY,
          ExponentialGenerator.EXPONENTIAL_FRAC_DEFAULT));
      deviceKeyChooser = new ExponentialGenerator(percentile, recordcount * frac);
      sensorKeyChooser = new ExponentialGenerator(percentile, recordcount * frac);
      measurementKeyChooser = new ExponentialGenerator(percentile, recordcount * frac);
    } else {
      orderedinserts = true;
    }

    keysequence = new CounterGenerator(insertstart);
    operationchooser = createOperationGenerator(p);

    transactioninsertkeysequence = new AcknowledgedCounterGenerator(recordcount);
    if (requestdistrib.compareTo("uniform") == 0) {
      deviceKeyChooser = new UniformLongGenerator(insertstart, insertstart + insertcount - 1);
      sensorKeyChooser = new UniformLongGenerator(insertstart, insertstart + insertcount - 1);
      measurementKeyChooser = new UniformLongGenerator(insertstart, insertstart + insertcount - 1);
    } else if (requestdistrib.compareTo("sequential") == 0) {
      deviceKeyChooser = new SequentialGenerator(DEVICE_NUM.get(), DEVICE_NUM.get() + insertcount - 1);
      sensorKeyChooser = new SequentialGenerator(SENSOR_NUM.get(), Integer.MAX_VALUE);
      measurementKeyChooser = new SequentialGenerator(MEASUREMENT_NUM.get() + 1, Integer.MAX_VALUE);
    } else if (requestdistrib.compareTo("zipfian") == 0) {
      // it does this by generating a random "next key" in part by taking the modulus over the
      // number of keys.
      // If the number of keys changes, this would shift the modulus, and we don't want that to
      // change which keys are popular so we'll actually construct the scrambled zipfian generator
      // with a keyspace that is larger than exists at the beginning of the test. that is, we'll predict
      // the number of inserts, and tell the scrambled zipfian generator the number of existing keys
      // plus the number of predicted keys as the total keyspace. then, if the generator picks a key
      // that hasn't been inserted yet, will just ignore it and pick another key. this way, the size of
      // the keyspace doesn't change from the perspective of the scrambled zipfian generator
      final double insertproportion = Double.parseDouble(
          p.getProperty(INSERT_PROPORTION_PROPERTY, INSERT_PROPORTION_PROPERTY_DEFAULT));
      int opcount = Integer.parseInt(p.getProperty(Client.OPERATION_COUNT_PROPERTY));
      int expectednewkeys = (int) ((opcount) * insertproportion * 2.0); // 2 is fudge factor

      deviceKeyChooser = new ScrambledZipfianGenerator(insertstart, insertstart + insertcount + expectednewkeys);
      sensorKeyChooser = new ScrambledZipfianGenerator(insertstart, insertstart + insertcount + expectednewkeys);
      measurementKeyChooser = new ScrambledZipfianGenerator(insertstart, insertstart + insertcount + expectednewkeys);
    } else if (requestdistrib.compareTo("latest") == 0) {
      deviceKeyChooser = new SkewedLatestGenerator(transactioninsertkeysequence);
      sensorKeyChooser = new SkewedLatestGenerator(transactioninsertkeysequence);
      measurementKeyChooser = new SkewedLatestGenerator(transactioninsertkeysequence);
    } else if (requestdistrib.equals("hotspot")) {
      double hotsetfraction =
          Double.parseDouble(p.getProperty(HOTSPOT_DATA_FRACTION, HOTSPOT_DATA_FRACTION_DEFAULT));
      double hotopnfraction =
          Double.parseDouble(p.getProperty(HOTSPOT_OPN_FRACTION, HOTSPOT_OPN_FRACTION_DEFAULT));
      deviceKeyChooser = new HotspotIntegerGenerator(insertstart, insertstart + insertcount - 1, hotsetfraction, hotopnfraction);
      sensorKeyChooser = new HotspotIntegerGenerator(insertstart, insertstart + insertcount - 1, hotsetfraction, hotopnfraction);
      measurementKeyChooser = new HotspotIntegerGenerator(insertstart, insertstart + insertcount - 1, hotsetfraction, hotopnfraction);
    } else {
      throw new WorkloadException("Unknown request distribution \"" + requestdistrib + "\"");
    }

    fieldchooser = new UniformLongGenerator(0, fieldcount - 1);

    if (scanlengthdistrib.compareTo("uniform") == 0) {
      scanlength = new UniformLongGenerator(minscanlength, maxscanlength);
    } else if (scanlengthdistrib.compareTo("zipfian") == 0) {
      scanlength = new ZipfianGenerator(minscanlength, maxscanlength);
    } else {
      throw new WorkloadException(
          "Distribution \"" + scanlengthdistrib + "\" not allowed for scan length");
    }

    insertionRetryLimit = Integer.parseInt(p.getProperty(
        INSERTION_RETRY_LIMIT, INSERTION_RETRY_LIMIT_DEFAULT));
    insertionRetryInterval = Integer.parseInt(p.getProperty(
        INSERTION_RETRY_INTERVAL, INSERTION_RETRY_INTERVAL_DEFAULT));

    printWorkflowProperties(p);
  }

  private void printWorkflowProperties(Properties p) {
    String [] properties = {
      WORKLOAD_PROPERTY, DATA_INTEGRITY_PROPERTY, REQUEST_DISTRIBUTION_PROPERTY, THREAD_COUNT_PROPERTY, RECORD_COUNT_PROPERTY,
      OPERATION_COUNT_PROPERTY, READ_PROPORTION_PROPERTY, READMODIFYWRITE_PROPORTION_PROPERTY, SCAN_PROPORTION_PROPERTY,
      INSERT_PROPORTION_PROPERTY, UPDATE_PROPORTION_PROPERTY
    };
    final Set<String> allPropertyNames = new HashSet<>(p.stringPropertyNames());
    allPropertyNames.addAll(Arrays.asList(properties));
    allPropertyNames.forEach(key -> {
      debug("Found property " + key + "=" + p.getProperty(key));
    });
  }

  /**
   * Builds a value for a randomly chosen field.
   */
  private HashMap<String, ByteIterator> buildSingleValue(String key, String fieldkey) {
    debug("buildSinglevalue key=" + key);
    HashMap<String, ByteIterator> value = new HashMap<>();

    ByteIterator data;
    if (dataintegrity) {
      data = new StringByteIterator(buildDeterministicValue(key, fieldkey));
    } else {
      // fill with random data
      data = new RandomByteIterator(fieldlengthgenerator.nextValue().longValue());
    }
    debug("built new value for key=" + fieldkey);
    value.put(fieldkey, data);

    debug("buildValue value = " + value);
    return value;
  }

  private void debug(String s) {
    LOG.debug("tname=" + Thread.currentThread().getName() + ": " + s);
  }

  private void error(String s) {
    LOG.error("tname=" + Thread.currentThread().getName() + ": " + s);
  }

  private String valuesToString(Map<String, ByteIterator> values) {
    StringBuilder sb = new StringBuilder("{");
    values.forEach((k, v) -> {
        sb.append("(k=" + k + ",v=" + (v == null? "null" : v.toString()) + ")");
    });
    return sb.append("}").toString();
  }

  /**
   * Build a deterministic value given the key information.
   */
  private String buildDeterministicValue(String key, String fieldkey) {
    debug("buildDeterministicValue key=" + key + ", fieldKey=" + fieldkey);
    int size = fieldlengthgenerator.nextValue().intValue();
    StringBuilder sb = new StringBuilder(size);
    sb.append(key);
    sb.append(':');
    sb.append(fieldkey);
    while (sb.length() < size) {
      sb.append(':');
      sb.append(sb.toString().hashCode());
    }
    sb.setLength(size);

    debug("buildDeterministicValue value=" + sb.toString());
    return sb.toString();
  }

  private String hash1(String id) {
    MessageDigest messageDigest = null;
    try {
      messageDigest = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
      return null;
    }
    messageDigest.update(id.getBytes(),0, id.length());
    return new BigInteger(1,messageDigest.digest()).toString(16);
  }

  private Device generateDevice(final int numSensors) {
    debug("generateDevice");
    final Device d = new Device();
    d.id = hash(String.valueOf(deviceKeyChooser.nextValue().longValue()));
    d.name  = "device-" + d.id;
    d.sensors.addAll(generateSensors(d.id, numSensors));
    return d;
  }

  private Collection<Sensor> generateSensors(String deviceId, final int sensorNum) {
    debug("generateSensors deviceId=" + deviceId + ", sensorNum=" + sensorNum);
    final Collection<Sensor> sensors = new ArrayList<>(sensorNum);
    for (int i = 0; i < sensorNum; i ++) {
      sensors.add(generateSensor(deviceId, 0));
    }
    return sensors;
  }

  private Sensor generateSensor(String deviceId, final int numMeasurements) {
    final Sensor s = new Sensor();
    s.id = hash(String.valueOf(sensorKeyChooser.nextValue().longValue()));
    s.deviceId = deviceId;
    s.name = "sensor-" + s.id;
    s.measurementList.addAll(generateMeasurements(s.id, numMeasurements));
    return s;
  }

  private List<Measurement> generateMeasurements(String sensorId, final int numMeasurements) {
    final List<Measurement> measurements = new ArrayList<>(numMeasurements);
    for (int i = 0; i < numMeasurements; i ++) {
      final int measurmentType = RANDOM.nextInt(Measurement.Type.values().length);
      final int totalMeasurements = MEASUREMENT_NUM.incrementAndGet();
      final long nextMeasurementId = measurementKeyChooser.nextValue().longValue();
      final String measurementIdHash = hash(String.valueOf(nextMeasurementId));
      LOG.debug("totalMeasurements={}, nextMeasurementId={}, hash={}.", totalMeasurements, nextMeasurementId, measurementIdHash);
      measurements.add(Measurement.Type.values()[measurmentType].generate(measurementIdHash, sensorId));
    }
    return measurements;
  }

  /**
   * Default hash to be used for IDs.
   */
  private String hash(String str) {
    final String hash = hash1(str);
    debug("hashed '" + str + "' to '" + hash + "'");
    return hash;
  }

  static class BooleanHolder {
    Boolean b;
  }

  /**
   * Do one insert operation. Because it will be called concurrently from multiple client threads,
   * this function must be thread safe. However, avoid synchronized, or the threads will block waiting
   * for each other, and it will be difficult to reach the target throughput. Ideally, this function would
   * have no side effects other than DB operations.
   */
  @Override
  public boolean doInsert(DB db, Object threadstate) {
    debug("doInsert");
    final Device d = generateDevice(NUM_SENSORS);

    final BooleanHolder b = new BooleanHolder();
    b.b = doRetryInsert(db, Device.TABLE_NAME, d.id, d.dbValues());

    d.sensors.forEach(s -> {
      b.b &= doRetryInsert(db, Sensor.TABLE_NAME, s.id, s.dbValues());
    });

    return b.b;
  }

  private boolean doRetryInsert(DB db, String table, String dbkey, Map<String, ByteIterator> values) {
    debug("doRetryInsert table=" + table + ", dbKey=" + dbkey + ", values=" + valuesToString(values));

    Status status;
    int numOfRetries = 0;
    do {
      status = db.insert(table, dbkey, values);
      if (null != status && status.isOk()) {
        break;
      }
      // Retry if configured. Without retrying, the load process will fail
      // even if one single insertion fails. User can optionally configure
      // an insertion retry limit (default is 0) to enable retry.
      if (++numOfRetries <= insertionRetryLimit) {
        System.err.println("Retrying insertion, retry count: " + numOfRetries);
        try {
          // Sleep for a random number between [0.8, 1.2)*insertionRetryInterval.
          int sleepTime = (int) (1000 * insertionRetryInterval * (0.8 + 0.4 * Math.random()));
          Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
          break;
        }

      } else {
        System.err.println("Error inserting, not retrying any more. number of attempts: " + numOfRetries +
            "Insertion Retry Limit: " + insertionRetryLimit);
        break;

      }
    } while (true);

    return null != status && status.isOk();
  }

  /**
   * Do one transaction operation. Because it will be called concurrently from multiple client
   * threads, this function must be thread safe. However, avoid synchronized, or the threads will block waiting
   * for each other, and it will be difficult to reach the target throughput. Ideally, this function would
   * have no side effects other than DB operations.
   */
  @Override
  public boolean doTransaction(DB db, Object threadstate) {
    String operation = operationchooser.nextString();
    debug("doTransaction operation = " + operation);
    if (operation == null) {
      return false;
    }

    switch (operation) {
    case "READ":
      doTransactionRead(db);
      break;
    case "UPDATE":
      doTransactionUpdate(db);
      break;
    case "INSERT":
      doTransactionInsert(db);
      break;
    case "SCAN":
      doTransactionScan(db);
      break;
    default:
      doTransactionReadModifyWrite(db);
    }

    return true;
  }

  /**
   * Results are reported in the first three buckets of the histogram under
   * the label "VERIFY".
   * Bucket 0 means the expected data was returned.
   * Bucket 1 means incorrect data was returned.
   * Bucket 2 means null data was returned when some data was expected.
   */
  private void verifyRow(String key, HashMap<String, ByteIterator> cells) {
    debug("verifyRow key=" + key + ", values=" + valuesToString(cells));
    Status verifyStatus = Status.OK;
    long startTime = System.nanoTime();
    if (!cells.isEmpty()) {
      for (Map.Entry<String, ByteIterator> entry : cells.entrySet()) {
        if (!entry.getValue().toString().equals(buildDeterministicValue(key, entry.getKey()))) {
          verifyStatus = Status.UNEXPECTED_STATE;
          break;
        }
      }
    } else {
      // This assumes that null data is never valid
      verifyStatus = Status.ERROR;
    }
    long endTime = System.nanoTime();
    measurements.measure("VERIFY", (int) (endTime - startTime) / 1000);
    measurements.reportStatus("VERIFY", verifyStatus);
  }

  private String selectRandomMeasurementId() {
    debug("select random measurement for total measurement #" + MEASUREMENT_NUM.get());
    return selectRandomHashId(MEASUREMENT_NUM.get());
  }

  private String selectRandomSensorId() {
    debug("select random sensor for total sensor #" + SENSOR_NUM.get());
    return selectRandomHashId(SENSOR_NUM.get());
  }

  private String selectRandomHashId(final int intId) {
    debug("Selecting random hash id for max limit " + intId);
    return hash(String.valueOf(RANDOM.nextInt(intId)));
  }

  private void doTransactionRead(DB db) {

    if (MEASUREMENT_NUM.get() == 0) {
      debug("measurement num is 0, skipping read.");
      return;
    }

    // choose a random key

    String measurementId = selectRandomMeasurementId();

    debug("doTransactionRead measurementId=" + measurementId + ", fieldNamesSet=" + fieldnamesSet);

    HashMap<String, ByteIterator> cells = new HashMap<>();
    Status status = db.read(Measurement.TABLE_NAME, measurementId, fieldnamesSet, cells);
    debug("doTransactionRead read status=" + status + ", cells=" + cells);
    if (!status.isOk()) {
      LOG.error("doTransactionRead error for measurementId={}, status={}.", measurementId, status);
    }

    if (dataintegrity) {
      verifyRow(measurementId, cells);
    }
  }

  private void doTransactionReadModifyWrite(DB db) {

    if (MEASUREMENT_NUM.get() == 0) {
      debug("measurement num is 0, skipping read-modify-write.");
      return;
    }

    final String measurementId = selectRandomMeasurementId();

    debug("doTransactionReadModifyWrite measurementId=" + measurementId);

    // do the transaction
    HashMap<String, ByteIterator> cells = new HashMap<>();

    long ist = measurements.getIntendedtartTimeNs();
    long st = System.nanoTime();
    Status read = db.read(Measurement.TABLE_NAME, measurementId, fieldnamesSet, cells);
    debug("readModifyWrite read status=" + read + ", fieldNames=" + fieldnamesSet + ", cells=" + cells);
    if (!read.isOk()) {
      LOG.error("Could not find record, will skip read-modify-write operation.");
      return;
    }

    cells.put(Measurement.VALUES, new StringByteIterator(cells.get(Measurement.VALUES).toString() + "-u"));
    debug("readModifyWrite update measurementId=" + measurementId + ", values=" + cells);
    Status update = db.update(Measurement.TABLE_NAME, measurementId, cells);
    debug("readModifyWrite update status=" + update);

    long en = System.nanoTime();

    if (dataintegrity) {
      verifyRow(measurementId, cells);
    }

    measurements.measure("READ-MODIFY-WRITE", (int) ((en - st) / 1000));
    measurements.measureIntended("READ-MODIFY-WRITE", (int) ((en - ist) / 1000));
  }

  private void doTransactionScan(DB db) {

    if (MEASUREMENT_NUM.get() == 0) {
      debug("measurement num is 0, skipping scan.");
      return;
    }

    // choose a random key
    final String measurementId = selectRandomMeasurementId();

    //  choose a random scan length
    final int len = 30 + RANDOM.nextInt(30);

    debug("doTransactionScan measurementId=" + measurementId + ", startkeyname=" + measurementId + ", len=" + len
      + ", fieldNamesSet=" + fieldnamesSet);

    Status status = db.scan(Measurement.TABLE_NAME, measurementId, len, fieldnamesSet, new Vector<>());
    debug("doTransactionScan scan status = " + status);
  }

  private void doTransactionUpdate(DB db) {

    if (MEASUREMENT_NUM.get() == 0) {
      debug("measurement num is 0, skipping update.");
      return;
    }

    // choose a random key
    final String measurementId = selectRandomMeasurementId();

    HashMap<String, ByteIterator> values;

    values = buildSingleValue(measurementId, Measurement.VALUES);

    debug("doTransactionUpdate measurementId=" + measurementId + ", values=" + values);
    Status update = db.update(Measurement.TABLE_NAME, measurementId, values);
    debug("doTransactionUpdate update status: " + update);
    Map<String, ByteIterator> readMap = new HashMap<>();
    Status read = db.read(Measurement.TABLE_NAME, measurementId, fieldnamesSet, readMap);
    debug("doTransactionUpdate read status " + read);
    debug("doTransactionUpdate values " + readMap);

    if (dataintegrity) {
      verifyRow(measurementId, values);
    }
  }

  private void doTransactionInsert(DB db) {

    if (SENSOR_NUM.get() == 0) {
      debug("sensor num is 0, skipping insert.");
      return;
    }

    String sensorId = selectRandomSensorId();
    Measurement measurement = generateMeasurements(sensorId, 1).get(0);

    try {
      debug("doTransactionInsert measurement id =" + measurement.id + ", values=" + measurement.dbValues());
      Status insert = db.insert(Measurement.TABLE_NAME, measurement.id, measurement.dbValues());
      debug("doTransactionInsert insert status = " + insert);
      if (insert.isOk()) {
        int currentMeasurements = MEASUREMENT_NUM.incrementAndGet();
        debug("current measurements: " + currentMeasurements);
      }
    } catch (Exception e) {
      LOG.error("Error while inserting in DB ", e);
      throw e;
    }
  }

  /**
   * Creates a weighted discrete values with database operations for a workload to perform.
   * Weights/proportions are read from the properties list and defaults are used
   * when values are not configured.
   * Current operations are "READ", "UPDATE", "INSERT", "SCAN" and "READMODIFYWRITE".
   *
   * @param p The properties list to pull weights from.
   * @return A generator that can be used to determine the next operation to perform.
   * @throws IllegalArgumentException if the properties object was null.
   */
  private static DiscreteGenerator createOperationGenerator(final Properties p) {
    if (p == null) {
      throw new IllegalArgumentException("Properties object cannot be null");
    }
    final double readproportion = Double.parseDouble(
        p.getProperty(READ_PROPORTION_PROPERTY, READ_PROPORTION_PROPERTY_DEFAULT));
    final double updateproportion = Double.parseDouble(
        p.getProperty(UPDATE_PROPORTION_PROPERTY, UPDATE_PROPORTION_PROPERTY_DEFAULT));
    final double insertproportion = Double.parseDouble(
        p.getProperty(INSERT_PROPORTION_PROPERTY, INSERT_PROPORTION_PROPERTY_DEFAULT));
    final double scanproportion = Double.parseDouble(
        p.getProperty(SCAN_PROPORTION_PROPERTY, SCAN_PROPORTION_PROPERTY_DEFAULT));
    final double readmodifywriteproportion = Double.parseDouble(p.getProperty(
        READMODIFYWRITE_PROPORTION_PROPERTY, READMODIFYWRITE_PROPORTION_PROPERTY_DEFAULT));

    final DiscreteGenerator operationchooser = new DiscreteGenerator();
    if (readproportion > 0) {
      operationchooser.addValue(readproportion, "READ");
    }

    if (updateproportion > 0) {
      operationchooser.addValue(updateproportion, "UPDATE");
    }

    if (insertproportion > 0) {
      operationchooser.addValue(insertproportion, "INSERT");
    }

    if (scanproportion > 0) {
      operationchooser.addValue(scanproportion, "SCAN");
    }

    if (readmodifywriteproportion > 0) {
      operationchooser.addValue(readmodifywriteproportion, "READMODIFYWRITE");
    }
    return operationchooser;
  }
}
