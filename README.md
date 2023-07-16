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
    * Key-value store (Bitcask) for the latest reading from each individual station
    * ElasticSearch / Kibana that are running over the Parquet files.
 
![image](https://github.com/AmrMomtaz/Weather-Stations-Monitoring/assets/61145262/e0b80ce6-ad12-460c-b7fc-cb4d78810fef)

