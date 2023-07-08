/*
 * Created by Amr Momtaz.
 */

package org.service;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class Main {
    public static void main(String[] args) {
        ManagedChannel managedChannel = ManagedChannelBuilder.forAddress("localhost", 6565).usePlaintext().build();
        org.service.WeatherDataServiceGrpc.WeatherDataServiceBlockingStub blockingStub = org.service.WeatherDataServiceGrpc.newBlockingStub(managedChannel);
        org.service.WeatherDataOuterClass.WeatherData weatherData = blockingStub.getWeatherData
                (org.service.WeatherDataOuterClass.WeatherDataRequest.newBuilder().setClientId("1").build());
        System.out.println(weatherData.toString());
    }
}