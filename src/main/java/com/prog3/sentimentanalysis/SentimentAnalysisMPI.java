package com.prog3.sentimentanalysis;
import mpi.*;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.Queue;

public class SentimentAnalysisMPI {
    private static String topic = null;
    private static WebSocketSession session;
    private static Queue<String> reviewQueue = new LinkedList<>();
    private static final String OUTPUT_FILE = "reviews_per_second.txt";

    // Method to write results to a text file
    private void writeResultsToFile(int reviewsPerSecond) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(OUTPUT_FILE, true))) {
            writer.println("Reviews processed per second: " + reviewsPerSecond);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Constructor
    public SentimentAnalysisMPI() {    }
    public static void main(String[] args) {
        MPI.Init(args);
        int me = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

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
        if (me == 0) { sentimentAnalysisMPI.masterProcess(); }
        else { sentimentAnalysisMPI.workerProcess(me); }

        MPI.Finalize();
    }

    private void masterProcess() {
        System.out.println("Master");
        connectToServer();
    }

    private void workerProcess(int rank) {
        // Create an instance of SentimentAnalyzer
        SentimentAnalyzer sentimentAnalyzer = new SentimentAnalyzer();

        // Initialize variables for tracking reviews processed per second
        int reviewsProcessedThisSecond = 0;
        long startTime = System.currentTimeMillis();

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
                reviewsProcessedThisSecond++;
            }

            // Check if one second has elapsed
            long currentTime = System.currentTimeMillis();
            if (currentTime - startTime >= 1000) {
                // Print the rate of reviews processed per second
                System.out.println("Reviews processed per second: " + reviewsProcessedThisSecond);
                writeResultsToFile(reviewsProcessedThisSecond);
                // Reset the count for the next second
                reviewsProcessedThisSecond = 0;
                // Update the start time
                startTime = currentTime;
            }
        }
    }

    private void connectToServer(){
        org.springframework.web.socket.client.WebSocketClient webSocketClient = new StandardWebSocketClient();
        String serverUri = "wss://prog3.student.famnit.upr.si/sentiment";
        webSocketClient.doHandshake(new TextWebSocketHandler() {
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
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                System.out.println("Received a new message.");
                String receivedMessage = message.getPayload();
                // Extract the review text from the received json message
                String extractedReview = JsonParser.extractReviewText(receivedMessage, topic);

                // Check if extractedReview is null before continuing
                if (extractedReview != null) {
                    if (!reviewQueue.contains(extractedReview)) {
                        reviewQueue.offer(extractedReview);
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
    }
    private int nextWorkerRank = 1; // Track the next worker process to send a review to
    private void distributeReviews() {
        int numWorkers = MPI.COMM_WORLD.Size() - 1;

        while (!reviewQueue.isEmpty()) {
            String review = reviewQueue.poll();
            byte[] reviewBytes = review.getBytes();

            // Send the review to the next worker process in round-robin fashion
            MPI.COMM_WORLD.Send(reviewBytes, 0, reviewBytes.length, MPI.BYTE, nextWorkerRank, 0);
            System.out.println("Sent review to worker process " + nextWorkerRank);

            // Increment nextWorkerRank and wrap around if needed
            nextWorkerRank++;
            if (nextWorkerRank > numWorkers) {
                nextWorkerRank = 1;
            }
        }
    }
}
