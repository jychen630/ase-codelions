package com.team.mcp.search;

/**
 * Optional filter parameters for search operations.
 */
public final class SearchFilters {

  /** Start date filter (ISO-8601) or null. */
  private final String fromDate;

  /** End date filter (ISO-8601) or null. */
  private final String toDate;

  /** Author username filter or null. */
  private final String author;

  /** Media filter (true = only with media) or null. */
  private final Boolean hasMedia;

  /**
   * Creates filter container.
   *
   * @param fromDateVal start date (ISO-8601) or null
   * @param toDateVal end date (ISO-8601) or null
   * @param authorVal author username or null
   * @param hasMediaVal media filter or null
   */
  public SearchFilters(
      final String fromDateVal,
      final String toDateVal,
      final String authorVal,
      final Boolean hasMediaVal) {
    this.fromDate = fromDateVal;
    this.toDate = toDateVal;
    this.author = authorVal;
    this.hasMedia = hasMediaVal;
  }

  /**
   * @return start date filter or null
   */
  public String fromDate() {
    return fromDate;
  }

  /**
   * @return end date filter or null
   */
  public String toDate() {
    return toDate;
  }

  /**
   * @return author filter or null
   */
  public String author() {
    return author;
  }

  /**
   * @return media filter or null
   */
  public Boolean hasMedia() {
    return hasMedia;
  }
}

