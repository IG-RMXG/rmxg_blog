package cn.CDPersonal.common.core;

import java.io.Serial;
import java.io.Serializable;

import static cn.CDPersonal.common.core.ResponseStatus.SUCCESS;

/**
 * 统一响应体，三个服务的 Controller 都返回这个结构。
 *
 * <p>对外契约：{@code code} 业务状态码（见 {@link ResponseStatus}）、{@code msg} 提示文案、
 * {@code data} 业务数据、{@code timestamp} 服务端毫秒时间戳（便于对齐问题发生时刻）。
 * 前端 axios 拦截器按 {@code code} 分流（200 放行、401 跳登录、其它弹错），
 * 成功时把整个响应体交给调用方，业务数据仍需从 {@code data} 字段取。
 *
 * <p>{@code data} 为 null 时 JSON 里仍然出现该字段，前端取值前仍需判空。
 */
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

    // 该重载未使用传入的 msg：msg 仍取 SUCCESS 的默认文案，调用方传入的自定义提示不会生效
    public static <T> ResponseVO<T> success(String msg, T data) {
        return new ResponseVO<>(SUCCESS.getCode(), SUCCESS.getMessage(), data, System.currentTimeMillis());
    }

    public static <T> ResponseVO<T> success(T data) {
        return new ResponseVO<>(SUCCESS.getCode(), SUCCESS.getMessage(), data, System.currentTimeMillis());
    }

    /**
     * 构造失败响应：data 固定为 null，调用方只关心 code 与提示文案。
     */
    public static <T> ResponseVO<T> error(Integer code, String message) {
        return new ResponseVO<>(code, message, null, System.currentTimeMillis());
    }
}
