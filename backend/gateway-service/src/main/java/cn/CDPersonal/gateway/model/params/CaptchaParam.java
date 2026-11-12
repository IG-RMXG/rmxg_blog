package cn.CDPersonal.gateway.model.params;

/**
 * 滑动验证码校验入参（POST /captcha 的请求体）。
 *
 * <p>两个字段都由前端回传：服务端只用 token 查 Redis 里的答案、用 x 做容差比对，
 * 不信任请求体里的其它任何信息；token 缺失或过期都按校验失败处理。
 */
public class CaptchaParam {

    /** 用户拖动后拼图块的水平坐标（像素） */
    private Integer x;

    /** 获取验证码时下发的 token，即 Redis 中答案的 key */
    private String token;

    public Integer getX() {
        return x;
    }

    public void setX(Integer x) {
        this.x = x;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public String toString() {
        return "CaptchaParam{" +
                "x=" + x +
                ", token='" + token + '\'' +
                '}';
    }
}
