package cn.CDPersonal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * UAT 验收服务启动类（8020）。
 *
 * 只在 UAT 环境部署（scripts/uat-up.ps1）；DEV 环境不启动本服务，
 * 网关也不路由 /uat/**（gateway.forward.uat-service 默认留空）。
 */
@SpringBootApplication
public class UatApplication {

    public static void main(String[] args) {
        SpringApplication.run(UatApplication.class, args);
        System.out.println("╰(*°▽°*)╯  UAT服务启动成功   ψ(｀∇´)ψ  \n");
    }
}
