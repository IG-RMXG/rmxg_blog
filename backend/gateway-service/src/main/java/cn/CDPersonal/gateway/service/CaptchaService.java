package cn.CDPersonal.gateway.service;

import cn.CDPersonal.gateway.model.params.CaptchaParam;
import cn.CDPersonal.gateway.model.vo.CaptchaVO;

import java.io.IOException;

/**
 * 验证码抽象类，定义了验证码的抽象方法
 *
 * <p>当前唯一实现是 {@link cn.CDPersonal.gateway.service.impl.BlockPuzzleCaptchaServiceImpl}（拼图滑块）。
 * 两个方法的配套契约：getCaptcha 返回的 token 即服务端保存答案的凭据，checkCaptcha 必须凭同一个 token 校验；
 * token 有 TTL，过期后校验一律返回 false。
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
