package top.xiyamani.week04.exception;

/**
 * @author xiyamani
 * @date 2026/3/26
 * @description 自定义业务异常类
 */

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final int code;
    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
