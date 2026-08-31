package com.formula11.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerResponse {
    private Long id;
    private String name;
    private String league;
    private String team;
    private String position;
    private BigDecimal baseValue;
}
