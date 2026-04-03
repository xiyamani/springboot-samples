package top.xiyamani.week04.handler;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import top.xiyamani.week04.common.Result;
import top.xiyamani.week04.exception.BusinessException;

import java.util.StringJoiner;

/**
 * @author xiyamani
 * @date 2026/3/26
 * @description 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理参数校验异常
     *
     * @param e 参数校验异常
     * @return 错误信息
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValException(MethodArgumentNotValidException e) {
        StringJoiner sj = new StringJoiner(", ");
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            sj.add(fieldError.getDefaultMessage());
        }
        return Result.error(400, sj.toString());
    }

    /**
     * 处理业务异常
     *
     * @param e 业务异常
     * @return 错误信息
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理其他异常
     * @param e 业务异常
     * @return 错误信息
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        return Result.error(500, "服务器异常，请稍后重试");
    }
}
