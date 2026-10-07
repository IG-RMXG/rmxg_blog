package cn.CDPersonal.common.core;

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



    private final Integer code;

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
