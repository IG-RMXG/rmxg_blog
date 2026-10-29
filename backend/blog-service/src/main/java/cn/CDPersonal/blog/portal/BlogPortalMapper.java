package cn.CDPersonal.blog.portal;

import cn.CDPersonal.blog.domain.BlogArticle;
import cn.CDPersonal.blog.domain.BlogCategory;
import cn.CDPersonal.blog.domain.BlogComment;
import cn.CDPersonal.blog.domain.BlogTag;
import cn.CDPersonal.blog.portal.domain.PortalArchiveItem;
import cn.CDPersonal.blog.portal.domain.PortalAuthor;
import cn.CDPersonal.blog.portal.domain.PortalQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 门户只读查询（/blog/public/**）。
 *
 * 与 BlogArticleMapper 的区别：这里的所有查询都写死 status = '1'（已发布），
 * 不参与管理员/普通用户分权——门户面对的是游客，只能看已发布内容。
 * 安全：所有条件一律 #{}，排序字段用 <choose> 白名单，没有任何 ${} 拼接。
 */
@Mapper
public interface BlogPortalMapper {

    // ==================== 文章 ====================

    @Select("""
            <script>
            SELECT a.article_id, a.title, a.summary, a.cover_url, a.category_id, a.status,
                   a.is_top, a.is_recommend, a.view_count, a.like_count, a.comment_count,
                   a.user_id, a.publish_time, a.create_time,
                   c.category_name,
                   u.nick_name AS author_nick_name,
                   (SELECT GROUP_CONCAT(t.tag_name ORDER BY t.tag_name SEPARATOR ',')
                      FROM blog_article_tag at2
                      JOIN blog_tag t ON t.tag_id = at2.tag_id
                     WHERE at2.article_id = a.article_id) AS tag_names
            FROM blog_article a
            LEFT JOIN blog_category c ON c.category_id = a.category_id
            LEFT JOIN sys_user u ON u.user_id = a.user_id
            <where>
              a.status = '1'
              <if test="categoryId != null">
                AND a.category_id = #{categoryId}
              </if>
              <if test="tagId != null">
                AND EXISTS (SELECT 1 FROM blog_article_tag at3
                             WHERE at3.article_id = a.article_id AND at3.tag_id = #{tagId})
              </if>
              <if test="keyword != null and keyword != ''">
                AND (a.title LIKE CONCAT('%', #{keyword}, '%')
                     OR a.summary LIKE CONCAT('%', #{keyword}, '%'))
              </if>
              <if test="month != null and month != ''">
                AND DATE_FORMAT(a.publish_time, '%Y-%m') = #{month}
              </if>
            </where>
            ORDER BY
            <choose>
              <when test="orderBy != null and orderBy == 'hot'">
                a.view_count DESC, a.publish_time DESC, a.article_id DESC
              </when>
              <when test="orderBy != null and orderBy == 'comments'">
                a.comment_count DESC, a.publish_time DESC, a.article_id DESC
              </when>
              <otherwise>
                a.is_top DESC, a.publish_time DESC, a.article_id DESC
              </otherwise>
            </choose>
            </script>
            """)
    List<BlogArticle> selectPublishedList(PortalQuery query);

    @Select("""
            SELECT a.article_id, a.title, a.summary, a.content, a.cover_url, a.category_id, a.status,
                   a.is_top, a.is_recommend, a.view_count, a.like_count, a.comment_count,
                   a.user_id, a.publish_time, a.create_time, a.update_time,
                   c.category_name,
                   u.nick_name AS author_nick_name
            FROM blog_article a
            LEFT JOIN blog_category c ON c.category_id = a.category_id
            LEFT JOIN sys_user u ON u.user_id = a.user_id
            WHERE a.article_id = #{articleId} AND a.status = '1'
            """)
    BlogArticle selectPublishedById(@Param("articleId") Long articleId);

    @Select("""
            SELECT t.tag_id, t.tag_name, t.sort
            FROM blog_article_tag at2
            JOIN blog_tag t ON t.tag_id = at2.tag_id
            WHERE at2.article_id = #{articleId}
            ORDER BY t.sort ASC, t.tag_id ASC
            """)
    List<BlogTag> selectTagsByArticleId(@Param("articleId") Long articleId);

    /** 热门文章（按浏览量），限条数，供首页/详情页侧栏使用 */
    @Select("""
            SELECT a.article_id, a.title, a.view_count, a.comment_count, a.publish_time,
                   c.category_name
            FROM blog_article a
            LEFT JOIN blog_category c ON c.category_id = a.category_id
            WHERE a.status = '1'
            ORDER BY a.view_count DESC, a.article_id DESC
            LIMIT #{limit}
            """)
    List<BlogArticle> selectHotArticles(@Param("limit") int limit);

