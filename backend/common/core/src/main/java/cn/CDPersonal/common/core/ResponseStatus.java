package cn.CDPersonal.common.core;

/**
 * 全局响应状态码枚举，配合 {@link ResponseVO} 组成统一返回体。
 *
 * <p>code 直接沿用 HTTP 语义：4xx 为调用方问题、5xx 为服务端问题，
 * 前端只按 code 判断成败，不依赖 HTTP status。新增错误类型时保持这个分段约定，
 * 否则前端的统一错误提示会分不清「该提示用户」还是「该报系统异常」。
 *
 * <p>SUCCESS(200) 被 {@link ResponseVO} 的成功构造方法直接引用；
 * 401/403 同时散落在鉴权、权限校验逻辑里，改数值前需全仓库确认。
 */
public enum ResponseStatus {
    SUCCESS(200, "成功"),
    BAD_REQUEST(400, "错误请求"),
    UNAUTHORIZED(401, "未授权"),
    NOT_FOUND(404, "未找到"),
    FORBIDDEN(403, "禁止访问"),
    NOT_ACCEPTABLE(406, "请求的资源不可接受"),
    REQUEST_TIMEOUT(408, "请求超时"),
    CONFLICT(409, "冲突"),
    GONE(410, "已删除"),
    UNSUPPORTED_MEDIA_TYPE(415, "不支持的媒体类型"),

    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),
    NOT_IMPLEMENTED(501, "尚未实现"),
    BAD_GATEWAY(502, "错误网关"),
    SERVICE_UNAVAILABLE(503, "服务不可用"),
    GATEWAY_TIMEOUT(504, "网关超时"),
    HTTP_VERSION_NOT_SUPPORTED(505, "HTTP版本不受支持");



    /** 响应码：与 HTTP 状态码数值一致，前端据此判定成败 */
    private final Integer code;

    /** 默认提示文案：成功响应与未细分错误场景直接用它填充 msg */
    private final String message;

    ResponseStatus(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }


    public String getMessage() {
        return message;
    }
}
