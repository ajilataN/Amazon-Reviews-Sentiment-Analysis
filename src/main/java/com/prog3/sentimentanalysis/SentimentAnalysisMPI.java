package com.prog3.sentimentanalysis;
import mpi.*;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class SentimentAnalysisMPI {
    private static String topic = null;
    private static WebSocketSession session;
    private final TextWebSocketHandler webSocketHandler;
    private static BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
    public SentimentAnalysisMPI() {
        messageQueue = new LinkedBlockingQueue<>();
        webSocketHandler = new TextWebSocketHandler(){
            @Override
            public void afterConnectionEstablished(WebSocketSession session) {
                System.out.println("WebSocket connection established.");
                // Assign the session to the session field for further use if needed
                SentimentAnalysisMPI.session = session;
                subscribeToTopic(topic);
            }
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                String receivedMessage = message.getPayload();
                String extractedReview = JsonParser.extractReviewText(receivedMessage, topic);
                System.out.println("Message: " + extractedReview);
                // Store the message in the queue
                messageQueue.offer(extractedReview);
            }
        };
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
        String message = messageQueue.take();
        System.out.println("I got the message: "+ message);
//        while (true) {
//            try {
//
//                Thread.sleep(Long.MAX_VALUE);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
    }

    private void workerProcess(int rank) {

    }

    private void connectToServer(){
        org.springframework.web.socket.client.WebSocketClient webSocketClient = new StandardWebSocketClient();
        String serverUri = "wss://prog3.student.famnit.upr.si/sentiment";
        webSocketClient.doHandshake(webSocketHandler, serverUri);
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
