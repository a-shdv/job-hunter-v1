package com.company.aggregator.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table
@Getter
@Setter
@ToString(exclude = "user")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Statistics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    String avgSalaryTitle;
    String avgSalaryDescription;
    String medianSalaryTitle;
    String medianSalaryDescription;
    String modalSalaryTitle;
    String modalSalaryDescription;
    String profession;
    String city;
    String year;
    String currency;
    @OneToOne(mappedBy = "statistics") private User user;
}
