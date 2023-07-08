/*
 * Created by Amr Momtaz.
 */

package org.service;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

/**
 * Used just for testing the GRPC connection and the received response.
 */
public class MockClient {
    public static void main(String[] args) {
        ManagedChannel managedChannel = ManagedChannelBuilder.forAddress("localhost", 6565).usePlaintext().build();

        WeatherDataServiceGrpc.WeatherDataServiceBlockingStub blockingStub = WeatherDataServiceGrpc.newBlockingStub(managedChannel);

        WeatherDataOuterClass.WeatherData weatherData = blockingStub.getWeatherData
                (WeatherDataOuterClass.WeatherDataRequest.newBuilder().setClientId("1").build());
        System.out.println(weatherData.toString());
    }
}
