package com.prog3.sentimentanalysis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class SentimentAnalysisApplication {

	public static void main(String[] args) {
		SpringApplication.run(SentimentAnalysisApplication.class, args);

		// Set the duration for application execution (in minutes)
		int executionDurationMinutes = 10;

		// Schedule a task to shutdown the application after the specified duration
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		scheduler.schedule(() -> {
			System.out.println("Application execution completed. Shutting down...");
			// Shutdown the application gracefully
			System.exit(0);
		}, executionDurationMinutes, TimeUnit.MINUTES);

		// Shutdown the scheduler when the application completes
		scheduler.shutdown();
	}

}