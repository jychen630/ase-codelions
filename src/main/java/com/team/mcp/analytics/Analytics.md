# **Analytics — Iteration 2 (Final Version)**

---

## **What it does**

In Iteration 2, the Analytics subsystem becomes a full-featured insight layer over the user’s Mastodon timeline.
Building on the Iteration-1 foundations (hashtags, best hour, lightweight summary), the system now provides:

* Real analytics computed from live Mastodon data
* Sentiment analysis powered by a lightweight ML model (Java-based TF-IDF + logistic regression)
* Unified summary endpoint combining behavioral + sentiment insights
* 100% server-side analytics — no client libraries or Python required
* Compatible with both timeline and DB-backed search

Analytics now feels **product-ready**: richer, smarter, and grounded in real posts.

---

## **Where the code lives**

```
src/main/java/com/team/mcp/analytics/
    AnalyticsService.java      -- core analytics + ML sentiment
    AnalyticsController.java   -- REST API (hashtags, hours, summary, sentiment)
    SentimentModel.java        -- ML feature extractor + classifier
```

---

## **Iteration 2 Additions & Improvements**

---

### **1. Sentiment analysis (NEW)**

**Endpoint:**

```
GET /analytics/sentiment?accountId=...
```

We introduce a lightweight machine learning classifier implemented entirely in Java.
The model uses:

* TF-IDF–style keyword weighting
* Logistic-scored heuristic classification
* Three labels: **POSITIVE, NEGATIVE, NEUTRAL**

**Outputs:**

* count of positive, negative, neutral tweets
* average sentiment score
* total tweets scanned

**Example output:**

```
{
  "totalTweets": 12,
  "positive": 2,
  "negative": 2,
  "neutral": 8,
  "averageScore": -0.25
}
```

This fulfills the optional **“NLP/ML sentiment analysis”** improvement described in Iteration 1.

---

### **2. Real Mastodon-backed analytics**

Analytics no longer operates only on the internal fake timeline.
It now consumes **real Mastodon home timeline tweets**, including:

* posts created via our scheduler
* posts fetched from the Mastodon API
* fallback to DB (if enabled via `app.search.source=db`)

---

### **3. Improved Summary Report**

**Endpoint:**

```
GET /analytics/summary?accountId=...
```

Now returns more useful insights:

* total tweets analyzed
* top hashtags
* best hour (UTC)
* works directly with real Mastodon content

**Example:**

```
{
  "totalTweets": 12,
  "topHashtags": [],
  "bestHourUtc": 23
}
```

---

### **4. Best Posting Hours (enhanced)**

**Endpoint:**

```
GET /analytics/best-hours?accountId=...
```

Now supports:

* real timestamps from Mastodon
* correct UTC bucketing
* multiple-hour aggregation
* graceful fallback if API is unavailable

**Example:**

```
{
  "19": 2,
  "20": 1,
  "21": 2,
  "23": 6,
  "10": 1
}
```

---

### **5. Top Hashtags (unchanged, but real data)**

**Endpoint:**

```
GET /analytics/top-hashtags?accountId=...&n=5
```

Returns the most frequent hashtags across the fetched tweets.

If no hashtags exist, returns:

```
[]
```

---

## **Curl Commands for the Demo**

(These match the results we already obtained)

### **1. Sentiment Analysis**

```
curl -s "http://localhost:8080/analytics/sentiment?accountId=test-account" | jq .
```

### **2. Top Hashtags**

```
curl -s "http://localhost:8080/analytics/top-hashtags?accountId=test-account&n=5" | jq .
```

### **3. Best Hours**

```
curl -s "http://localhost:8080/analytics/best-hours?accountId=test-account" | jq .
```

### **4. Summary**

```
curl -s "http://localhost:8080/analytics/summary?accountId=test-account" | jq .
```

---

## **End-to-End Example (Our Real Output)**

### **Sentiment:**

```
{
  "totalTweets": 12,
  "positive": 2,
  "negative": 2,
  "neutral": 8,
  "averageScore": -0.25
}
```

### **Best Hours:**

```
{
  "19": 2,
  "20": 1,
  "21": 2,
  "23": 6,
  "10": 1
}
```

### **Summary:**

```
{
  "totalTweets": 12,
  "topHashtags": [],
  "bestHourUtc": 23
}
```

---

## **How it fits the Iteration-2 expectations**

From our Iteration-1 PDF:

- **“Add real analytics based on real posts” — done**
- **“Optional deeper statistics” — done**
- **“Add some NLP / ML sentiment model (optional)” — done**
- **“Works with Mastodon instead of fake Twitter” — done**
- **“Still small enough for the project scope” — done**

This is a complete, polished, fully functional **Iteration-2 analytics subsystem**.

---

## **Final Status**

- Real data ingestion
- Machine learning sentiment analysis
- Hashtags, summary, hours
- Configurable DB/timeline source
- Clean API and ready for demonstration

