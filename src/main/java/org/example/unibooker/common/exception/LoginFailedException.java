package org.example.unibooker.common.exception;

import lombok.Getter;
import org.example.unibooker.common.BaseResponseStatus;

/**
 * 로그인 실패 예외
 * - 남은 시도 횟수 정보 포함
 */
@Getter
public class LoginFailedException extends RuntimeException {

    private final BaseResponseStatus status;
    private final int remainingAttempts;

    public LoginFailedException(BaseResponseStatus status, int remainingAttempts) {
        super(status.getMessage());
        this.status = status;
        this.remainingAttempts = remainingAttempts;
    }

    public int getCode() {
        return status.getCode();
    }
}