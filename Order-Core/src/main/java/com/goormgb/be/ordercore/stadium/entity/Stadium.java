package com.goormgb.be.ordercore.stadium.entity;

import com.goormgb.be.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stadiums")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stadium extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String region;

    @Column(name = "ko_name", nullable = false, length = 100)
    private String koName;

    @Column(name = "en_name", nullable = false, length = 100)
    private String enName;

    @Column(length = 255)
    private String address;
}
