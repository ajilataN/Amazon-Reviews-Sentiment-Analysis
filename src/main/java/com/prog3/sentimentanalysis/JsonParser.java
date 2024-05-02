package com.prog3.sentimentanalysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonParser {
    public static String extractReviewText(String reviewJson, String topic) {
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

}
