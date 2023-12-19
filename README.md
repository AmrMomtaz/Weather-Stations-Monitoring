# Weather-Stations-Monitoring

This project servers as the course project for **CSE-4E3: Designing Data Intensive Applications** which aims at:
1) Implementing different integration patterns discuessed in [**Enterprise Integration Patterns**](https://www.enterpriseintegrationpatterns.com/).
2) Connecting microservices using Kafka and gRPC.
3) Deploying using Kuberneets and Docker.
4) Implementing [**Bitcask**](https://riak.com/assets/bitcask-intro.pdf) (A Log-Structured Hash Table).

## Overview

The Internet of Things (IoT) is an important source of data streams in the modern digital world.
The "Things" are huge in count and emit messages in very high frequency which flood the
global internet. Hence, efficient stream processing is inevitable.

One use case is the distributed weather stations use case. Each "**weather station**" emits
readings for the current weather status to the "**base central station**" for persistence and
analysis. In this project, the architecture of a weather monitoring system is implemented using 
microservices.

## System Architecture

The system is composed of three stages:
* **Data Acquisition:** multiple weather stations fetch data from the weather data service using _gRPC_ calls and feed a queueing service (Kafka) with their readings.
* **Data Processing & Archiving:** The base central station is consuming the streamed
data and archiving all data in the form of Parquet files.
* **Indexing:** two variants of index are maintained:
    * Key-value store (Bitcask) for the latest reading from each individual station.
    * ElasticSearch / Kibana that are running over the Parquet files.
 
![image](https://github.com/AmrMomtaz/Weather-Stations-Monitoring/assets/61145262/e0b80ce6-ad12-460c-b7fc-cb4d78810fef)

**_Log4j2_** is used for logging in all the microservices (modules).<br>
The next sections describes each microservice, its configurations and how to build it.

## Weather Data Service

Provides the current weather data to the weather stations performing the following steps: 
1) Receives a gRPC request from a weather station requesting the current weather data.
2) Makes API call to [**Open-Meteo**](https://open-meteo.com/) (**Internet connection is necessary**).
3) Sends the gRPC response back to the weather station.

Important notes:
* The gRPC server runs on its default port 6565 (Can be changed in the source code).
* Both the "weather data service" and the "weather station" use the same exact protobuf schema.
* This service only fetches the response and sends it back to the station after parsing it to match the protobuf schema. It doesn't modify any data from the received API response.

To build the jar, go to the project's directory and run ```mvn clean package``` and it will be build in the target dicrectory with name _WeatherDataService-1.0-SNAPSHOT-jar-with-dependencies.jar_.

## Weather Station

Each weather station outputs a status message every **1 second** to report its sampled
weather status. The weather message have the following schema:

```yaml
{
   "station_id": 1, # Long (randomly generared)
   "s_no": 1, # Long auto-incremental with each message per service
   "battery_status": "low", # String of (low, medium, high)
   "status_timestamp": 1681521224, # Long Unix timestamp
   "weather": {
      "humidity": 35, # Integer percentage
      "temperature": 100, # Integer in fahrenheit
      "wind_speed": 13, # Integer km/h
   }
}
```

The weather station randomly drops messages on a **10%** rate and randomly changes the battery status according to the following:
* Low = **30%** of messages.
* Medium = **40%** of messages.
* High = **30%** of messages.

The weather station performs the following:
1) Sends gRPC request to weather data service and receives weather data response (message).
2) Drops unused fields and filters the message [**"Contents Filter Pattern"**](https://www.enterpriseintegrationpatterns.com/patterns/messaging/ContentFilter.html).
3) Enriches the message with the missing weather station state fields [**"Contents Enricher Pattern"**](https://www.enterpriseintegrationpatterns.com/patterns/messaging/DataEnricher.html).
4) Stores invalid messages in a seperate channel [**"Invalid Message Channel"**](https://www.enterpriseintegrationpatterns.com/patterns/messaging/InvalidMessageChannel.html).
5) Feeds the message to Kafka service (while dropping **10%** of them).

The API response, filtered message and enriched message's are avaialable in the project's resources.<br>
To build the jar, go to the project's directory and run ```mvn clean package``` and it will be build in the target's dicrectory with name _WeatherStation-1.0-SNAPSHOT-shaded.jar_.

## Base Central Station

The base central station consumes the streamed data from Kafka (polls the data every 100ms) and persists the data in **Bitcask Store** where it keeps the latest reading of each weather station in the store (as described in the next section).<br>

It also persists the data in **Elasticsearch** archiving all the weather statuses history for the all stations in parquet files partitioned by time which is then imported in Elasticsearch.
Each parquet file contains **1,000** weather messages. The parquet files are written in the _parquet_data_ directory and they are named by the timestamp of the first received weather status.
After receiving the 1,000 weather messages, the parquet file is flushed and all its data is bulk imported into elasticsearch in the _"weather_data"_ index.<br>
To build the jar, go to the project's directory and run ```mvn clean package``` and the jar will be located in the target's directory with name _BaseCentralStation-1.0-SNAPSHOT-shaded.jar_.


## Bitcask Store

Bitcask store is used to store the latest individual reading for each weather station. This implementation follows exactly the Bitcask [paper](https://riak.com/assets/bitcask-intro.pdf) where the segment's size is **1,000 KB**. 
However, there are two points I've skipped in my implementation which are:
1) No error detection and correction (CRC).
2) No concurrency control over multiple instances on the same bitcask root directory.

For completness, the merge function could be implemented much more effeciently by grouping and sorting records to be read and written sequentially instead randomly accessing the records (which is currently implemented).

When creating the Bitcask handler, you specify the store's root directory (if it doesn't exist, it would be created it starting a new bitcask store). The data files are stored with the following naming format:
```
Data files:
"epoch_{epoch_num}_{fileId}"
Where the {epoch_num} determines the number of merges which have occured

Hint files:
"hint_epoch_{epoch_num}_{fileId}"
Created after merge operations as described in the paper
```
The Bitcask store is imported as a dependency in the base central station (you must run ```mvn install``` in the project's directory so it would be available in your local maven repository).
