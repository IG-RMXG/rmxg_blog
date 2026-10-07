package cn.CDPersonal.gateway.model.cache;

import java.io.Serial;
import java.io.Serializable;

/** 用于redis缓存验证码点
 * @author CDPersonal
 * @date 2021/1/26
 */
public class CaptchaPointCache implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    // x,y
    public int x;
    public int y;

    public CaptchaPointCache() {
    }

    public CaptchaPointCache(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return this.x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return this.y;
    }

    public void setY(int y) {
        this.y = y;
    }

    @Override
    public String toString() {
        return "CaptchaPointCache{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
}
