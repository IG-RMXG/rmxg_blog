package cn.CDPersonal.gateway.model.dto;


/**
 * CaptchaDTO 验证码传输对象
 *
 * <p>只在服务内部流转：由图片生成逻辑填充，随后拆成「下发给前端的图片 + 写入 Redis 的答案坐标」，
 * 因此这里同时带着两张图的 Base64 与答案 x/y，不直接序列化给客户端。
 * @author CDPersonal
 * @date 2025/10/23
 */
public class CaptchaDTO {

    //验证码原图
    private String originalImageBase64;
    //验证码拼图
    private String jigsawImageBase64;
    //拼图块x坐标
    private int x;
    //拼图块y坐标
    private int y;

    public String getOriginalImageBase64() {
        return originalImageBase64;
    }

    public void setOriginalImageBase64(String originalImageBase64) {
        this.originalImageBase64 = originalImageBase64;
    }

    public String getJigsawImageBase64() {
        return jigsawImageBase64;
    }

    public void setJigsawImageBase64(String jigsawImageBase64) {
        this.jigsawImageBase64 = jigsawImageBase64;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    @Override
    public String toString() {
        return "CaptchaDTO{" +
                "originalImageBase64='" + originalImageBase64 + '\'' +
                ", jigsawImageBase64='" + jigsawImageBase64 + '\'' +
                ", x=" + x +
                ", y=" + y +
                '}';
    }
}
