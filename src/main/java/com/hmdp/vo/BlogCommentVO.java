// 文件说明：博客评论视图对象，返回给前端时补充评论作者信息。

package com.hmdp.vo;

import com.hmdp.entity.BlogComments;
import lombok.Data;

import java.time.LocalDateTime;

// Lombok 注解：自动生成 getter、setter、toString 等常用方法
@Data
public class BlogCommentVO {
    private Long id;
    private Long userId;
    private Long blogId;
    private Long parentId;
    private Long answerId;
    private String content;
    private Integer liked;
    private LocalDateTime createTime;
    // 评论作者昵称
    private String nickName;
    // 评论作者头像
    private String icon;

    public static BlogCommentVO from(BlogComments comment) {
        BlogCommentVO vo = new BlogCommentVO();
        vo.setId(comment.getId());
        vo.setUserId(comment.getUserId());
        vo.setBlogId(comment.getBlogId());
        vo.setParentId(comment.getParentId());
        vo.setAnswerId(comment.getAnswerId());
        vo.setContent(comment.getContent());
        vo.setLiked(comment.getLiked());
        vo.setCreateTime(comment.getCreateTime());
        return vo;
    }
}
