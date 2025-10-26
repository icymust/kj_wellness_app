package com.ndl.numbers_dont_lie.health;

public class GoalProgress {
  public static class Result {
    public final Double percent;     // 0..100
    public final double coveredKg;   // пройдено
    public final double remainingKg; // осталось до цели
    public final int milestones5;
    public Result(Double percent, double coveredKg, double remainingKg, int milestones5) {
      this.percent = percent; this.coveredKg = coveredKg; this.remainingKg = remainingKg; this.milestones5 = milestones5;
    }
  }

  private static double round1(double v) {
    return Math.round(v * 10.0) / 10.0;
  }

  public static Result progress(Double initialKg, Double targetKg, Double currentKg) {
    if (initialKg == null || targetKg == null || currentKg == null)
      return new Result(null, Double.NaN, Double.NaN, 0);

    double total = Math.abs(initialKg - targetKg);
    double covered = Math.abs(initialKg - currentKg);
    double percent = (total == 0 ? 100.0 : Math.min(covered / total * 100.0, 100.0));
    double remaining = Math.abs(currentKg - targetKg);
    int milestones = (int) Math.floor(percent / 5.0);
    return new Result(round1(percent), round1(covered), round1(remaining), milestones);
  }
}
