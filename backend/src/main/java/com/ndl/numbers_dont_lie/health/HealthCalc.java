package com.ndl.numbers_dont_lie.health;

public class HealthCalc {

  public static class BmiResult {
    public final double bmi;
    public final String classification;
    public BmiResult(double bmi, String classification) { this.bmi = bmi; this.classification = classification; }
  }

  public static BmiResult bmi(double weightKg, int heightCm) {
    double h = heightCm / 100.0;
    double v = weightKg / (h * h);
    String cls = (v < 18.5) ? "underweight" :
                 (v < 25.0) ? "normal" :
                 (v < 30.0) ? "overweight" : "obese";
    return new BmiResult(round1(v), cls);
  }

  public static double bmiScoreComponent(String classification) {
    // 0..100 — лучше в центре
    return switch (classification) {
      case "normal" -> 100;
      case "underweight", "overweight" -> 60;
      default -> 30; // obese
    };
  }

  public static double activityScore(String level) {
    if (level == null) return 50;
    return switch (level) {
      case "low" -> 40;
      case "moderate" -> 75;
      case "high" -> 95;
      default -> 50;
    };
  }

  public static double progressScore(double latestWeightKg, Double targetWeightKg) {
    if (targetWeightKg == null) return 50;
    // чем ближе к цели — тем выше; простая эвристика
    double diff = Math.abs(latestWeightKg - targetWeightKg);
    if (diff < 1) return 95;
    if (diff < 3) return 80;
    if (diff < 7) return 65;
    return 50;
  }

  public static double habitsScore(String activityLevel) {
    // пока используем активность как прокси привычек
    return activityScore(activityLevel);
  }

  public static double weeklyActivityBooster(int totalMinutes, int daysActive) {
    // WHO ориентир: ≥150 мин/нед умеренной активности
    if (totalMinutes >= 300 && daysActive >= 5) return 95; // отличная неделя
    if (totalMinutes >= 150 && daysActive >= 3) return 85; // норма
    if (totalMinutes >= 60  && daysActive >= 2) return 70; // неплохо
    return 50; // слабовато
  }

  public static double wellness(double bmiScore, double activity, double progress, double habits) {
    return round1(bmiScore * 0.3 + activity * 0.3 + progress * 0.2 + habits * 0.2);
  }

  private static double round1(double v) { return Math.round(v * 10.0) / 10.0; }
}
