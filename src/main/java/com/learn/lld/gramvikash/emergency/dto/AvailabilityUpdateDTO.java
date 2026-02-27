package com.learn.lld.gramvikash.emergency.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityUpdateDTO {

    @NotNull(message = "ID is required")
    private UUID id;

    @NotNull(message = "Availability status is required")
    private Boolean available;
}
