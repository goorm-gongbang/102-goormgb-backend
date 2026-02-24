package com.goormgb.be.ordercore.club.dto.response;

import com.goormgb.be.ordercore.club.entity.Club;

public record ClubGetResponse(
        Long clubId,
        String koName,
        String enName,
        String logoImg,
        String clubColor
) {
    public static ClubGetResponse from(Club club) {
        return new ClubGetResponse(
               club.getId(),
               club.getKoName(),
               club.getEnName(),
               club.getLogoImg(),
               club.getClubColor()
        );
    }
}