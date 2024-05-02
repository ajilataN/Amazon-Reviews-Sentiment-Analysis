package com.prog3.sentimentanalysis;
import mpi.*;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class SentimentAnalysisMPI {
    private static String topic = null;
    private static WebSocketSession session;
//    private final TextWebSocketHandler webSocketHandler;

    // Constructor
    public SentimentAnalysisMPI() {

    }
    public static void main(String[] args) throws InterruptedException {
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
            System.err.println("Please specify the topic using the --topic=music argument (music, toys, pet-supplies, automotive, sport).");
            MPI.Finalize();
            return;
        }

        SentimentAnalysisMPI sentimentAnalysisMPI = new SentimentAnalysisMPI();

        if (me == 0) {
            // Master process
            sentimentAnalysisMPI.masterProcess();
        }
        else {
            // Worker processes
            sentimentAnalysisMPI.workerProcess(me);
        }

        MPI.Finalize();
    }

    private void masterProcess() throws InterruptedException {
        System.out.println("Master");
        connectToServer();

        while (true) {
            try {
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void workerProcess(int rank) {
        // Create an instance of SentimentAnalyzer
        SentimentAnalyzer sentimentAnalyzer = new SentimentAnalyzer();

        // Receive messages from master process
        while (true) {
            // Buffer to receive the message
            byte[] messageBytes = new byte[4096]; // Adjust the size as per your message size

            // Receive message from master process
            MPI.COMM_WORLD.Recv(messageBytes, 0, messageBytes.length, MPI.BYTE, 0, MPI.ANY_TAG);

            // Convert received bytes to string
            String receivedMessage = new String(messageBytes).trim();

            System.out.println("what worker process receives "+receivedMessage);

            // Perform sentiment analysis on the received message
            String sentiment = sentimentAnalyzer.analyzeSentiment(receivedMessage);

            // Print the sentiment result
            System.out.println("Worker Process " + rank + " - Sentiment: " + sentiment);
        }
    }
    private void connectToServer(){
        org.springframework.web.socket.client.WebSocketClient webSocketClient = new StandardWebSocketClient();
        String serverUri = "wss://prog3.student.famnit.upr.si/sentiment";
        webSocketClient.doHandshake(new TextWebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) {
                System.out.println("WebSocket connection established.");
                SentimentAnalysisMPI.session = session;
                subscribeToTopic(topic);
            }
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                String receivedMessage = message.getPayload();
                String extractedReview = JsonParser.extractReviewText(receivedMessage, topic);
                System.out.println("Message: " + extractedReview);

                // Convert the extracted review to bytes
                byte[] reviewBytes = extractedReview.getBytes();

                // Get the number of worker processes
                int numWorkers = MPI.COMM_WORLD.Size() - 1; // Subtracting 1 for master process

                // Distribute the review to worker processes
                for (int workerRank = 1; workerRank <= numWorkers; workerRank++) {
                    MPI.COMM_WORLD.Send(reviewBytes, 0, reviewBytes.length, MPI.BYTE, workerRank, 0);
                    System.out.println("Sent review to worker process " + workerRank);
                }

            }

        }, serverUri);
    }
    public void subscribeToTopic(String topic) {
        try {
            session.sendMessage(new TextMessage("topic: " + topic));
            System.out.println("Subscribed to topic: " + topic);
        } catch (Exception e) {
            // TODO: Check more robust option
            e.printStackTrace();
        }
    }


}
