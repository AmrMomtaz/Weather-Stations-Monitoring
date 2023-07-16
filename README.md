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
readings for the current weather status to the "**central base station**" for persistence and
analysis. In this project, you will be required to implement the architecture of a weather
monitoring system.

## System Architecture

The system is composed of three stages:
* **Data Acquisition:** multiple weather stations fetch data from the weather data service using _gRPC_ calls and feed a queueing service (Kafka) with their readings.
* **Data Processing & Archiving:** The base central station is consuming the streamed
data and archiving all data in the form of Parquet files.
* **Indexing:** two variants of index are maintained
    * Key-value store (Bitcask) for the latest reading from each individual station.
    * ElasticSearch / Kibana that are running over the Parquet files.
 
![image](https://github.com/AmrMomtaz/Weather-Stations-Monitoring/assets/61145262/e0b80ce6-ad12-460c-b7fc-cb4d78810fef)

**_Log4j2_** is used for logging in all the modules.<br>
The next sections describes the requirements and implementation of each module (microservice).

## Weather Data Service

Provides the current weather data to the weather stations performing the following steps: 
1) Receives a gRPC request from a weather station requesting the the current weather data.
2) Makes API call to [**Open-Meteo**](https://open-meteo.com/).
3) Sends the response back to the weather station using RPC.

Important notes:
* The gRPC server runs on its default port 6565 (Althought this can be changed as mentioned in the code).
* Both the "weather data service" and the "weather station" uses the same exact protobuf schema.
* This service only fetches the response and and sends it back to the station after parsing it to match the protobuf schema. And it doesn't add, remove or change any data on the received response.

## Weather Station

Each weather station outputs a status message every **1 second** to report its sampled
weather status. The weather message should have the following schema:

![image](https://github.com/AmrMomtaz/Weather-Stations-Monitoring/assets/61145262/bc58489e-c809-4bee-83c1-27c6a509fe98)

The weather station randomly change the battery status according the following:
* Low = **30%** of messages per service.
* Medium = **40%** of messages per service.
* High = **30%** of messages per service.

And it randomly drops messages on a **10%** rate.
