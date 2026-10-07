package cn.CDPersonal.common.core;

import java.io.Serial;
import java.io.Serializable;

import static cn.CDPersonal.common.core.ResponseStatus.SUCCESS;

public class ResponseVO<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 状态码
    private Integer code;

    // 提示信息
    private String msg;

    // 数据
    private T data;

    // 时间戳
    private Long timestamp;

    public ResponseVO() {
    }

    public ResponseVO(Integer code, String msg, T data, Long timestamp) {
        this.code = code;
        this.msg = msg;
        this.data = data;
        this.timestamp = timestamp;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public static <T> ResponseVO<T> success() {
        return new ResponseVO<>(SUCCESS.getCode(), SUCCESS.getMessage(), null, System.currentTimeMillis());
    }

    public static <T> ResponseVO<T> success(String msg, T data) {
        return new ResponseVO<>(SUCCESS.getCode(), SUCCESS.getMessage(), data, System.currentTimeMillis());
    }

    public static <T> ResponseVO<T> success(T data) {
        return new ResponseVO<>(SUCCESS.getCode(), SUCCESS.getMessage(), data, System.currentTimeMillis());
    }

    public static <T> ResponseVO<T> error(Integer code, String message) {
        return new ResponseVO<>(code, message, null, System.currentTimeMillis());
    }
}
