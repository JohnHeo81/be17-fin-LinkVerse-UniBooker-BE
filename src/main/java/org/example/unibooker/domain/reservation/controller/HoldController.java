package org.example.unibooker.domain.reservation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.unibooker.common.BaseResponse;
import org.example.unibooker.domain.reservation.model.dto.HoldDto;
import org.example.unibooker.domain.reservation.service.HoldService;
import org.example.unibooker.domain.user.model.dto.AuthDto;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Hold(임시 점유) 컨트롤러
 * - 시간/좌석 선택 시 임시 점유 관리
 */
@Tag(name = "Hold API", description = "시간/좌석 임시 점유 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/holds")
public class HoldController {

    private final HoldService holdService;

    /**
     * Hold 생성
     */
    @Operation(summary = "Hold 생성", description = "시간/좌석 선택 시 2분간 임시 점유합니다.")
    @PostMapping("/{resourceId}")
    public ResponseEntity<BaseResponse<HoldDto.Response>> createHold(
            @PathVariable Long resourceId,
            @RequestBody HoldDto.Request request,
            @AuthenticationPrincipal AuthDto.AuthUser authUser) {

        HoldDto.Response response = holdService.createHold(resourceId, authUser.getId(), request);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Hold 해제
     */
    @Operation(summary = "Hold 해제", description = "선택을 취소하고 임시 점유를 해제합니다.")
    @DeleteMapping("/{resourceId}")
    public ResponseEntity<BaseResponse<String>> releaseHold(
            @PathVariable Long resourceId,
            @RequestBody HoldDto.Request request,
            @AuthenticationPrincipal AuthDto.AuthUser authUser) {

        holdService.releaseHold(resourceId, authUser.getId(), request);
        return ResponseEntity.ok(BaseResponse.success("선택이 취소되었습니다."));
    }

    /**
     * Hold 상태 조회
     */
    @Operation(summary = "Hold 상태 조회", description = "특정 리소스의 현재 Hold 목록을 조회합니다.")
    @GetMapping("/{resourceId}/status")
    public ResponseEntity<BaseResponse<HoldDto.StatusResponse>> getHoldStatus(
            @PathVariable Long resourceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        HoldDto.StatusResponse response = holdService.getHoldStatus(resourceId, date);
        return ResponseEntity.ok(BaseResponse.success(response));
    }
}