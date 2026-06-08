package com.doccase.common.exception;

import com.doccase.common.enums.ResponseCode;
import lombok.Getter;

@Getter
public class AuthException extends BizException {

    public AuthException(ResponseCode responseCode) {
        super(responseCode);
    }

    public AuthException(ResponseCode responseCode, String detail) {
        super(responseCode, detail);
    }

    public AuthException(int code, String message) {
        super(code, message);
    }
}
