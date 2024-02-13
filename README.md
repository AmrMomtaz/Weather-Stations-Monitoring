# Weather-Stations-Monitoring

This project servers as the course project for **CSE-4E3: Designing Data Intensive Applications** which aims at:
1) Applying the microservices architecture.
2) Implementing different integration patterns discuessed in [**Enterprise Integration Patterns**](https://www.enterpriseintegrationpatterns.com/).
3) Connecting microservices using Kafka and gRPC.
4) Deploying using Kuberneets and Docker.
5) Implementing [**Bitcask**](https://riak.com/assets/bitcask-intro.pdf) (A Log-Structured Hash Table).

## Overview

The Internet of Things (IoT) is an important source of data streams in the modern digital world.
The "Things" are huge in count and emit messages in very high frequency which flood the
global internet. Hence, efficient stream processing is inevitable.

One use case is the distributed weather stations. Each "**weather station**" emits
readings for the current weather status to the "**base central station**" for persistence and
analysis. In this project, the architecture of a weather monitoring system is implemented using 
microservices.

## System Architecture

The system is composed of three stages:
* **Data Acquisition:** multiple weather stations fetch data from the weather data service using _gRPC_ calls and feed a queueing service (_Kafka_) with their readings.
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
* Both the "weather data service" and the "weather station" use the same exact protobuf schema (located in the project's resources).
* This service only fetches the response and sends it back to the station after parsing it to match the protobuf schema. It doesn't modify the received API response.

To build the jar, go to the project's directory and run ```mvn clean package``` and it will be build in the target dicrectory with name _WeatherDataService-1.0-SNAPSHOT-jar-with-dependencies.jar_.

## Weather Station

Each weather station outputs a status message every **1 second** to report its sampled
weather status. The weather message have the following schema:

```yaml
{
   "station_id": 1, # Long (randomly generated)
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
5) Feeds the message to Kafka service (drops **10%** of them).

The API response, filtered message and enriched message are located in the project's resources.<br>
To build the jar, go to the project's directory and run ```mvn clean package``` and it will be build in the target's dicrectory with name _WeatherStation-1.0-SNAPSHOT-shaded.jar_.

## Base Central Station

The base central station consumes the streamed data from Kafka (polls the data every 100ms) and persists the data in **Bitcask Store** where it keeps the latest reading of each weather station in the store (as described in the next section).<br>

The central station flattens the incoming json objects and renames its fields (for better readability). It initializes the **_weather_data_** index in elasticsearch and configures its options and
mappings using _**IndexConfigs.json**_ (located in the project's resources).<br>
To write parquet records, the json objects are converted to **Avro** (using the avro schema defined in _**AvroSchema.avsc**_) 
which are then written as Parquet records. This is because Parquet doesn't have its own set of Java objects. Instead, it reuses the objects from other formats like Avro [(link)](https://stackoverflow.com/questions/39858856/json-object-to-parquet-format-using-java-without-converting-to-avrowithout-usin). Note that you must have **HADOOP_HOME** and **hadoop.home.dir** set in your enviroment variables.<br>

It persists the data in **Elasticsearch** archiving all the weather data history of the all stations in parquet files partitioned by time.
Each parquet file contains **1,000** weather messages and it is stored in the _parquet_data_ directory and they are named after the timestamp of the first received weather message in this file.
After receiving 1,000 weather messages, the parquet file is flushed and all its data is bulk imported into elasticsearch in the _"weather_data"_ index.<br>

To build the jar, go to the project's directory and run ```mvn clean package``` and the jar will be created in the target's directory named _BaseCentralStation-1.0-SNAPSHOT-shaded.jar_.


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

## Elasticsearch & Kibana

[**Elasticsearch**](https://www.elastic.co/) is a search engine which provides a distributed, multitenant-capable full-text search engine with an HTTP web interface 
and schema-free JSON documents. [**Kibana**](https://www.elastic.co/kibana) is a source-available data visualization dashboard software for Elasticsearch.<br>

I've created a data view for the _weather_data_ index and deployed three weather stations and waited for three parquet files (located in the BaseCentralStation's resources) to be imported to elasticsearch and got the following results:

![Screenshot from 2023-12-21 09-16-22](https://github.com/AmrMomtaz/Weather-Stations-Monitoring/assets/61145262/6f93c89b-182f-47c5-98bb-1f64b70ba03b)

A total of 3,000 records are imported where the distrubution of the battery status of the weather stations is (40.7% medium, 29.8% low, 29.5% high) which matches the required ratios.

From selecting a single weather station and inspecting the sequence number we get the following:

![image](https://github.com/AmrMomtaz/Weather-Stations-Monitoring/assets/61145262/c5000ae1-4c3c-44f3-b51f-c1b08cfb4efd)

The record's count is 991 where the maximum sequence number is 1,109 which means that **89.3%** of records are delivered and **10.7%** are dropped.


## Configurations & Deployment (Docker & Kuberneets)

This part describes the system configurations and the subsequent subsections describes the system's deployement.

The **WeatherDataService** runs **SpringBootApplication** and the **gRPC** server. It fetches the data from open-meteo by sending a **GET** request and it makes five trials trying to fetch the data and in case of internet failure the service hangs.

**Kafka** starts using **ZooKeeper** where the Kafka Broker config advertised.listeners is set to PLAINTEXT:localhost and its number of partitions is set to one. The Kafka topic's name which connects the weather stations with the base central stations is _"weather_data_topic"_ where the keys/values are serialized and deserialized using **StringSerializer**.

The **BaseCentralStation** connects to Kafka using the group ID "base_central_station" and creates a new Bitcask handler with a root directory named _"bitcask_store"_. And it sets the index name in elasticsearch to _"weather_data"_.

**Elasticsearch & Kibana** runs with SSL/TLS mode **disabled** and elasticsearch forms a single node cluster.

The port and version of each service are described as following (all the ports are exposed with the same number):

| **Service**       | **Port Number** | **Version** |
|:------------------|:---------------:|:-----------:|
| **gRPC**          | 6565            | 4.7.1       |
| **ZooKeeper**     | 2181            | 3.4.13      |
| **Kafka**         | 9092            | 2.6.0       |
| **Elasticsearch** | 9200            | 8.10.4      |
| **Kibana**        | 5601            | 8.10.4      |

And finally, The following table shows my development enviroment versions used:

|                   | **Version**     |
|:------------------|:---------------:|
| **Java**          | 17.0.2          |
| **Maven**         | 3.9.0           |
| **Docker**        | 25.0.3          |
| **Docker-Compose**| 2.24.5          |
| **Kuberneets**    | 1.28.3          |

### Docker

To build the images, go to the **_deployement_** directory in _WeatherDataService_, _WeatherStation_ & _BaseCentralStation_ and run the following command ```docker build -t <image-name> .``` replacing the \<image-name\> accordingly.

Also, These images are hosted on [DockerHub](https://hub.docker.com/r/amrmomtaz/weather-stations-monitoring/tags) and they can be pulled directly.<br>
The following [image](https://hub.docker.com/r/johnnypark/kafka-zookeeper/) is used for _Kafka_ including _Zookeeper_, and this [one](https://hub.docker.com/r/nshou/elasticsearch-kibana) for _Elasticsearch_ and _Kibana_.

The following commands are used to run the containers:
```bash
# Run Kafka
docker run -d --name Kafka -p 2181:2181 -p 9092:9092 -e ADVERTISED_HOST=localhost -e NUM_PARTITIONS=1 johnnypark/kafka-zookeeper:2.6.0

# Run Kibana and Elasticsearch
docker run -d --name Elasticsearch_Kibana -p 9200:9200 -p 5601:5601 -e SSL_MODE=false -e discovery.type=single-node nshou/elasticsearch-kibana

# Run the WeatherDataService
docker run -d --name WeatherDataService --network=host amrmomtaz/weather-stations-monitoring:weather-data-service

# Run the WeatherStation
docker run -d --name WeatherStation --network=host amrmomtaz/weather-stations-monitoring:weather-station

# Run the BaseCentralStation
docker run -d --name BaseCentralStation --network=host amrmomtaz/weather-stations-monitoring:base-central-station
```
Please note the following points in the previous script:
* The commands' order must be maintained.
* The _host_ network can be changed (for security reasons). To create a seperate network use this command ```docker network create {my-bridge-network}```.
* More weather stations can be deployed by running the forth command changing the container's name.

Finally, a _docker-compose.yml_ file is available in the root directory which can be used directly to build and run everything.<br>
Go to the repo root directory and run the following command where you can specify the desired number of weather stations:
```bash
docker-compose up -d --scale weather_station={Number of Weather Stations}
```
The following screenshot shows the running containers on _docker-desktop_:

![image](https://github.com/AmrMomtaz/Weather-Stations-Monitoring/assets/61145262/f76763b5-eb40-43e5-9ad2-95f9b8eb9e22)

### Kuberneets

The system was deployed locally on one node **k8s** cluster using [**minikube**](https://minikube.sigs.k8s.io/docs/start/) and [**kubectl**](https://kubernetes.io/docs/tasks/tools/) was used to control this cluster.<br>
After having installed _minikube_ and _kubectl_, follow these commands to deploy the services:
```bash
# Start minikube cluster
minikube start --driver docker


```
