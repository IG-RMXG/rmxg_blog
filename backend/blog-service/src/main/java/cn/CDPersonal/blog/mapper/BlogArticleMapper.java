package cn.CDPersonal.blog.mapper;

import cn.CDPersonal.blog.domain.BlogArticle;
import cn.CDPersonal.blog.domain.BlogTag;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 博客文章 Mapper（blog_article）
 *
 * 状态约定：0=草稿 1=已发布 2=待审核 3=已驳回；本接口不按状态过滤，
 * 可见性（普通用户只看自己的、门户只看已发布）由 Service 与 BlogPortalMapper 分别负责。
 * 表里没有逻辑删除列，删除即物理删除；view/like/comment_count 是冗余计数列，用 COALESCE 兜住历史 NULL。
 */
@Mapper
public interface BlogArticleMapper {

    /**
     * 文章列表（分页由 PageHelper 拦截）。
     * 普通用户调用前由 service 把 query.userId 设成当前用户，实现"只看自己的文章"。
     */
    // 排序 is_top DESC, article_id DESC：置顶优先，其余按主键倒序（近似"最新在前"），避免同秒发布时顺序抖动。
    // tag_names 是 GROUP_CONCAT 出来的一行逗号串，只供列表展示；文章没有标签时该列为 NULL 而不是空串。
    @Select("""
            <script>
            SELECT a.article_id, a.title, a.summary, a.cover_url, a.category_id, a.status,
                   a.is_top, a.is_recommend, a.view_count, a.like_count, a.comment_count,
                   a.user_id, a.publish_time, a.create_by, a.create_time, a.update_by, a.update_time, a.remark,
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
              <if test="title != null and title != ''">
                AND a.title LIKE CONCAT('%', #{title}, '%')
              </if>
              <if test="status != null and status != ''">
                AND a.status = #{status}
              </if>
              <if test="categoryId != null">
                AND a.category_id = #{categoryId}
              </if>
              <if test="isTop != null and isTop != ''">
                AND a.is_top = #{isTop}
              </if>
              <if test="isRecommend != null and isRecommend != ''">
                AND a.is_recommend = #{isRecommend}
              </if>
              <if test="userId != null">
                AND a.user_id = #{userId}
              </if>
              <if test="params.beginTime != null and params.beginTime != ''">
                AND DATE(a.create_time) &gt;= DATE(#{params.beginTime})
              </if>
              <if test="params.endTime != null and params.endTime != ''">
                AND DATE(a.create_time) &lt;= DATE(#{params.endTime})
              </if>
            </where>
            ORDER BY a.is_top DESC, a.article_id DESC
            </script>
            """)
    List<BlogArticle> selectArticleList(BlogArticle query);

    // 详情带正文 content、不含 tag_names 聚合串；标签由下方 selectTagsByArticleId 单独查，编辑回显用。
    @Select("""
            SELECT a.article_id, a.title, a.summary, a.content, a.cover_url, a.category_id, a.status,
                   a.is_top, a.is_recommend, a.view_count, a.like_count, a.comment_count,
                   a.user_id, a.publish_time, a.create_by, a.create_time, a.update_by, a.update_time, a.remark,
                   c.category_name,
                   u.nick_name AS author_nick_name
            FROM blog_article a
            LEFT JOIN blog_category c ON c.category_id = a.category_id
            LEFT JOIN sys_user u ON u.user_id = a.user_id
            WHERE a.article_id = #{articleId}
            """)
    BlogArticle selectArticleById(@Param("articleId") Long articleId);

    // 文章已选标签，排序与标签下拉一致，保证编辑页勾选项的展示顺序稳定。
    @Select("""
            SELECT t.tag_id, t.tag_name, t.sort
            FROM blog_article_tag at2
            JOIN blog_tag t ON t.tag_id = at2.tag_id
            WHERE at2.article_id = #{articleId}
            ORDER BY t.sort ASC, t.tag_id ASC
            """)
    List<BlogTag> selectTagsByArticleId(@Param("articleId") Long articleId);

