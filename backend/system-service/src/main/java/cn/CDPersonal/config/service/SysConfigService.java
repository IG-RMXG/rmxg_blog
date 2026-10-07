package cn.CDPersonal.config.service;

import cn.CDPersonal.common.domain.entity.SysConfig;
import cn.CDPersonal.config.mapper.SysConfigMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 参数配置管理。
 *
 * 当前没有引入参数缓存（每次直接查库），refreshCache 由 controller 直接返回成功。
 */
@Service
public class SysConfigService {

    /**
     * 内置参数 id 上界（不含）：RuoYi 初始化脚本里内置参数是 config_id 1~8，
     * 这些参数被系统逻辑依赖，删掉会导致登录/注册等功能异常。
     *
     * 这里用 &lt; 而不是 &lt;= ：RuoYi 的建库脚本对 sys_config 执行了
     * AUTO_INCREMENT = 100，用户新增的第一条参数 id 正好是 100，
     * 用 &lt;= 会把它误判成内置参数、导致"自己新增的参数删不掉"。
     */
    private static final long BUILTIN_MAX_ID_EXCLUSIVE = 100L;

    private final SysConfigMapper configMapper;

    public SysConfigService(SysConfigMapper configMapper) {
        this.configMapper = configMapper;
    }

    public List<SysConfig> selectConfigList(SysConfig query) {
        return configMapper.selectConfigList(query);
    }

    public SysConfig selectConfigById(Long configId) {
        return configMapper.selectConfigById(configId);
    }

    /** 按参数键名取参数值，键名不存在返回 null */
    public String selectConfigValueByKey(String configKey) {
        SysConfig config = configMapper.checkConfigKeyUnique(configKey);
        return config == null ? null : config.getConfigValue();
    }

    /** 参数键名是否唯一（新增时 configId 为空，修改时排除自身） */
    public boolean checkConfigKeyUnique(SysConfig config) {
        SysConfig exist = configMapper.checkConfigKeyUnique(config.getConfigKey());
        if (exist == null) {
            return true;
        }
        return exist.getConfigId() != null && exist.getConfigId().equals(config.getConfigId());
    }

    /**
     * 删除前逐个校验，返回第一个不允许删除的内置参数；全部可删时返回 null。
     * 判据：config_type = 'Y'（RuoYi 约定）或 config_id &lt; 100（内置初始化数据）。
     */
    public SysConfig findUndeletableConfig(Long[] configIds) {
        for (Long configId : configIds) {
            SysConfig config = configMapper.selectConfigById(configId);
            if (config == null) {
                // 记录已不存在，直接跳过，不再阻断删除
                continue;
            }
            boolean builtin = "Y".equals(config.getConfigType())
                    || (config.getConfigId() != null && config.getConfigId() < BUILTIN_MAX_ID_EXCLUSIVE);
            if (builtin) {
                return config;
            }
        }
        return null;
    }

    @Transactional
    public int insertConfig(SysConfig config) {
        return configMapper.insertConfig(config);
    }

    @Transactional
    public int updateConfig(SysConfig config) {
        return configMapper.updateConfig(config);
    }

    @Transactional
    public int deleteConfigByIds(Long[] configIds) {
        return configMapper.deleteConfigByIds(configIds);
    }
}
