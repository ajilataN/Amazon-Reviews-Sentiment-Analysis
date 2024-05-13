# Sentiment Analysis on Amazon reviews

The project constists of two application.

## 1. Sequential and Parallel Implementation - Spring Boot
One application is run for the sequentiall and parallel part. It is a Spring Boot Application and accepts two parameters.
--mode=sequential --topic=music
The Spring Boot application connects to a WebSocket server to receive text messages, subscribes to a specified topic and performs sentiment analysis to every review.

## 2. Distributed Implementation
The other application is for the distributed implementation of the project.
It needs only one parameter before running 
--topic=music


## Measures
All of the modes (sequential, parallel and distributed) measure how many reviews were analyzed in a second.
The measures are saved to a separate files with distinguishable names:

sequential_review_counts, parallel_review_counts or distributed_review_counts.

The measuring goes on until you stop the application. 
