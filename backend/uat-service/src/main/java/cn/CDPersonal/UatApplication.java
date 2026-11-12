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

    /**
     * 启动入口：只做 Spring Boot 引导，不在这里注册额外组件。
     *
     * 组件扫描根是 cn.CDPersonal，MyBatis starter 会据此自动发现 @Mapper 接口（本模块没有 @MapperScan）；
     * 数据源 / Redis 参数全部来自 application.yml 与环境变量。末尾那行输出只是给人眼看的启动标记，
     * 不参与健康检查，改它不会影响脚本判断。
     */
    public static void main(String[] args) {
        SpringApplication.run(UatApplication.class, args);
        System.out.println("╰(*°▽°*)╯  UAT服务启动成功   ψ(｀∇´)ψ  \n");
    }
}