    // ==================== 分类 / 标签 ====================

    /** 有已发布文章的分类（含已发布文章数），按 sort 升序 */
    @Select("""
            SELECT t.category_id, t.category_name, t.sort, t.article_count
            FROM (
              SELECT c.category_id, c.category_name, c.sort,
                     (SELECT COUNT(*) FROM blog_article a
                       WHERE a.category_id = c.category_id AND a.status = '1') AS article_count
              FROM blog_category c
              WHERE c.status = '0'
            ) t
            WHERE t.article_count > 0
            ORDER BY t.sort ASC, t.category_id ASC
            """)
    List<BlogCategory> selectPublishedCategories();

    /** 有已发布文章的标签（含已发布文章数），按 sort 升序 */
    @Select("""
            SELECT t.tag_id, t.tag_name, t.sort, t.article_count
            FROM (
              SELECT g.tag_id, g.tag_name, g.sort,
                     (SELECT COUNT(*)
                        FROM blog_article_tag at2
                        JOIN blog_article a ON a.article_id = at2.article_id
                       WHERE at2.tag_id = g.tag_id AND a.status = '1') AS article_count
              FROM blog_tag g
            ) t
            WHERE t.article_count > 0
            ORDER BY t.sort ASC, t.tag_id ASC
            """)
    List<BlogTag> selectPublishedTags();

    // ==================== 归档 ====================

    @Select("""
            SELECT DATE_FORMAT(a.publish_time, '%Y-%m') AS month, COUNT(*) AS count
            FROM blog_article a
            WHERE a.status = '1' AND a.publish_time IS NOT NULL
            GROUP BY month
            ORDER BY month DESC
            """)
    List<PortalArchiveItem> selectArchive();

    // ==================== 评论 ====================

    /** 某篇已发布文章的正常评论，按时间正序（楼层顺序） */
    @Select("""
            SELECT c.comment_id, c.article_id, c.parent_id, c.content, c.user_id, c.status,
                   c.create_by, c.create_time,
                   u.nick_name AS nick_name, u.user_name AS user_name
            FROM blog_comment c
            JOIN blog_article a ON a.article_id = c.article_id AND a.status = '1'
            LEFT JOIN sys_user u ON u.user_id = c.user_id
            WHERE c.article_id = #{articleId} AND c.status = '0'
            ORDER BY c.comment_id ASC
            """)
    List<BlogComment> selectPublishedComments(@Param("articleId") Long articleId);

    /** 最新评论（侧栏），只取已发布文章下的正常评论 */
    @Select("""
            SELECT c.comment_id, c.article_id, c.content, c.create_time,
                   a.title AS article_title,
                   u.nick_name AS nick_name, u.user_name AS user_name
            FROM blog_comment c
            JOIN blog_article a ON a.article_id = c.article_id AND a.status = '1'
            LEFT JOIN sys_user u ON u.user_id = c.user_id
            WHERE c.status = '0'
            ORDER BY c.comment_id DESC
            LIMIT #{limit}
            """)
    List<BlogComment> selectLatestComments(@Param("limit") int limit);

    // ==================== 站点统计 ====================

    /** 博主：取 role_key = admin 的第一个启用账号 */
    @Select("""
            SELECT u.user_id, u.nick_name, u.avatar
            FROM sys_user u
            JOIN sys_user_role ur ON ur.user_id = u.user_id
            JOIN sys_role r ON r.role_id = ur.role_id
            WHERE r.role_key = 'admin' AND u.status = '0' AND u.del_flag = '0'
            ORDER BY u.user_id ASC
            LIMIT 1
            """)
    PortalAuthor selectAuthor();

    @Select("SELECT COUNT(*) FROM blog_article WHERE status = '1'")
    int countPublishedArticles();

    @Select("SELECT COALESCE(SUM(view_count), 0) FROM blog_article WHERE status = '1'")
    long sumPublishedViews();

    @Select("""
            SELECT COUNT(*)
            FROM blog_comment c
            JOIN blog_article a ON a.article_id = c.article_id
            WHERE c.status = '0' AND a.status = '1'
            """)
    int countPublishedComments();

    // ==================== 浏览计数 ====================

    /** 门户浏览：只给已发布文章 +1，草稿/待审核不会被公开接口碰到 */
    @Update("""
            UPDATE blog_article SET view_count = COALESCE(view_count, 0) + 1
            WHERE article_id = #{articleId} AND status = '1'
            """)
    int incrementPublishedViewCount(@Param("articleId") Long articleId);
}
