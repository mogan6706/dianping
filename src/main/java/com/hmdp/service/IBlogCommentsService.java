// 文件说明：IBlogCommentsService 业务接口，定义 Blog Comments Service 模块对外提供的能力。

package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.BlogComments;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
// 业务接口：先定义当前模块要提供哪些能力
public interface IBlogCommentsService extends IService<BlogComments> {

    Result queryCommentsByBlogId(Long blogId, Integer current);

    Result queryRepliesByParentId(Long parentId, Integer current);

    Result saveComment(BlogComments comment);

    Result deleteComment(Long id);
}
