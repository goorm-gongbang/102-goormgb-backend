package com.goormgb.be.ordercore.club.controller;

import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.ordercore.club.dto.response.ClubGetResponse;
import com.goormgb.be.ordercore.club.service.ClubService;
import com.goormgb.be.ordercore.support.WebMvcTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static com.goormgb.be.ordercore.fixture.club.ClubControllerFixture.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ClubController.class)
@AutoConfigureMockMvc(addFilters = false)
class ClubControllerTest extends WebMvcTestSupport {

    @MockitoBean
    private ClubService clubService;

    @Test
    @DisplayName("GET /clubs - 구단 전체 조회 성공")
    void 구단_전체_조회_성공() throws  Exception {

        // given
        ClubGetResponse response = clubGetResponse();
        given(clubService.getAllClubs()).willReturn(response);

        // when & then
        mockMvc.perform(get("/clubs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.message").value("성공"))
                .andExpect(jsonPath("$.data.clubs.length()").value(3))
                .andExpect(jsonPath("$.data.clubs[0].clubId").value(1L))
                .andExpect(jsonPath("$.data.clubs[0].koName").value("구름 FC"));
    }

    @Test
    @DisplayName("GET /clubs - 클럽이 존재하지 않으면 CLUB_NOT_FOUND 에러 반환")
    void 구단_전체_조회_실패_클럽없음() throws Exception {
        // given
        given(clubService.getAllClubs())
                .willThrow(new CustomException(ErrorCode.CLUB_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/clubs"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR"))
                .andExpect(jsonPath("$.message").value("구단을 찾을 수 없습니다."));
    }
}