package com.goormgb.be.ordercore.club.entity;


import com.goormgb.be.global.entity.BaseEntity;
import com.goormgb.be.ordercore.stadium.entity.Stadium;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "clubs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Club extends BaseEntity {

    @Column(name = "ko_name", nullable = false, length = 100)
    private String koName;

    @Column(name = "en_name", nullable = false, length = 100)
    private String enName;

    @Column(name = "logo_img", length = 255)
    private String logoImg;

    @Column(name = "club_color", length = 20)
    private String clubColor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stadium_id", nullable = false)
    private Stadium stadium;

    @Column(name = "homepage_redirect_url", length = 255)
    private String homepageRedirectUrl;
}
