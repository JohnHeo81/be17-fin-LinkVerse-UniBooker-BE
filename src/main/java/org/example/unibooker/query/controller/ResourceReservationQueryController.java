package org.example.unibooker.query.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.unibooker.common.BaseResponse;
import org.example.unibooker.query.model.dto.ResourceReservationDto;
import org.example.unibooker.query.service.ResourceReservationQueryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "ResourceReservationQuery API", description = "리소스와 예약 관련 복합 조회 API")
@RequestMapping("/api/resource-reservation")
public class ResourceReservationQueryController {

    private final ResourceReservationQueryService resourceReservationQueryService;

    /** 하루(시간 슬롯) 단위 */
    @Operation(summary = "일별 예약 현황 조회", description = "특정 날짜의 시간대별 예약 현황을 조회합니다.")
    @GetMapping("count/day")
    public BaseResponse<List<ResourceReservationDto.ResourceReservationCountRes>> day(
            @RequestParam Long groupId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer slotMinutes // default 60
    ) {
        List<ResourceReservationDto.ResourceReservationCountRes> data =
                resourceReservationQueryService.getDayHourlyCounts(groupId, date, slotMinutes);
        return BaseResponse.success(data);
    }

    /** 주(시간 슬롯) 단위 */
    @Operation(summary = "주간 예약 현황 조회", description = "특정 주의 시간대별 예약 현황을 조회합니다.")
    @GetMapping("count/week")
    public BaseResponse<List<ResourceReservationDto.ResourceReservationCountRes>> week(
            @RequestParam Long groupId,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) Integer slotMinutes // default 60
    ) {
        List<ResourceReservationDto.ResourceReservationCountRes> data =
                resourceReservationQueryService.getWeekHourlyCounts(groupId, startDate, slotMinutes);
        return BaseResponse.success(data);
    }

    /** 월(하루 슬롯) 단위 */
    @Operation(summary = "월간 예약 현황 조회", description = "특정 월의 일별 예약 현황을 조회합니다.")
    @GetMapping("count/month")
    public BaseResponse<List<ResourceReservationDto.ResourceReservationCountRes>> month(
            @RequestParam Long groupId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        List<ResourceReservationDto.ResourceReservationCountRes> data =
                resourceReservationQueryService.getMonthDailyCounts(groupId, year, month);
        return BaseResponse.success(data);
    }
}
