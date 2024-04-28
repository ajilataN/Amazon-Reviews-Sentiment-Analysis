package com.prog3.sentimentanalysis;

import java.util.List;
import java.util.Arrays;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@Component
public class WebSocketClient extends TextWebSocketHandler implements CommandLineRunner {

    private static final String SEQUENTIAL_MODE = "sequential";
    private static final String PARALLEL_MODE = "parallel";
    private ExecutorService executorService;
    private String mode;
    private String topic;
    private WebSocketSession session;
    private int reviewCount = 0;
    private ScheduledExecutorService scheduler;
    private SentimentAnalyzer sentimentAnalyzer;

    public WebSocketClient() {
        this.sentimentAnalyzer = new SentimentAnalyzer();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("Connected to WebSocket server.");
        this.session = session;
        subscribeToTopic(topic);
    }

    public void setSession(WebSocketSession session) {
        this.session = session;
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

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        System.out.println("Received message: " + message.getPayload());
        String receivedMessage = message.getPayload();
        String extractedReview = extractReviewText(receivedMessage);

        if (mode.equals(SEQUENTIAL_MODE)) {
            analyzeSentimentSequential(extractedReview);
        } else if (mode.equals(PARALLEL_MODE)) {
            analyzeSentimentParallel(extractedReview);
        } else {
            System.err.println("Invalid mode: " + mode);
        }
    }

    private String extractReviewText(String reviewJson) {
        try {
            // Parse the JSON string
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(reviewJson);

            JsonNode internalNode = jsonNode.get(topic);

            if (internalNode != null && !internalNode.isNull()) {

                String reviewText = internalNode.asText();
                JsonNode innerNode = objectMapper.readTree(reviewText);
                JsonNode reviewTextNode = innerNode.get("reviewText");

                System.out.println("Review text: "+ reviewTextNode);

                if (reviewTextNode != null) {
                    return reviewTextNode.asText();
                } else {
                    System.err.println("Review text not found within internal node.");
                    return null;
                }
            } else {
                System.err.println("Error parsing internal json node!");
                return null;
            }
        } catch (Exception e) {
            System.err.println("Error parsing JSON: " + e.getMessage());
            return null;
        }
    }

    private void analyzeSentimentSequential(String reviewText) {
        if (reviewText != null) {
            String sentiment = sentimentAnalyzer.analyzeSentiment(reviewText);
            if (sentiment != null) {
                System.out.println("Sentiment for review: " + sentiment);
            } else {
                System.err.println("Review text not found in the message.");
            }
        } else {
            System.err.println("Review text not found in the message.");
        }
        reviewCount++;
    }

    private void analyzeSentimentParallel(String reviewText) {
        if (executorService == null) {
            // Initialize thread pool
            executorService = Executors.newCachedThreadPool();
        }
        // Submit task to thread pool
        executorService.submit(() -> analyzeSentimentSequential(reviewText));
    }

    public void connectToServer(){
        org.springframework.web.socket.client.WebSocketClient webSocketClient = new StandardWebSocketClient();
        String serverUri = "wss://prog3.student.famnit.upr.si/sentiment";
        webSocketClient.doHandshake(this, serverUri);
    }

    @Override
    public void run(String... args) throws Exception {
        if (args.length < 2) {
            System.err.println("Please specify the mode (sequential or parallel) as the first argument." +
                    "and the topic as the second argument (music, toys, pet-supplies, automotive, sport).");
            return;
        }

        // Extracting mode and topic from command-line arguments
        for (String arg : args) {
            if (arg.startsWith("--mode=")) {
                mode = arg.substring(7).toLowerCase();
            } else if (arg.startsWith("--topic=")) {
                topic = arg.substring(8).toLowerCase();
            }
        }

        if (!mode.equals(SEQUENTIAL_MODE) && !mode.equals(PARALLEL_MODE)) {
            System.err.println("Invalid mode: " + mode);
            return;
        }

        // List of allowed topics
        List<String> allowedTopics = Arrays.asList("movies", "electronics", "music", "toys", "pet-supplies", "automotive", "sport");
        if (!allowedTopics.contains(topic.toLowerCase())) {
            System.err.println("Invalid topic: " + topic);
            return;
        }

        // Connect to the server to get reviews
        connectToServer();

        // Schedule the task to output review counts every second
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            System.out.println("Reviews Analyzed per Second: " + reviewCount);
            reviewCount = 0; // Reset after 1 second
        }, 0, 1, TimeUnit.SECONDS);
    }
}

