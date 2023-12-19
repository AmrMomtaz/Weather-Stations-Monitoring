/*
 * Created by Amr Momtaz.
 */

package org.service;

import org.apache.http.HttpHost;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.*;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.hadoop.util.HadoopInputFile;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.elasticsearch.xcontent.XContentType;

/**
 * Manages the connection and imports data to Elasticsearch.
 */
public class ElasticsearchManager {

    private static final Logger logger;
    private static final RestHighLevelClient elasticsearchClient;
    private static final String INDEX_NAME = "weather_data";
    private static final int ELASTICSEARCH_PORT_NUMBER = 9200;

    static {
        logger = LogManager.getLogger(ElasticsearchManager.class);
        elasticsearchClient = new RestHighLevelClientBuilder
            (RestClient.builder(new HttpHost("localhost", ELASTICSEARCH_PORT_NUMBER)).build())
            .setApiCompatibilityMode(true)
            .build();
    }

    /**
     * Imports the data of the given parquet file path to elasticsearch.
     */
    public static void importDataToElasticsearch(String parquetFilePath) {
        try {
            // Create a Parquet reader
            ParquetReader<GenericRecord> parquetReader = AvroParquetReader.<GenericRecord>builder
                            (HadoopInputFile.fromPath(new Path(parquetFilePath), new Configuration()))
                    .withDataModel(GenericData.get())
                    .build();

            // Create a bulk request for indexing
            BulkRequest bulkRequest = new BulkRequest();

            GenericRecord record;
            while ((record = parquetReader.read()) != null) {
                bulkRequest.add(new IndexRequest(INDEX_NAME).source(record, XContentType.JSON));
            }
            parquetReader.close();

            BulkResponse bulkResponse = elasticsearchClient.bulk(bulkRequest, RequestOptions.DEFAULT);
            if (! bulkResponse.hasFailures()) logger.debug("Data imported to elasticsearch successfully.");
            else throw new RuntimeException(bulkResponse.buildFailureMessage());
        }
        catch (Exception e) {
            logger.error("Couldn't import parquet file data to elasticsearch");
            throw new RuntimeException(e);
        }
    }
}
