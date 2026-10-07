package cn.CDPersonal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/*
 * 原来的 exclude = {DataSourceAutoConfiguration.class} 会导致
 * application.yml 里的 spring.datasource 配置完全不生效（数据源不被创建），
 * 菜单/用户查询拿不到数据库连接，因此这里必须去掉。
 */
@SpringBootApplication
public class SystemApplication {
    public static void main(String[] args)
    {
        SpringApplication.run(SystemApplication.class, args);
        System.out.println("╰(*°▽°*)╯  系统启动成功   ψ(｀∇´)ψ  \n");
    }
}