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
 */
@Mapper
public interface BlogArticleMapper {

    /**
     * 文章列表（分页由 PageHelper 拦截）。
     * 普通用户调用前由 service 把 query.userId 设成当前用户，实现"只看自己的文章"。
     */
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

    @Select("""
            SELECT t.tag_id, t.tag_name, t.sort
            FROM blog_article_tag at2
            JOIN blog_tag t ON t.tag_id = at2.tag_id
            WHERE at2.article_id = #{articleId}
            ORDER BY t.sort ASC, t.tag_id ASC
            """)
    List<BlogTag> selectTagsByArticleId(@Param("articleId") Long articleId);

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

    @Update("UPDATE blog_article SET view_count = COALESCE(view_count, 0) + 1 WHERE article_id = #{articleId}")
    int incrementViewCount(@Param("articleId") Long articleId);

    @Update("UPDATE blog_article SET comment_count = COALESCE(comment_count, 0) + 1 WHERE article_id = #{articleId}")
    int incrementCommentCount(@Param("articleId") Long articleId);

    @Update("UPDATE blog_article SET comment_count = GREATEST(COALESCE(comment_count, 0) - 1, 0) WHERE article_id = #{articleId}")
    int decrementCommentCount(@Param("articleId") Long articleId);

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

    @Delete("DELETE FROM blog_article_tag WHERE article_id = #{articleId}")
    int deleteArticleTags(@Param("articleId") Long articleId);

    @Insert("""
            <script>
            INSERT INTO blog_article_tag (article_id, tag_id) VALUES
            <foreach collection="tagIds" item="tagId" separator=",">
              (#{articleId}, #{tagId})
            </foreach>
            </script>
            """)
    int insertArticleTags(@Param("articleId") Long articleId, @Param("tagIds") Long[] tagIds);

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
