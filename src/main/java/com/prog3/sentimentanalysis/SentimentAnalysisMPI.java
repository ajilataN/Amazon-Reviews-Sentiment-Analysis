package com.prog3.sentimentanalysis;
import mpi.*;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

public class SentimentAnalysisMPI {
    private static String topic = null;
    private static WebSocketSession session;
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
            System.err.println("Please specify the topic using the --topic=music argument (music, toys, pet-supplies, automotive, sport).");
            MPI.Finalize();
            return;
        }

        if (me == 0) {
            // Master process
            masterProcess();
        } else {
            // Worker processes
            workerProcess(me);
        }

        MPI.Finalize();
    }

    private static void masterProcess() {
        ConfigurableApplicationContext context = SpringApplication.run(SentimentAnalysisApplication.class, new String[]{});
        // Get WebSocketClient bean from context
        WebSocketClient webSocketClient = context.getBean(WebSocketClient.class);
        // Connect to the server using webSocketClient
        webSocketClient.connectToServer();
        // Create session
        webSocketClient.setSession(session);
        // Subscribe to the selected topic
        webSocketClient.subscribeToTopic(topic);



        context.close(); // Close the context after use
    }

    private static void workerProcess(int rank) {
        // Worker process logic
        // Connect to WebSocket server, receive tasks, process them
    }
}
