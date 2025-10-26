package com.ndl.numbers_dont_lie.entity;

import com.ndl.numbers_dont_lie.entity.UserEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "profiles", uniqueConstraints = {
  @UniqueConstraint(name = "uk_profiles_user", columnNames = "user_id")
})
public class ProfileEntity {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(optional = false)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private UserEntity user;

  private Integer age;              // 5..120
  @Column(length = 16)  private String gender;         // male|female|other
  private Integer heightCm;         // 50..300
  private Double  weightKg;         // 20..500
  private Double  targetWeightKg;   // цель по весу (кг)
  @Column(length = 24)  private String activityLevel;  // low|moderate|high
  @Column(length = 32)  private String goal;           // weight_loss|muscle_gain|general_fitness

  public Long getId() { return id; }
  public UserEntity getUser() { return user; }
  public void setUser(UserEntity user) { this.user = user; }
  public Integer getAge() { return age; }
  public void setAge(Integer age) { this.age = age; }
  public String getGender() { return gender; }
  public void setGender(String gender) { this.gender = gender; }
  public Integer getHeightCm() { return heightCm; }
  public void setHeightCm(Integer heightCm) { this.heightCm = heightCm; }
  public Double getWeightKg() { return weightKg; }
  public void setWeightKg(Double weightKg) { this.weightKg = weightKg; }
  public Double getTargetWeightKg() { return targetWeightKg; }
  public void setTargetWeightKg(Double targetWeightKg) { this.targetWeightKg = targetWeightKg; }
  public String getActivityLevel() { return activityLevel; }
  public void setActivityLevel(String activityLevel) { this.activityLevel = activityLevel; }
  public String getGoal() { return goal; }
  public void setGoal(String goal) { this.goal = goal; }
}
