package com.goormgb.be.ordercore.onboarding.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PreferredBlockUpdateRequest(
	@NotNull @Size(min = 1, max = 10)
	List<Long> preferredBlockIds
) {
}