    // 计数列与 publish_time 由 Service 先补默认值再插入（非空约定），create_time 用数据库 NOW() 生成。
    @Insert("""
            INSERT INTO blog_article
              (title, summary, content, cover_url, category_id, status,
               is_top, is_recommend, view_count, like_count, comment_count,
               user_id, publish_time, create_by, create_time, remark)
            VALUES
              (#{title}, #{summary}, #{content}, #{coverUrl}, #{categoryId}, #{status},
               #{isTop}, #{isRecommend}, #{viewCount}, #{likeCount}, #{commentCount},
               #{userId}, #{publishTime}, #{createBy}, NOW(), #{remark})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "articleId")
    int insertArticle(BlogArticle article);

    // 刻意不写 view_count / like_count / comment_count：编辑保存不会把前端提交的计数写回，计数只由增减语句维护。
    @Update("""
            UPDATE blog_article
            SET title = #{title},
                summary = #{summary},
                content = #{content},
                cover_url = #{coverUrl},
                category_id = #{categoryId},
                status = #{status},
                is_top = #{isTop},
                is_recommend = #{isRecommend},
                publish_time = #{publishTime},
                update_by = #{updateBy},
                update_time = NOW(),
                remark = #{remark}
            WHERE article_id = #{articleId}
            """)
    int updateArticle(BlogArticle article);

    /** 单独更新状态（发布/审核/驳回/回收草稿） */
    // 只动 status / publish_time / update_by：转入非发布态时 Service 传 null 清空发布时间，
    // 转回已发布且原有发布时间不为空时沿用首次发布时间，避免重复发布把时间刷新。
    @Update("""
            UPDATE blog_article
            SET status = #{status},
                publish_time = #{publishTime},
                update_by = #{updateBy},
                update_time = NOW()
            WHERE article_id = #{articleId}
            """)
    int updateStatus(@Param("articleId") Long articleId,
                     @Param("status") String status,
                     @Param("publishTime") java.util.Date publishTime,
                     @Param("updateBy") String updateBy);

    // 原子自增：COALESCE 兜住历史数据里的 NULL 计数，否则 NULL + 1 仍然是 NULL；没有去重/防刷，刷新即 +1。
    @Update("UPDATE blog_article SET view_count = COALESCE(view_count, 0) + 1 WHERE article_id = #{articleId}")
    int incrementViewCount(@Param("articleId") Long articleId);

    // 与评论插入同事务执行；审核（隐藏/恢复正常）不改评论行数，所以不需要走这里。
    @Update("UPDATE blog_article SET comment_count = COALESCE(comment_count, 0) + 1 WHERE article_id = #{articleId}")
    int incrementCommentCount(@Param("articleId") Long articleId);

    // GREATEST(..., 0) 保证计数不会被减成负数（评论已被删过一次、或计数本就为 0 时仍安全）。
    @Update("UPDATE blog_article SET comment_count = GREATEST(COALESCE(comment_count, 0) - 1, 0) WHERE article_id = #{articleId}")
    int decrementCommentCount(@Param("articleId") Long articleId);

    // 物理批量删除；blog_article_tag 中的关联由下方 deleteArticleTagsByArticleIds 先行清理。
    @Delete("""
            <script>
            DELETE FROM blog_article WHERE article_id IN
            <foreach collection="array" item="articleId" open="(" separator="," close=")">
              #{articleId}
            </foreach>
            </script>
            """)
    int deleteArticleByIds(@Param("array") Long[] articleIds);

    // ==================== 文章-标签关联 ====================

    // 保存标签采用「全删再全插」的覆盖式写法：编辑时先清空旧关联，再由 insertArticleTags 批量写入本次选择。
    @Delete("DELETE FROM blog_article_tag WHERE article_id = #{articleId}")
    int deleteArticleTags(@Param("articleId") Long articleId);

    // 调用前必须保证 tagIds 非空：<foreach> 展开成空列表会拼出 "VALUES" 后无值的非法 SQL，Service 已做判空。
    @Insert("""
            <script>
            INSERT INTO blog_article_tag (article_id, tag_id) VALUES
            <foreach collection="tagIds" item="tagId" separator=",">
              (#{articleId}, #{tagId})
            </foreach>
            </script>
            """)
    int insertArticleTags(@Param("articleId") Long articleId, @Param("tagIds") Long[] tagIds);

    // 删除文章前批量清理关联：两表间无外键级联，顺序反了会留下指向已删文章的中间表记录。
    @Delete("""
            <script>
            DELETE FROM blog_article_tag WHERE article_id IN
            <foreach collection="array" item="articleId" open="(" separator="," close=")">
              #{articleId}
            </foreach>
            </script>
            """)
    int deleteArticleTagsByArticleIds(@Param("array") Long[] articleIds);
}
