package com.prog3.sentimentanalysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *  The purpose of this class is to parse the Json objects that we get from the server.
* The message is of this form:
* {"music":"{\"reviewerID\": \"A3V5XBBT7OZG5G\", \"asin\": \"0001393774\", \"reviewerName\": \"gflady\", \"verified\": true, \"reviewText\": \"One of my very favourite albums from one of my very favourite singers.  I was happy to see I could replace the old worn cassettes from years ago.\", \"overall\": 5.0, \"reviewTime\": \"02 23, 2016\", \"summary\": \"One of my very favourite albums from one of my very favourite singers\", \"unixReviewTime\": 1456185600}"}
*  and the relevant part for analyzing is just the contents of \"reviewText\"
* The method extractReviewText returns only the contents of the \"reviewText\".
* */
public class JsonParser {
    public static String extractReviewText(String reviewJson, String topic) {
        try {
            // Parse the JSON string
            ObjectMapper objectMapper = new ObjectMapper();
            // Create a jsonNode and read the tree
            JsonNode jsonNode = objectMapper.readTree(reviewJson);
            JsonNode internalNode = jsonNode.get(topic);

            // Check if the message is of the expected format
            if (internalNode != null && !internalNode.isNull()) {
                String reviewText = internalNode.asText();
                JsonNode innerNode = objectMapper.readTree(reviewText);
                JsonNode reviewTextNode = innerNode.get("reviewText");

//                System.out.println("Review text: "+ reviewTextNode);

                // Once we have the review text, we convert it to text
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
