package cn.CDPersonal.blog.portal.domain;

/**
 * 门户展示的博主信息（取自 sys_user 中 role_key = admin 的账号）。
 *
 * 只暴露昵称和头像，不返回邮箱、手机号等隐私字段。
 *
 * 取值口径见 BlogPortalMapper.selectAuthor：在 role_key='admin' 且状态正常、未逻辑删除的账号中
 * 取 user_id 最小的一个，因此存在多个管理员时门户只展示这一位。
 */
public class PortalAuthor {

    private Long userId;
    private String nickName;
    private String avatar;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
}
