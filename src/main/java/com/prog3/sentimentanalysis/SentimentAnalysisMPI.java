package com.prog3.sentimentanalysis;
import mpi.*;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SentimentAnalysisMPI {
    // Valid topic from command arguments
    private static String topic = null;
    // Web socket session for the server connection
    private static WebSocketSession session;
    // Queue to store reviews
    private static Queue<String> reviewQueue = new LinkedList<>();
    // Output file for the results
    private static final String OUTPUT_FILE = "distributed_review_counts.txt";

    // Method to write results to a text file
    private static void saveToFile(int reviewsPerSecond) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(OUTPUT_FILE, true))) {
            writer.println("Reviews processed per second: " + reviewsPerSecond);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to clear the output file
    private static void clearOutputFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(OUTPUT_FILE))) {
            writer.print("");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Constructor
    public SentimentAnalysisMPI() {    }
    // MAIN method
    public static void main(String[] args) {

        // Set the duration for application execution (in minutes)
        int executionDurationMinutes = 10;

        // Schedule a task to shutdown the application after the specified duration
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(() -> {
            System.out.println("Application execution completed. Shutting down...");
            // Finalize MPI environment and exit the application
            MPI.Finalize();
            System.exit(0);
        }, executionDurationMinutes, TimeUnit.MINUTES);

        // Environment initialization
        MPI.Init(args);

        // Communicator info
        int me = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

        // Handle error if there are less than 2 processors free
        // At least 2 processors needed in the configuration for MPI
        if (size < 2) {
            System.err.println("This MPI program requires at least 2 processes.");
            MPI.Finalize();
            return;
        }

        // Reading topic from command line arguments
        for (String arg : args) {
            if (arg.startsWith("--topic=")) {
                topic = arg.substring(8).toLowerCase();
            }
        }

        // If the topic is not specified, return
        if (topic == null) {
            System.err.println("Please specify the topic using the --topic=<topic> argument (change <topic> for music, toys, pet-supplies, automotive or sport).");
            MPI.Finalize();
            return;
        }

        // Decide if master or worker process is called
        SentimentAnalysisMPI sentimentAnalysisMPI = new SentimentAnalysisMPI();
        if (me == 0) {
            // Master process in case rank is 0
            sentimentAnalysisMPI.masterProcess(size);
        }
        else {
            // Worker processes otherwise
            sentimentAnalysisMPI.workerProcess(me);
        }

        MPI.Finalize();
    }

    // Master process logic
    // Handles connection, subscription to a topic and parses the text review
    private void masterProcess(int numWorkers) {
        clearOutputFile(); // Clear the output file at the start
        org.springframework.web.socket.client.WebSocketClient webSocketClient = new StandardWebSocketClient();
        String serverUri = "wss://prog3.student.famnit.upr.si/sentiment";
        webSocketClient.doHandshake(new TextWebSocketHandler() {
            // After connection, create a session, then send a message to subscribe to topic
            @Override
            public void afterConnectionEstablished(WebSocketSession session) {
                System.out.println("WebSocket connection established.");
                // Subscribe to topic
                SentimentAnalysisMPI.session = session;
                try {
                    session.sendMessage(new TextMessage("topic: " + topic));
//                    System.out.println("Subscribed to topic: " + topic);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            // Handle the json
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
//                System.out.println("Received a new message.");
                // New message received
                String receivedMessage = message.getPayload();
                // Extract the review text from the received json message
                String extractedReview = JsonParser.extractReviewText(receivedMessage, topic);

                // Check if extractedReview is null before continuing
                if (extractedReview != null) {
                    if (!reviewQueue.contains(extractedReview)) {
                        reviewQueue.offer(extractedReview);
                        // Distribute reviews among workers
                        distributeReviews();
                    } else {
                        System.out.println("Skipping processing. Duplicate review received.");
                    }
                } else {
                    // Handle the case where extractedReview is null
                    System.out.println("Extracted review is null. Skipping processing.");
                }
            }

        }, serverUri);
        // Schedule a task to log the number of reviews processed per second
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            int totalReviewsPerSecond = 0;

            // Receive the number of processed reviews from each worker
            for (int i = 1; i < numWorkers; i++) {
                int[] reviewsPerSecond = new int[1];
                MPI.COMM_WORLD.Recv(reviewsPerSecond, 0, 1, MPI.INT, i, 1);
                totalReviewsPerSecond += reviewsPerSecond[0];
            }

            System.out.println("Total reviews processed per second: " + totalReviewsPerSecond);
            saveToFile(totalReviewsPerSecond);
        }, 1, 1, TimeUnit.SECONDS);
    }

    // Worker process logic
    // Creates a sentiment Analyzer instance and  evaluate the review
    private void workerProcess(int rank) {
        // Create an instance of SentimentAnalyzer
        SentimentAnalyzer sentimentAnalyzer = new SentimentAnalyzer();

        // Initialize variables for tracking reviews processed per second
        final int[] analyzedReviews = {0};
        // Schedule a task to log the number of reviews processed per second
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            int[] reviewsPerSecond = {analyzedReviews[0]};
            MPI.COMM_WORLD.Send(reviewsPerSecond, 0, 1, MPI.INT, 0, 1);
            analyzedReviews[0] = 0; // Reset count after sending
        }, 1, 1, TimeUnit.SECONDS);

        // Receive messages from master process
        while (true) {
            // Buffer to receive the message
            byte[] messageBytes = new byte[8192];

            // Receive message from master process
            Status status = MPI.COMM_WORLD.Recv(messageBytes, 0, messageBytes.length, MPI.BYTE, 0, MPI.ANY_TAG);

            if (status.tag == 0) {
                // Convert received bytes to string
                String receivedMessage = new String(messageBytes).trim();
                // Perform sentiment analysis on the received message
                String sentiment = sentimentAnalyzer.analyzeSentiment(receivedMessage);
                // Print the sentiment result
                System.out.println("Worker Process " + rank + " - Sentiment: " + sentiment);
                // Increment the count of reviews processed this second
                analyzedReviews[0]++;
            }
        }
    }
    private int nextWorkerRank = 1; // Track the next worker process to send a review
    // Method to distribute the messages to worker processes
    private void distributeReviews() {
        // Number of available processes
        int numWorkers = MPI.COMM_WORLD.Size() - 1;

        // Distribute the available reviews
        while (!reviewQueue.isEmpty()) {
            // Get the next review from the queue
            String review = reviewQueue.poll();
            // Get the bytes to send to the worker process
            byte[] reviewBytes = review.getBytes();

            // Send the review to the next worker process in round-robin order
            MPI.COMM_WORLD.Send(reviewBytes, 0, reviewBytes.length, MPI.BYTE, nextWorkerRank, 0);
//            System.out.println("Sent review to worker process " + nextWorkerRank);

            // Increment nextWorkerRank and wrap around
            nextWorkerRank++;
            if (nextWorkerRank > numWorkers) {
                nextWorkerRank = 1;
            }
        }
    }
}
