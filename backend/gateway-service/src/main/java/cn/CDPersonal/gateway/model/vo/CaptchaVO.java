package cn.CDPersonal.gateway.model.vo;

import java.io.Serial;
import java.io.Serializable;

/**
 * CaptchaVO 验证码返回对象
 * @author CDPersonal
 * @date 2025/10/23
 */
public class CaptchaVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    //验证码原图
    private String originalImageBase64;
    //验证码拼图
    private String jigsawImageBase64;

    private Integer jigsawY;
    //token
    private String token;


    public String getOriginalImageBase64() {
        return this.originalImageBase64;
    }

    public void setOriginalImageBase64(String originalImageBase64) {
        this.originalImageBase64 = originalImageBase64;
    }


    public String getJigsawImageBase64() {
        return this.jigsawImageBase64;
    }

    public void setJigsawImageBase64(String jigsawImageBase64) {
        this.jigsawImageBase64 = jigsawImageBase64;
    }

    public Integer getJigsawY() {
        return jigsawY;
    }

    public void setJigsawY(Integer jigsawY) {
        this.jigsawY = jigsawY;
    }

    public String getToken() {
        return this.token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public String toString() {
        return "CaptchaVO{" +
                "originalImageBase64='" + originalImageBase64 + '\'' +
                ", jigsawImageBase64='" + jigsawImageBase64 + '\'' +
                ", jigsawY=" + jigsawY +
                ", token='" + token + '\'' +
                '}';
    }
}
