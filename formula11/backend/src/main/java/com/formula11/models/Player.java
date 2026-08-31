package com.formula11.models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "players")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String league;

    @Column(nullable = false)
    private String team;

    @Column(nullable = false)
    private String position;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal baseValue;
}
