package cn.CDPersonal.post.mapper;

import cn.CDPersonal.common.domain.entity.SysPost;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 岗位 mapper。sys_post 没有 del_flag，删除是物理删除。
 */
@Mapper
public interface SysPostMapper {

    @Select("""
            <script>
            SELECT post_id, post_code, post_name, post_sort, status,
                   create_by, create_time, update_by, update_time, remark
            FROM sys_post
            <where>
              <if test="postCode != null and postCode != ''">
                AND post_code LIKE CONCAT('%', #{postCode}, '%')
              </if>
              <if test="postName != null and postName != ''">
                AND post_name LIKE CONCAT('%', #{postName}, '%')
              </if>
              <if test="status != null and status != ''">
                AND status = #{status}
              </if>
            </where>
            ORDER BY post_sort ASC
            </script>
            """)
    List<SysPost> selectPostList(SysPost query);

    @Select("""
            SELECT post_id, post_code, post_name, post_sort, status,
                   create_by, create_time, update_by, update_time, remark
            FROM sys_post
            WHERE post_id = #{postId}
            """)
    SysPost selectPostById(@Param("postId") Long postId);

    /** 岗位编码唯一性校验（调用方负责排除自身） */
    @Select("""
            SELECT post_id, post_code, post_name, post_sort, status
            FROM sys_post
            WHERE post_code = #{postCode}
            LIMIT 1
            """)
    SysPost checkPostCodeUnique(@Param("postCode") String postCode);

    /** 岗位名称唯一性校验（调用方负责排除自身） */
    @Select("""
            SELECT post_id, post_code, post_name, post_sort, status
            FROM sys_post
            WHERE post_name = #{postName}
            LIMIT 1
            """)
    SysPost checkPostNameUnique(@Param("postName") String postName);

    @Insert("""
            INSERT INTO sys_post
              (post_code, post_name, post_sort, status, create_by, create_time, remark)
            VALUES
              (#{postCode}, #{postName}, #{postSort}, #{status}, #{createBy}, NOW(), #{remark})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "postId")
    int insertPost(SysPost post);

    @Update("""
            UPDATE sys_post
            SET post_code = #{postCode},
                post_name = #{postName},
                post_sort = #{postSort},
                status = #{status},
                update_by = #{updateBy},
                update_time = NOW(),
                remark = #{remark}
            WHERE post_id = #{postId}
            """)
    int updatePost(SysPost post);

    @Delete("""
            <script>
            DELETE FROM sys_post WHERE post_id IN
            <foreach collection="array" item="postId" open="(" separator="," close=")">
              #{postId}
            </foreach>
            </script>
            """)
    int deletePostByIds(@Param("array") Long[] postIds);

    /** 岗位是否已分配给用户（删除前校验） */
    @Select("""
            <script>
            SELECT COUNT(*) FROM sys_user_post WHERE post_id IN
            <foreach collection="array" item="postId" open="(" separator="," close=")">
              #{postId}
            </foreach>
            </script>
            """)
    int countUserPostByIds(@Param("array") Long[] postIds);
}
