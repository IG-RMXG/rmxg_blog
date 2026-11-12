package cn.CDPersonal.common.redis.config;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 自动配置：随 redis 模块的 AutoConfiguration.imports 注册，引入该模块的服务
 * 拿到的就是这一套（含网关、system-service），不必各自声明 Bean。
 *
 * <p>序列化约定（其它服务读写同一个 key 时必须与此一致，否则乱码或反序列化失败）：
 * <ul>
 *   <li>key / hashKey 用 String：redis-cli 里可直接读，也方便脚本按 key 排查。</li>
 *   <li>value / hashValue 用 Jackson JSON：JSON 里带类型信息，反序列化才能还原成原对象；
 *       被缓存的类因此必须有可访问的无参构造（如 CaptchaPointCache）。</li>
 * </ul>
 *
 * <p>{@code @AutoConfigureBefore(RedisAutoConfiguration.class)} 用于抢先于 Spring Boot 默认实现注册，
 * 配合 {@code @ConditionalOnMissingBean} 保证业务方自定义的 RedisTemplate 优先，本类只做兜底。
 */
@Configuration
@AutoConfigureBefore(RedisAutoConfiguration.class)
public class RedisConfiguration {

    // 仅当容器里恰好有一个 RedisConnectionFactory 时才注册：多数据源场景下不猜用哪个，交给业务方自己声明
    @Bean
    @ConditionalOnMissingBean(name = "redisTemplate")
    @ConditionalOnSingleCandidate(RedisConnectionFactory.class)
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        // 设置序列化器
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        // 手工 new 的 Bean 不会自动触发 Spring 的初始化回调，这里显式调用完成模板初始化
        template.afterPropertiesSet();
        System.out.println("RedisTemplate初始化完成");
        return template;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnSingleCandidate(RedisConnectionFactory.class)
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        return new StringRedisTemplate(redisConnectionFactory);
    }
}