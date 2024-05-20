    package com.prog3.sentimentanalysis;

    import java.io.FileWriter;
    import java.io.IOException;
    import java.io.PrintWriter;
    import java.util.List;
    import java.util.Arrays;
    import lombok.Setter;
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

    /**
     * WebSocketClient class serves as a client that connects to the un server to receive text messages,
     * analyzes the sentiment of the received messages, and subscribes to a specified topic.
     * It contains two implementations, sequential and parallel sentiment analysis.
     * Which mode is run is decided based on the arguments from command line.
     */
    @Component
    public class WebSocketClient extends TextWebSocketHandler implements CommandLineRunner {
        // Constants to check which mode is chosen
        private static final String SEQUENTIAL_MODE = "sequential";
        private static final String PARALLEL_MODE = "parallel";
        // Output file for the results
        private static String output_file;
        private ExecutorService executorService;
        // Variable to store the mode from command line
        private String mode;
        // Variable to store the topic from command line
        private String topic;
        @Setter
        private WebSocketSession session;
        // Review counter used to check how many reviews were analyzed for a second
        private int reviewCount = 0;
        private final SentimentAnalyzer sentimentAnalyzer;
        // File writer for saving review counts
        private PrintWriter fileWriter;
        public WebSocketClient() {
            this.sentimentAnalyzer = new SentimentAnalyzer();
        }
        // After connection, create a session, then send a message to subscribe to topic
        @Override
        public void afterConnectionEstablished(WebSocketSession session) {
            System.out.println("Connected to WebSocket server.");
            this.session = session;
            subscribeToTopic(topic);
        }
        // Send a message to subscribe to a topic
        public void subscribeToTopic(String topic) {
            try {
                session.sendMessage(new TextMessage("topic: " + topic));
                System.out.println("Subscribed to topic: " + topic);
            } catch (Exception e) {
                // TODO: Check more robust option
                e.printStackTrace();
            }
        }

        // Handle the json
        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            System.out.println("Received message: " + message.getPayload());
            String receivedMessage = message.getPayload();
            String extractedReview = JsonParser.extractReviewText(receivedMessage, topic);

            if (mode.equals(SEQUENTIAL_MODE)) {
                analyzeSentimentSequential(extractedReview);
            } else if (mode.equals(PARALLEL_MODE)) {
                analyzeSentimentParallel(extractedReview);
            } else {
                System.err.println("Invalid mode: " + mode);
            }
        }

        // Method to execute the analysis in sequential order
        private void analyzeSentimentSequential(String reviewText) {
            // Check if the review exists
            if (reviewText != null) {
                // Analyze the review
                String sentiment = sentimentAnalyzer.analyzeSentiment(reviewText);
                // If the sentiment exists, print it and save to file
                if (sentiment != null) {
                    System.out.println("Sentiment for review: " + sentiment);
                } else {
                    System.err.println("Review text not found in the message.");
                }
            } else {
                System.err.println("Review text not found in the message.");
            }
            // Increase the review counter for a second
            reviewCount++;
        }
        // Method to execute the analysis in parallel order
        private void analyzeSentimentParallel(String reviewText) {
            if (executorService == null) {
                // Initialize thread pool
                executorService = Executors.newCachedThreadPool();
            }
            // Submit task to thread pool
            executorService.submit(() -> analyzeSentimentSequential(reviewText));
        }
        // Establish a server connection
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
            // Check if a valid mode is entered
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

            output_file = mode.equals(SEQUENTIAL_MODE) ? "sequential_review_counts.txt" : "parallel_review_counts.txt";

            // Clear the output file at the beginning
            clearOutputFile();

            // Connect to the server to get reviews
            connectToServer();

            // Schedule the task to output review counts every second
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(() -> {
                System.out.println("Reviews Analyzed per Second: " + reviewCount);
                saveToFile(reviewCount);
                reviewCount = 0; // Reset after 1 second
            }, 0, 1, TimeUnit.SECONDS);
        }

        // Method to write results to a text file
        private void saveToFile(int reviewsPerSecond) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(output_file, true))) {
                writer.println("Reviews processed per second: " + reviewsPerSecond);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Method to clear the output file
        private void clearOutputFile() {
            try (PrintWriter writer = new PrintWriter(new FileWriter(output_file))) {
                writer.print("");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

