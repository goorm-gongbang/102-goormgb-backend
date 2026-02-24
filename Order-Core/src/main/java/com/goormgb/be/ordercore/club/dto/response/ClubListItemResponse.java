package com.goormgb.be.ordercore.club.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ClubListItemResponse {

    private Long clubId;
    private String koName;
    private String enName;
    private String logoImg;
    private String clubColor;
}
