package com.example.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "ad_report")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdReport {

    @Id
    @Column(name = "ad_id")
    private String adId;

    @Column(name = "ad_name")
    private String adName;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "ad_goal")
    private String adGoal;

    @Column(name = "performance_goal")
    private String performanceGoal;

    @Column(name = "result_rate")
    private double resultRate;

    @Column(name = "post_change_rate")
    private double postChangeRate;
}