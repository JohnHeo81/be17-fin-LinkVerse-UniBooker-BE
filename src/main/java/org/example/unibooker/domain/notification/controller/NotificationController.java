package org.example.unibooker.domain.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.unibooker.common.BaseResponse;
import org.example.unibooker.domain.notification.model.dto.NotificationDto;
import org.example.unibooker.domain.notification.service.NotificationService;
import org.example.unibooker.domain.user.model.dto.AuthDto;
import org.springframework.data.domain.Page;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 알림 API 컨트롤러
 * - ERD notifications 테이블 기반
 * - 모든 권한(SUPER, ADMIN, USER)이 본인의 알림만 조회/관리 가능
 */
@Tag(name = "Notification API", description = "알림 관리 API")
@RestController
@RequestMapping("/api/notify")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;


    // -------------------- 알림 목록 조회 --------------------
    @Operation(summary = "알림 목록 조회", description = "현재 로그인한 사용자의 알림 목록을 페이징하여 조회합니다.")
    @GetMapping
    public BaseResponse<Page<NotificationDto.NotificationRes>> getNotifications(@AuthenticationPrincipal AuthDto.AuthenticatedUser authUser,
                                                                                @RequestParam(defaultValue = "0") int page,
                                                                                @RequestParam(defaultValue = "10") int size) {

        Page<NotificationDto.NotificationRes> response = notificationService.getUserNotifications(authUser.getId(), page, size);
        return BaseResponse.success(response);
    }


    // -------------------- 알림 읽음 처리 --------------------
    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 상태로 변경합니다.")
    @GetMapping("/read/{id}")
    public BaseResponse markAsRead(@AuthenticationPrincipal AuthDto.AuthenticatedUser authUser,
                                   @PathVariable("id") Long notificationId) {

        notificationService.markAsRead(notificationId, authUser.getId());
        return BaseResponse.success("알림 읽음 처리 완료.");
    }
}