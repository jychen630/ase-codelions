package com.team.mcp.analytics;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Tiny sentiment "model" used in Iteration-2 analytics.
 *
 * <p>This is intentionally lightweight and self-contained:
 * no external ML libraries, no network calls, and no large
 * model files. It behaves like a simple linear classifier
 * over a handful of sentiment keywords.
 *
 * <p>The goal is to demonstrate an ML-style analytics feature
 * (sentiment roll-up) without adding heavy dependencies.
 */
public final class SentimentModel {

  /** Weight used for moderately positive tokens. */
  private static final double POS_MEDIUM = 1.5;

  /** Weight used for strongly positive tokens. */
  private static final double POS_STRONG = 2.0;

  /** Weight used for very strongly positive tokens. */
  private static final double POS_VERY_STRONG = 2.5;

  /** Weight used for moderately negative tokens. */
  private static final double NEG_MEDIUM = -1.5;

  /** Weight used for strongly negative tokens. */
  private static final double NEG_STRONG = -2.0;

  /** Weight used for very strongly negative tokens. */
  private static final double NEG_VERY_STRONG = -2.5;

  /** Threshold for deciding non-neutral sentiment magnitude. */
  private static final double NEUTRAL_THRESHOLD = 0.5;

  /**
   * Sentiment labels for predictions.
   */
  public enum Label {

    /** Positive sentiment label. */
    POSITIVE,

    /** Negative sentiment label. */
    NEGATIVE,

    /** Neutral sentiment label. */
    NEUTRAL
  }

  /**
   * One prediction: label + signed score.
   *
   * <p>Score is a rough intensity measure:
   * positive -&gt; &gt; 0, negative -&gt; &lt; 0, neutral -&gt; 0.
   *
   * @param label predicted sentiment label
   * @param score signed sentiment score
   */
  public record Prediction(Label label, double score) { }

  /** Per-token sentiment weights. */
  private final Map<String, Double> weights;

  /** Bias term (rarely used here, but keeps it "model-like"). */
  private final double bias;

  /**
   * Creates a new sentiment model with the given weights and bias.
   *
   * @param modelWeights per-token sentiment weights
   * @param modelBias bias term added to the summed weights
   */
  private SentimentModel(final Map<String, Double> modelWeights,
                         final double modelBias) {
    this.weights = modelWeights;
    this.bias = modelBias;
  }

  /**
   * Build a small default model with a handful of
   * positive/negative keywords.
   *
   * <p>You can think of this as a tiny pre-trained model
   * baked into the code.
   *
   * @return a sentiment model instance with default weights
   */
  public static SentimentModel loadDefault() {
    final Map<String, Double> w = new HashMap<>();

    // Very small positive vocabulary
    w.put("good", POS_MEDIUM);
    w.put("great", POS_STRONG);
    w.put("excellent", POS_VERY_STRONG);
    w.put("amazing", POS_VERY_STRONG);
    w.put("love", POS_STRONG);
    w.put("like", 1.0);
    w.put("happy", POS_MEDIUM);
    w.put("awesome", POS_STRONG);

    // Very small negative vocabulary
    w.put("bad", NEG_MEDIUM);
    w.put("terrible", NEG_VERY_STRONG);
    w.put("awful", NEG_VERY_STRONG);
    w.put("hate", NEG_STRONG);
    w.put("worse", NEG_MEDIUM);
    w.put("worst", NEG_VERY_STRONG);
    w.put("sad", NEG_MEDIUM);
    w.put("angry", NEG_MEDIUM);

    // Small bias towards NEUTRAL (0.0 keeps it balanced)
    final double modelBias = 0.0;

    return new SentimentModel(w, modelBias);
  }

  /**
   * Predict sentiment for a single status/tweet.
   *
   * <p>Algorithm:
   * <ol>
   *   <li>Lowercase + simple tokenization on whitespace.</li>
   *   <li>Look up each token's weight (if any) and sum.</li>
   *   <li>Add bias.</li>
   *   <li>Map final score to POSITIVE / NEGATIVE / NEUTRAL
   *       via simple thresholds.</li>
   * </ol>
   *
   * @param text raw status text (may be null/empty)
   * @return prediction with label and score
   */
  public Prediction predict(final String text) {
    if (text == null || text.isBlank()) {
      return new Prediction(Label.NEUTRAL, 0.0);
    }

    final String norm = text.toLowerCase(Locale.ROOT);
    double score = bias;

    for (String rawToken : norm.split("\\s+")) {
      final String token = stripPunctuation(rawToken);
      if (token.isEmpty()) {
        continue;
      }
      final Double w = weights.get(token);
      if (w != null) {
        score += w;
      }
    }

    final Label label;
    if (score > NEUTRAL_THRESHOLD) {
      label = Label.POSITIVE;
    } else if (score < -NEUTRAL_THRESHOLD) {
      label = Label.NEGATIVE;
    } else {
      label = Label.NEUTRAL;
    }

    return new Prediction(label, score);
  }

  /**
   * Remove leading/trailing punctuation from a token.
   *
   * @param token raw token
   * @return cleaned token
   */
  private static String stripPunctuation(final String token) {
    int start = 0;
    int end = token.length();

    while (start < end && !Character.isLetterOrDigit(token.charAt(start))) {
      start++;
    }
    while (end > start && !Character.isLetterOrDigit(token.charAt(end - 1))) {
      end--;
    }
    return start >= end ? "" : token.substring(start, end);
  }
}

