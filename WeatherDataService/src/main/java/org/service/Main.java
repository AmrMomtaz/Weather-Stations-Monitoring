/*
 * Created by Amr Momtaz.
 */

package org.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

@SpringBootApplication
public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        // Used if needed to set the GRPC server port explicitly instead of 6565
//        ServerBuilder serverBuilder = ServerBuilder.forPort(1717);
//        serverBuilder.addService(new Server());
//        serverBuilder.build().start().awaitTermination();

        SpringApplication.run(Main.class);
    }
}