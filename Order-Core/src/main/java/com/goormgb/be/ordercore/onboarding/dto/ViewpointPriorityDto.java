package com.goormgb.be.ordercore.onboarding.dto;

import com.goormgb.be.domain.onboarding.entity.OnboardingViewpointPriority;
import com.goormgb.be.domain.onboarding.enums.Viewpoint;

public record ViewpointPriorityDto(
	Integer priority,
	Viewpoint viewpoint
) {
	public static ViewpointPriorityDto from(OnboardingViewpointPriority entity) {
		return new ViewpointPriorityDto(
			entity.getPriority(),
			entity.getViewpoint()
		);
	}
}
