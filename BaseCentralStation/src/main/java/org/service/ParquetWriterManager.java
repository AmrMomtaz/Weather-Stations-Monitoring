/*
 * Created by Amr Momtaz.
 */

package org.service;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

/**
 * Manager for writing parquet files.
 */
public class ParquetWriterManager {

    // Constants
    private static final Logger logger;
    private static final Schema AVRO_SCHEMA;
    private static final int MAXIMUM_OF_RECORDS_IN_PARQUET_FILE;

    // State variables
    private static ParquetWriter<GenericRecord> parquetWriter;
    private static int currentRecordIdx;

    static {
        logger = LogManager.getLogger(ParquetWriterManager.class);
        try {
            AVRO_SCHEMA = Schema.parse(new File(Objects.requireNonNull
                (ParquetWriterManager.class.getClassLoader().getResource("AvroSchema.avsc")).getFile()));
        } catch (IOException e) {
            logger.error("Couldn't find the AVRO Schema");
            throw new RuntimeException(e);
        }
        MAXIMUM_OF_RECORDS_IN_PARQUET_FILE = 10;
        resetParquetWriter();
    }

    /**
     * Writes the incoming response message to the parquet file.
     */
    public static void writeParquetRecord(JSONObject response) {
        JSONObject flattenedResponse = flattenResponse(response);
        GenericRecord avroRecord = jsonToAvro(flattenedResponse);
        try {
            Objects.requireNonNull(parquetWriter).write(avroRecord);
            currentRecordIdx++;
        } catch (IOException e) {
            logger.error("Couldn't write the avro record in the parquet file: " + e);
            throw new RuntimeException(e);
        }
        if (currentRecordIdx == MAXIMUM_OF_RECORDS_IN_PARQUET_FILE)
            resetParquetWriter();
    }

    //
    // Private Methods
    //

    /**
     * Flattens the incoming JSONObject and renames its fields.
     */
    private static JSONObject flattenResponse(JSONObject response) {
        try {
            JSONObject flattenedResponse = new JSONObject();
            JSONObject weatherObject = response.getJSONObject("weather");

            flattenedResponse.put("Station_ID", response.getLong("station_id"));
            flattenedResponse.put("Sequence_Number", response.getLong("s_no"));
            flattenedResponse.put("Battery_Status", response.getString("battery_status"));
            flattenedResponse.put("Timestamp", response.getLong("status_timestamp"));
            flattenedResponse.put("Humidity", weatherObject.getInt("humidity"));
            flattenedResponse.put("Temperature", weatherObject.getInt("temperature"));
            flattenedResponse.put("Wind_Speed", weatherObject.getInt("wind_speed"));
            return flattenedResponse;
        }
        catch (Exception e) {
            logger.error("Couldn't flatten the incoming response");
            throw new RuntimeException(e);
        }
    }

    /**
     * Parses the response to Avro generic record.
     */
    private static GenericRecord jsonToAvro(JSONObject response) {
        try {
            DatumReader<GenericRecord> datumReader = new SpecificDatumReader<>(AVRO_SCHEMA);
            Decoder decoder = DecoderFactory.get().jsonDecoder(AVRO_SCHEMA, response.toString());
            return datumReader.read(null, decoder);
        } catch (Exception e) {
            logger.error("Couldn't parse the response to Avro generic record");
            throw new RuntimeException(e);
        }
    }

    /**
     * Resets the current parquet writer and create a new writer with a new output file.
     */
    private static void resetParquetWriter() {
        try {
            if (parquetWriter != null) parquetWriter.close();
            currentRecordIdx = 0;
            parquetWriter = AvroParquetWriter
                    .<GenericRecord>builder(new Path(System.currentTimeMillis() + ".parquet"))
                    .withConf(new Configuration())
                    .withCompressionCodec(CompressionCodecName.SNAPPY)
                    .withSchema(AVRO_SCHEMA)
                    .build();
        } catch (IOException e) {
            logger.error("Couldn't reset the writer instance");
            throw new RuntimeException(e);
        }
    }
}
