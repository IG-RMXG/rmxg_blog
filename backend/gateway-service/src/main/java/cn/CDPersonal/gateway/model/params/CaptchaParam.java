package cn.CDPersonal.gateway.model.params;

public class CaptchaParam {

    private Integer x;

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
