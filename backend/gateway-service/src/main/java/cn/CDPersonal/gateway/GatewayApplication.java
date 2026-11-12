package cn.CDPersonal.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * 网关启动程序
 *
 * <p>网关只做请求转发与验证码，不需要数据源，所以显式排除 DataSourceAutoConfiguration。
 *
 * <p>端口由 application.yml 的 {@code ${GATEWAY_PORT:8889}} 决定，前端 vite 代理的目标就是这里。
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class })
public class GatewayApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(GatewayApplication.class, args);
        System.out.println("╰(*°▽°*)╯  网关启动成功   ψ(｀∇´)ψ  \n");
    }
}
