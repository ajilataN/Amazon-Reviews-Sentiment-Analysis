package com.prog3.sentimentanalysis;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;

import java.util.Properties;

/**
 *  This class serves to make the proper sentiment analysis to a review.
* It uses the Standford CoreNLP library for that purpose.
* */
public class SentimentAnalyzer {
    private StanfordCoreNLP pipeline;

    public SentimentAnalyzer() {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, parse, sentiment");
        this.pipeline = new StanfordCoreNLP(props);
    }

    public String analyzeSentiment(String reviewText) {
        if (reviewText != null) {
            // Perform sentiment analysis
            Annotation annotation = new Annotation(reviewText);
            pipeline.annotate(annotation);
            return annotation.get(CoreAnnotations.SentencesAnnotation.class)
                    .get(0)
                    .get(SentimentCoreAnnotations.SentimentClass.class);
        } else {
            return null;
        }
    }

}
