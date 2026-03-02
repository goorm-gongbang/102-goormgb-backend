package com.goormgb.be.ordercore.club.controller;

import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.ordercore.club.dto.response.ClubDetailGetResponse;
import com.goormgb.be.ordercore.club.dto.response.ClubGetResponse;
import com.goormgb.be.ordercore.club.service.ClubService;
import com.goormgb.be.ordercore.match.dto.response.ClubMonthlyMatchesResponse;
import com.goormgb.be.ordercore.support.WebMvcTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static com.goormgb.be.ordercore.fixture.club.ClubControllerFixture.*;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ClubController.class)
@AutoConfigureMockMvc(addFilters = false)
class ClubControllerTest extends WebMvcTestSupport {

    @MockitoBean
    private ClubService clubService;

    @Test
    @DisplayName("GET /clubs - 구단 전체 조회 성공")
    void 구단_전체_조회_성공() throws Exception {

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
    @DisplayName("GET /clubs - 구단이 존재하지 않으면 에러 반환")
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

    @Test
    @DisplayName("GET /clubs/{clubId} - 구단 상세 조회 성공")
    void 구단_상세_조회_성공() throws Exception {
        // given
        Long clubId = clubId();
        ClubDetailGetResponse response = clubDetailGetResponse(clubId);
        given(clubService.getClubDetail(clubId)).willReturn(response);

        // when & then
        mockMvc.perform(get("/clubs/{clubId}", clubId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.clubId").value(clubId))
                .andExpect(jsonPath("$.data.koName").value("구름 FC"))
                .andExpect(jsonPath("$.data.stadium.stadiumId").value(10L))
                .andExpect(jsonPath("$.data.currentSeasonStats.seasonYear").value(2026))
                .andExpect(jsonPath("$.data.currentSeasonStats.winRate").value(0.720));
    }

    @Test
    @DisplayName("GET /clubs/{clubId} - 시즌 정보가 없는 경우")
    void 구단_상세_조회_성공_시즌정보없음() throws Exception {
        // given
        Long clubId = clubId();
        ClubDetailGetResponse response = clubDetailGetResponseWithoutStats(clubId);
        given(clubService.getClubDetail(clubId)).willReturn(response);

        // when & then
        mockMvc.perform(get("/clubs/{clubId}", clubId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.clubId").value(clubId))
                .andExpect(jsonPath("$.data.koName").value("구름 FC"))
                .andExpect(jsonPath("$.data.stadium.stadiumId").value(10L))
                .andExpect(jsonPath("$.data.currentSeasonStats").value(nullValue()));
    }

    @Test
    @DisplayName("GET /clubs/{clubId} - 구단이 존재하지 않으면 에러 반환")
    void 구단_상세_조회_실패_클럽없음() throws Exception {
        // given
        Long clubId = clubId();
        given(clubService.getClubDetail(clubId))
                .willThrow(new CustomException(ErrorCode.CLUB_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/clubs/{clubId}", clubId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR"))
                .andExpect(jsonPath("$.message").value("구단을 찾을 수 없습니다."));
    }


    @Test
    @DisplayName("GET /clubs/{clubId}/matches - 구단 경기 일정(월) 조회 성공")
    void 구단_월별_경기일정_조회_성공() throws Exception {
        // given
        Long clubId = clubId();
        int year = year();
        int month = month();

        ClubMonthlyMatchesResponse response = clubMonthlyMatchesResponse(clubId, year, month);
        given(clubService.getClubMonthlyMatches(clubId, year, month)).willReturn(response);

        // when & then
        mockMvc.perform(get("/clubs/{clubId}/matches", clubId)
                        .queryParam("year", String.valueOf(year))
                        .queryParam("month", String.valueOf(month)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.message").value("성공"))

                // ====== 상위 메타 정보 ======
                .andExpect(jsonPath("$.data.clubId").value(clubId))
                .andExpect(jsonPath("$.data.year").value(year))
                .andExpect(jsonPath("$.data.month").value(month))
                .andExpect(jsonPath("$.data.totalMatchCount").value(2))

                // ====== 경기 리스트 ======
                .andExpect(jsonPath("$.data.matches").isArray())
                .andExpect(jsonPath("$.data.matches.length()").value(2))

                // ====== 첫 번째 경기 ======
                .andExpect(jsonPath("$.data.matches[0].matchId").value(1001L))
                .andExpect(jsonPath("$.data.matches[0].isHomeMatch").value(true))
                .andExpect(jsonPath("$.data.matches[0].saleStatus").value("ON_SALE"))
                .andExpect(jsonPath("$.data.matches[0].opponentClub.koName")
                        .value("테크업 유나이티드"))

                // ====== 두 번째 경기 ======
                .andExpect(jsonPath("$.data.matches[1].matchId").value(1002L))
                .andExpect(jsonPath("$.data.matches[1].isHomeMatch").value(false))
                .andExpect(jsonPath("$.data.matches[1].saleStatus").value("SOLD_OUT"))
                .andExpect(jsonPath("$.data.matches[1].opponentClub.koName")
                        .value("코딩 시티"));

        // service 호출 파라미터까지 확실히 고정
        then(clubService).should(times(1)).getClubMonthlyMatches(clubId, year, month);
    }

    @Test
    @DisplayName("GET /clubs/{clubId}/matches - year가 숫자가 아니면 400")
    void 월별조회_실패_year형식오류_400() throws Exception {
        mockMvc.perform(get("/clubs/{clubId}/matches", clubId())
                        .queryParam("year", "two-thousand")
                        .queryParam("month", "3"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /clubs/{clubId}/matches - month가 숫자가 아니면 400")
    void 월별조회_실패_month형식오류_400() throws Exception {
        mockMvc.perform(get("/clubs/{clubId}/matches", clubId())
                        .queryParam("year", "2026")
                        .queryParam("month", "march"))
                .andExpect(status().isBadRequest());
    }
}