package com.goormgb.be.global.request;

import org.springframework.data.domain.PageRequest;

public record Paging(
		// TODO: 페이징 정렬기능 나중에 추가해도 될듯? 작업자:seulgi
		Integer page,
		Integer size
) {
	public Paging {
		if (page == null || page < 1) {
			page = 1;
		}
		if (size == null || size < 1) {
			size = 10;
		}
	}

	public PageRequest toPageable() {
		return PageRequest.of(page - 1, size);
	}
}
