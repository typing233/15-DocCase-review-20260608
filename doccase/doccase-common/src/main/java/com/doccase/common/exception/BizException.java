package com.doccase.common.exception;

import com.doccase.common.enums.ResponseCode;
import lombok.Getter;

@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(String message) {
        super(message);
        this.code = 400;
    }

    public BizException(ResponseCode responseCode) {
        super(responseCode.getMessage());
        this.code = responseCode.getCode();
    }

    public BizException(ResponseCode responseCode, String detail) {
        super(responseCode.getMessage() + ": " + detail);
        this.code = responseCode.getCode();
    }
}
