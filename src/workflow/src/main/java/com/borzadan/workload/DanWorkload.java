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

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

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

  private final List<String> fieldnames = Arrays.asList(Measurement.VALUES, Measurement.TIMESTAMP, Measurement.TYPE, Measurement.SENSOR_ID);;
  private final Set<String> fieldnamesSet = new HashSet<>(fieldnames);

  /**
   * The name of the property for the field length distribution. Options are "uniform", "zipfian"
   * (favouring short records), "constant", and "histogram".
   * <p>
   * If "uniform", "zipfian" or "constant", the maximum field length will be that specified by the
   * fieldlength property. If "histogram", then the histogram will be read from the filename
   * specified in the "fieldlengthhistogram" property.
   */
  public static final String FIELD_LENGTH_DISTRIBUTION_PROPERTY = "fieldlengthdistribution";

  /**
   * The default field length distribution.
   */
  public static final String FIELD_LENGTH_DISTRIBUTION_PROPERTY_DEFAULT = "constant";

  /**
   * The name of the property for the length of a field in bytes.
   */
  public static final String FIELD_LENGTH_PROPERTY = "fieldlength";

  /**
   * The default maximum length of a field in bytes.
   */
  public static final String FIELD_LENGTH_PROPERTY_DEFAULT = "100";

  /**
   * The name of the property for the minimum length of a field in bytes.
   * The name of the property for the minimum length of a field in bytes.
   */
  public static final String MIN_FIELD_LENGTH_PROPERTY = "minfieldlength";

  /**
   * The default minimum length of a field in bytes.
   */
  public static final String MIN_FIELD_LENGTH_PROPERTY_DEFAULT = "1";

  /**
   * The name of a property that specifies the filename containing the field length histogram (only
   * used if fieldlengthdistribution is "histogram").
   */
  public static final String FIELD_LENGTH_HISTOGRAM_FILE_PROPERTY = "fieldlengthhistogram";

  /**
   * The default filename containing a field length histogram.
   */
  public static final String FIELD_LENGTH_HISTOGRAM_FILE_PROPERTY_DEFAULT = "hist.txt";

  /**
   * Generator object that produces field lengths.  The value of this depends on the properties that
   * start with "FIELD_LENGTH_".
   */
  protected NumberGenerator fieldlengthgenerator;

  /**
   * The name of the property for deciding whether to read one field (false) or all fields (true) of
   * a record.
   */
  public static final String READ_ALL_FIELDS_PROPERTY = "readallfields";

  /**
   * The default value for the readallfields property.
   */
  public static final String READ_ALL_FIELDS_PROPERTY_DEFAULT = "true";

  protected boolean readallfields;

  /**
   * The name of the property for deciding whether to write one field (false) or all fields (true)
   * of a record.
   */
  public static final String WRITE_ALL_FIELDS_PROPERTY = "writeallfields";

  /**
   * The default value for the writeallfields property.
   */
  public static final String WRITE_ALL_FIELDS_PROPERTY_DEFAULT = "false";

  protected boolean writeallfields;

  /**
   * The name of the property for deciding whether to check all returned
   * data against the formation template to ensure data integrity.
   */
  public static final String DATA_INTEGRITY_PROPERTY = "dataintegrity";

  /**
   * The default value for the dataintegrity property.
   */
  public static final String DATA_INTEGRITY_PROPERTY_DEFAULT = "false";

  /**
   * Set to true if want to check correctness of reads. Must also
   * be set to true during loading phase to function.
   */
  private boolean dataintegrity;

  /**
   * The name of the property for the proportion of transactions that are reads.
   */
  public static final String READ_PROPORTION_PROPERTY = "readproportion";

  /**
   * The default proportion of transactions that are reads.
   */
  public static final String READ_PROPORTION_PROPERTY_DEFAULT = "0.95";

  /**
   * The name of the property for the proportion of transactions that are updates.
   */
  public static final String UPDATE_PROPORTION_PROPERTY = "updateproportion";

  /**
   * The default proportion of transactions that are updates.
   */
  public static final String UPDATE_PROPORTION_PROPERTY_DEFAULT = "0.05";

  /**
   * The name of the property for the proportion of transactions that are inserts.
   */
  public static final String INSERT_PROPORTION_PROPERTY = "insertproportion";

  /**
   * The default proportion of transactions that are inserts.
   */
  public static final String INSERT_PROPORTION_PROPERTY_DEFAULT = "0.0";

  /**
   * The name of the property for the proportion of transactions that are scans.
   */
  public static final String SCAN_PROPORTION_PROPERTY = "scanproportion";

  /**
   * The default proportion of transactions that are scans.
   */
  public static final String SCAN_PROPORTION_PROPERTY_DEFAULT = "0.0";

  /**
   * The name of the property for the proportion of transactions that are read-modify-write.
   */
  public static final String READMODIFYWRITE_PROPORTION_PROPERTY = "readmodifywriteproportion";

  /**
   * The default proportion of transactions that are scans.
   */
  public static final String READMODIFYWRITE_PROPORTION_PROPERTY_DEFAULT = "0.0";

  /**
   * The name of the property for the the distribution of requests across the keyspace. Options are
   * "uniform", "zipfian" and "latest"
   */
  public static final String REQUEST_DISTRIBUTION_PROPERTY = "requestdistribution";

  /**
   * The default distribution of requests across the keyspace.
   */
  public static final String REQUEST_DISTRIBUTION_PROPERTY_DEFAULT = "uniform";

  /**
   * The name of the property for adding zero padding to record numbers in order to match
   * string sort order. Controls the number of 0s to left pad with.
   */
  public static final String ZERO_PADDING_PROPERTY = "zeropadding";

  /**
   * The default zero padding value. Matches integer sort order
   */
  public static final String ZERO_PADDING_PROPERTY_DEFAULT = "1";


  /**
   * The name of the property for the min scan length (number of records).
   */
  public static final String MIN_SCAN_LENGTH_PROPERTY = "minscanlength";

  /**
   * The default min scan length.
   */
  public static final String MIN_SCAN_LENGTH_PROPERTY_DEFAULT = "1";

  /**
   * The name of the property for the max scan length (number of records).
   */
  public static final String MAX_SCAN_LENGTH_PROPERTY = "maxscanlength";

  /**
   * The default max scan length.
   */
  public static final String MAX_SCAN_LENGTH_PROPERTY_DEFAULT = "1000";

  /**
   * The name of the property for the scan length distribution. Options are "uniform" and "zipfian"
   * (favoring short scans)
   */
  public static final String SCAN_LENGTH_DISTRIBUTION_PROPERTY = "scanlengthdistribution";

  /**
   * The default max scan length.
   */
  public static final String SCAN_LENGTH_DISTRIBUTION_PROPERTY_DEFAULT = "uniform";

  /**
   * The name of the property for the order to insert records. Options are "ordered" or "hashed"
   */
  public static final String INSERT_ORDER_PROPERTY = "insertorder";

  /**
   * Default insert order.
   */
  public static final String INSERT_ORDER_PROPERTY_DEFAULT = "hashed";

  /**
   * Percentage data items that constitute the hot set.
   */
  public static final String HOTSPOT_DATA_FRACTION = "hotspotdatafraction";

  /**
   * Default value of the size of the hot set.
   */
  public static final String HOTSPOT_DATA_FRACTION_DEFAULT = "0.2";

  /**
   * Percentage operations that access the hot set.
   */
  public static final String HOTSPOT_OPN_FRACTION = "hotspotopnfraction";

  /**
   * Default value of the percentage operations accessing the hot set.
   */
  public static final String HOTSPOT_OPN_FRACTION_DEFAULT = "0.8";

  /**
   * How many times to retry when insertion of a single item to a DB fails.
   */
  public static final String INSERTION_RETRY_LIMIT = "core_workload_insertion_retry_limit";
  public static final String INSERTION_RETRY_LIMIT_DEFAULT = "0";

  /**
   * On average, how long to wait between the retries, in seconds.
   */
  public static final String INSERTION_RETRY_INTERVAL = "core_workload_insertion_retry_interval";
  public static final String INSERTION_RETRY_INTERVAL_DEFAULT = "3";

  public static final String DEBUG = "debug";
  public static final String SENSORS = "sensors";

  protected NumberGenerator keysequence;
  protected DiscreteGenerator operationchooser;
  protected NumberGenerator deviceKeyChooser;
  protected NumberGenerator sensorKeyChooser;
  protected NumberGenerator measurementKeyChooser;
  protected NumberGenerator fieldchooser;
  protected AcknowledgedCounterGenerator transactioninsertkeysequence;
  protected NumberGenerator scanlength;
  protected boolean orderedinserts;
  protected long fieldcount;
  protected long recordcount;
  protected int zeropadding;
  protected int insertionRetryLimit;
  protected int insertionRetryInterval;
  protected boolean isDebug = false;

  private Measurements measurements = Measurements.getMeasurements();

  protected static NumberGenerator getFieldLengthGenerator(Properties p) throws WorkloadException {
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
    fieldcount = fieldnames.size() - 1;

    fieldlengthgenerator = DanWorkload.getFieldLengthGenerator(p);

    recordcount =
        Long.parseLong(p.getProperty(Client.RECORD_COUNT_PROPERTY, Client.DEFAULT_RECORD_COUNT));
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
    isDebug = Boolean.valueOf(p.getProperty(DEBUG, "false"));
    SENSOR_NUM.set(Integer.valueOf(p.getProperty(SENSORS, "100")));

    System.out.println(">>> sensors = " + SENSOR_NUM.get());

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

    readallfields = Boolean.parseBoolean(
        p.getProperty(READ_ALL_FIELDS_PROPERTY, READ_ALL_FIELDS_PROPERTY_DEFAULT));
    writeallfields = Boolean.parseBoolean(
        p.getProperty(WRITE_ALL_FIELDS_PROPERTY, WRITE_ALL_FIELDS_PROPERTY_DEFAULT));

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
      deviceKeyChooser = new SequentialGenerator(insertstart, insertstart + insertcount - 1);
      sensorKeyChooser = new SequentialGenerator(insertstart, Integer.MAX_VALUE);
      measurementKeyChooser = new SequentialGenerator(insertstart, Integer.MAX_VALUE);
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
  }

  /**
   * Keep track of the total number of sensors in order to be able to randomly select one.
   */
  private static final AtomicInteger SENSOR_NUM = new AtomicInteger(0);

  /**
   * Keep track of newly generated measurements during 'run' mode.
   */
  private static final AtomicInteger MEASUREMENT_NUM = new AtomicInteger(10);

  protected String buildKeyName(long keynum) {
    return String.valueOf(keynum);
  }

  /**
   * Builds a value for a randomly chosen field.
   */
  private HashMap<String, ByteIterator> buildSingleValue(String key, String fieldkey) {
    debug(">>> buildSinglevalue key=" + key);
    HashMap<String, ByteIterator> value = new HashMap<>();

    ByteIterator data;
    if (dataintegrity) {
      data = new StringByteIterator(buildDeterministicValue(key, fieldkey));
    } else {
      // fill with random data
      data = new RandomByteIterator(fieldlengthgenerator.nextValue().longValue());
    }
    debug(">>> built new value for key=" + fieldkey);
    value.put(fieldkey, data);

    debug(">>> buildValue value = " + value);
    return value;
  }

  private void debug(String s) {
    if (isDebug) {
      System.out.println(s);
    }
  }

  /**
   * Builds values for all fields.
   */
  private HashMap<String, ByteIterator> buildValues(String key) {
    debug(">>> buildValues key=" + key);
    HashMap<String, ByteIterator> values = new HashMap<>();

    for (String fieldkey : fieldnames) {
      ByteIterator data;
      if (dataintegrity) {
        data = new StringByteIterator(buildDeterministicValue(key, fieldkey));
      } else {
        // fill with random data
        data = new RandomByteIterator(fieldlengthgenerator.nextValue().longValue());
      }
      values.put(fieldkey, data);
    }
    debug(">>> buildValues values = " + valuesToString(values));
    return values;
  }

  private String valuesToString(Map<String, ByteIterator> values) {
    StringBuilder sb = new StringBuilder("{");
    values.forEach((k, v) -> {
        sb.append("(k=" + k + ",v=" + v.toString() + ")");
    });
    return sb.append("}").toString();
  }


    /**
   * Build a deterministic value given the key information.
   */
  private String buildDeterministicValue(String key, String fieldkey) {
    debug(">>> buildDeterministicValue key=" + key + ", fieldKey=" + fieldkey);
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

    debug(">>> buildDeterministicValue value=" + sb.toString());
    return sb.toString();
  }

  private static final Random r = new Random();

  public String hash1(String id) {
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

  public String hash2(String id) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA1");
      md.reset();
      byte[] buffer = id.getBytes("UTF-8");
      md.update(buffer);
      byte[] digest = md.digest();

      String hexStr = "";
      for (int i = 0; i < digest.length; i++) {
        hexStr += Integer.toString((digest[i] & 0xff) + 0x100, 16).substring(1);
      }
      return hexStr;
    }
    catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public Device generateDevice() {
    debug("generateDevice");
    final Device d = new Device();
    d.id = hash(String.valueOf(deviceKeyChooser.nextValue().longValue()));
    d.name  = "device-" + d.id;
    d.sensors.addAll(generateSensors(d.id, SENSOR_NUM.get(), 0));
    return d;
  }

  private Collection<Sensor> generateSensors(String deviceId, final int sensorNum, final int numMeasurements) {
    debug(">>> generateSensors deviceId=" + deviceId + ", sensorNum=" + sensorNum);
    final Collection<Sensor> sensors = new ArrayList<>(sensorNum);
    for (int i = 0; i < sensorNum; i ++) {
      sensors.add(generateSensor(deviceId, numMeasurements));
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
      final int measurmentType = r.nextInt(Measurement.Type.values().length);
      final String measurementId = hash(String.valueOf(measurementKeyChooser.nextValue().longValue()));
      measurements.add(Measurement.Type.values()[measurmentType].generate(measurementId, sensorId));
    }
    return measurements;
  }

  /**
   * Default hash to be used for IDs.
   */
  private String hash(String str) {
    return hash1(str);
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
    final Device d = generateDevice();

    final BooleanHolder b = new BooleanHolder();
    b.b = doRetryInsert(db, Device.TABLE_NAME, d.id, d.dbValues());

    d.sensors.forEach(s -> {
      b.b &= doRetryInsert(db, Sensor.TABLE_NAME, s.id, s.dbValues());
    });

    return b.b;
  }

  private boolean doRetryInsert(DB db, String table, String dbkey, Map<String, ByteIterator> values) {
    debug(">>> doRetryInsert table=" + table + ", dbKey=" + dbkey);
    debug(">>> doRetryInsert values=" + valuesToString(values));

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
    debug(">>> doTransaction operation = " + operation);
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
  protected void verifyRow(String key, HashMap<String, ByteIterator> cells) {
    debug(">>> verifyRow");
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

  long nextKeynum() {
    long keynum;
    if (deviceKeyChooser instanceof ExponentialGenerator) {
      do {
        keynum = transactioninsertkeysequence.lastValue() - deviceKeyChooser.nextValue().intValue();
      } while (keynum < 0);
    } else {
      do {
        keynum = deviceKeyChooser.nextValue().intValue();
      } while (keynum > transactioninsertkeysequence.lastValue());
    }
    return keynum;
  }

  private String selectRandomMeasurementId() {
    final int measurementNum = MEASUREMENT_NUM.get();
    return hash(String.valueOf(r.nextInt(measurementNum)));
  }

  private String selectRandomSensorId() {
    final int measurementNum = SENSOR_NUM.get();
    return hash(String.valueOf(r.nextInt(measurementNum)));
  }

  public void doTransactionRead(DB db) {
    // choose a random key

    String measurementId = selectRandomMeasurementId();

    debug(">>> doTransactionRead measurementId=" + measurementId);

    HashMap<String, ByteIterator> cells = new HashMap<>();
    db.read(Measurement.TABLE_NAME, measurementId, fieldnamesSet, cells);

    if (dataintegrity) {
      verifyRow(measurementId, cells);
    }
  }

  public void doTransactionReadModifyWrite(DB db) {
    final String measurementId = selectRandomMeasurementId();

    debug(">>> doTransactionReadModifyWrite measurementId=" + measurementId);

    final HashMap<String, ByteIterator> values;

    debug(">>> updating 'values' field");
    values = buildSingleValue(measurementId, Measurement.VALUES);

    // do the transaction
    HashMap<String, ByteIterator> cells = new HashMap<>();

    long ist = measurements.getIntendedtartTimeNs();
    long st = System.nanoTime();
    db.read(Measurement.TABLE_NAME, measurementId, fieldnamesSet, cells);

    db.update(Measurement.TABLE_NAME, measurementId, values);

    long en = System.nanoTime();

    if (dataintegrity) {
      verifyRow(measurementId, cells);
    }

    measurements.measure("READ-MODIFY-WRITE", (int) ((en - st) / 1000));
    measurements.measureIntended("READ-MODIFY-WRITE", (int) ((en - ist) / 1000));
  }

  public void doTransactionScan(DB db) {
    // choose a random key
    final String measurementId = selectRandomMeasurementId();
    final int len = 100;

    debug(">>> doTransactionScan measurementId=" + measurementId + ", startkeyname=" + len);
    // choose a random scan length

    db.scan(Measurement.TABLE_NAME, measurementId, len, fieldnamesSet, new Vector<>());
  }

  public void doTransactionUpdate(DB db) {
    // choose a random key
    final String measurementId = selectRandomMeasurementId();

    debug(">>> doTransactionUpdate measurementId=" + measurementId);
    HashMap<String, ByteIterator> values;

    debug(">>> updating 'values' field");
    values = buildSingleValue(measurementId, Measurement.VALUES);

    db.update(Measurement.TABLE_NAME, measurementId, values);
  }

  public void doTransactionInsert(DB db) {
    String sensorId = selectRandomSensorId();
    Measurement measurement = generateMeasurements(sensorId, 1).get(0);

    debug(">>> doTransactionInsert measurementId=" + measurement.id);
    try {
      debug(">>> generated measurement=" + measurement);
      debug(">>> measurement db values=" + measurement.dbValues());
      Status insert = db.insert(Measurement.TABLE_NAME, measurement.id, measurement.dbValues());
      debug(">>> insert status = " + insert);
      if (insert.isOk()) {
        MEASUREMENT_NUM.incrementAndGet();
      }
    } catch (Exception e) {
      System.out.println("Problems saving record into DB " + measurement);
      e.printStackTrace(System.out);
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
  protected static DiscreteGenerator createOperationGenerator(final Properties p) {
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
