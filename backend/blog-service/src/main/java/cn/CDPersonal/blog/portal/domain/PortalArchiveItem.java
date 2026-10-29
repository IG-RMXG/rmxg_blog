package cn.CDPersonal.blog.portal.domain;

/**
 * 门户归档项：某个月份的已发布文章数。
 */
public class PortalArchiveItem {

    /** 格式 yyyy-MM */
    private String month;

    private int count;

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
