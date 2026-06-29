package com.vencentdev.backend.match.enums;

public enum PieceType {
  FIVE_STAR_GENERAL("Five-Star General", "5G", 14, 1),
  FOUR_STAR_GENERAL("Four-Star General", "4G", 13, 1),
  THREE_STAR_GENERAL("Three-Star General", "3G", 12, 1),
  TWO_STAR_GENERAL("Two-Star General", "2G", 11, 1),
  ONE_STAR_GENERAL("One-Star General", "1G", 10, 1),
  COLONEL("Colonel", "Col", 9, 1),
  LT_COLONEL("Lt. Colonel", "LC", 8, 1),
  MAJOR("Major", "Maj", 7, 1),
  CAPTAIN("Captain", "Capt", 6, 1),
  FIRST_LIEUTENANT("First Lieutenant", "1Lt", 5, 1),
  SECOND_LIEUTENANT("Second Lieutenant", "2Lt", 4, 1),
  SERGEANT("Sergeant", "Sgt", 3, 1),
  SPY("Spy", "Spy", 2, 2),
  PRIVATE("Private", "Pvt", 1, 6),
  FLAG("Flag", "F", 0, 1);

  private final String label;
  private final String abbreviation;
  private final int rank;
  private final int count;

  PieceType(String label, String abbreviation, int rank, int count) {
    this.label = label;
    this.abbreviation = abbreviation;
    this.rank = rank;
    this.count = count;
  }

  public String label() {
    return label;
  }

  public String abbreviation() {
    return abbreviation;
  }

  public int rank() {
    return rank;
  }

  public int count() {
    return count;
  }
}
