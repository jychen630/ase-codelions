package com.team.mcp.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.team.mcp.analytics.SentimentModel.Label;
import com.team.mcp.analytics.SentimentModel.Prediction;
import org.junit.jupiter.api.Test;

final class SentimentModelTest {

  @Test
  void predict_nullOrBlank_returnsNeutralZero() {
    SentimentModel model = SentimentModel.loadDefault();

    Prediction p1 = model.predict(null);
    Prediction p2 = model.predict("   ");

    assertEquals(Label.NEUTRAL, p1.label());
    assertEquals(0.0, p1.score(), 1e-9);

    assertEquals(Label.NEUTRAL, p2.label());
    assertEquals(0.0, p2.score(), 1e-9);
  }

  @Test
  void predict_handlesPositiveNegativeAndPunctuation() {
    SentimentModel model = SentimentModel.loadDefault();

    Prediction pos = model.predict("This is AMAZING!!!");
    assertEquals(Label.POSITIVE, pos.label());
    assertTrue(pos.score() > 0.5);

    Prediction neg = model.predict("This is terrible...");
    assertEquals(Label.NEGATIVE, neg.label());
    assertTrue(neg.score() < -0.5);

    // Only punctuation should strip to empty and have neutral score
    Prediction neutral = model.predict("!!!");
    assertEquals(Label.NEUTRAL, neutral.label());
    assertEquals(0.0, neutral.score(), 1e-9);

    // Mixed text with punctuation around tokens
    Prediction mixed = model.predict("I love, but also hate, this.");
    // Score should be close to zero but non-neutral threshold may vary,
    // just assert it is within a small band around 0.
    assertTrue(Math.abs(mixed.score()) < 3.0);
  }
}

