package com.example.backend.dto;

import com.example.backend.entitie.enumuration.StatusDepense;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepenseDecisionRequest {
    @NotNull
    private StatusDepense decisionStatus;

    private String rejectionReason;

    @AssertTrue(message = "Decision must be APPROVED or DECLINED")
    public boolean isDecisionValid() {
        return decisionStatus != null && decisionStatus != StatusDepense.PENDING;
    }
}
