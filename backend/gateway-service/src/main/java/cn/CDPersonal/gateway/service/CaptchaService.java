package cn.CDPersonal.gateway.service;

import cn.CDPersonal.gateway.model.params.CaptchaParam;
import cn.CDPersonal.gateway.model.vo.CaptchaVO;

import java.io.IOException;

/**
 * 验证码抽象类，定义了验证码的抽象方法
 */
public interface CaptchaService {

    /**
     * 生成验证码,类型为拼图方式
     * @return 验证码
     */
    CaptchaVO getCaptcha() throws IOException;

    /**
     * 校验验证码
     * @param captchaParam 验证码参数
     * @return 校验结果
     */
    boolean checkCaptcha(CaptchaParam captchaParam);
}
