package cn.CDPersonal.gateway.model.cache;

import java.io.Serial;
import java.io.Serializable;

/** 用于redis缓存验证码点
 *
 * <p>缓存的是「正确答案」：拼图块在原图中的左上角坐标（像素），只用于滑动校验，
 * 不会随验证码图片一起下发给前端。TTL 由生成方设置（当前 5 分钟）。
 *
 * <p>字段为 public 且保留无参构造，是为了让 JSON 反序列化能重建对象，不要改成不可变类。
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
