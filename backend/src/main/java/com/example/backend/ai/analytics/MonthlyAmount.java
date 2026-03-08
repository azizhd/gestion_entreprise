package com.example.backend.ai.analytics;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthlyAmount(YearMonth month, BigDecimal amount) { }
