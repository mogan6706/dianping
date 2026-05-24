// 文件说明：BlogCommentsServiceImpl 业务实现类，真正编排 Blog Comments 模块的业务流程。

package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.BlogComments;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogCommentsMapper;
import com.hmdp.service.IBlogCommentsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IBlogService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import com.hmdp.vo.BlogCommentVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

// 业务类：负责处理当前模块的核心业务逻辑
@Service
// 业务实现类：真正编排当前模块的业务流程
public class BlogCommentsServiceImpl extends ServiceImpl<BlogCommentsMapper, BlogComments> implements IBlogCommentsService {

    // 注入 blogService，用来校验博客是否存在并维护评论数。
    @Resource
    private IBlogService blogService;
    // 注入 userService，用来补充评论作者昵称和头像。
    @Resource
    private IUserService userService;

    // 分页查询某篇博客下的一级评论。
    @Override
    public Result queryCommentsByBlogId(Long blogId, Integer current) {
        if (blogId == null) {
            return Result.fail("博客id不能为空");
        }
        Page<BlogComments> page = query()
                .eq("blog_id", blogId)
                .eq("parent_id", 0)
                .and(wrapper -> wrapper.eq("status", false).or().isNull("status"))
                .orderByDesc("create_time")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        return Result.ok(toVOList(page.getRecords()), page.getTotal());
    }

    // 分页查询某条一级评论下的回复。
    @Override
    public Result queryRepliesByParentId(Long parentId, Integer current) {
        if (parentId == null) {
            return Result.fail("父评论id不能为空");
        }
        Page<BlogComments> page = query()
                .eq("parent_id", parentId)
                .ne("parent_id", 0)
                .and(wrapper -> wrapper.eq("status", false).or().isNull("status"))
                .orderByAsc("create_time")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        return Result.ok(toVOList(page.getRecords()), page.getTotal());
    }

    // 新增评论或回复。
    @Override
    @Transactional
    public Result saveComment(BlogComments comment) {
        if (comment == null || comment.getBlogId() == null) {
            return Result.fail("博客id不能为空");
        }
        if (StrUtil.isBlank(comment.getContent())) {
            return Result.fail("评论内容不能为空");
        }
        Blog blog = blogService.getById(comment.getBlogId());
        if (blog == null) {
            return Result.fail("博客不存在");
        }
        // 评论归属当前登录用户，前端传来的 userId 不可信。
        UserDTO user = UserHolder.getUser();
        comment.setUserId(user.getId());
        if (comment.getParentId() == null) {
            comment.setParentId(0L);
        }
        if (comment.getAnswerId() == null) {
            comment.setAnswerId(0L);
        }
        comment.setLiked(0);
        comment.setStatus(false);
        boolean success = save(comment);
        if (!success) {
            return Result.fail("评论失败");
        }
        // 维护博客评论数，包含一级评论和回复。
        blogService.update().setSql("comments = IFNULL(comments, 0) + 1").eq("id", comment.getBlogId()).update();
        return Result.ok(comment.getId());
    }

    // 删除当前用户自己的评论。
    @Override
    @Transactional
    public Result deleteComment(Long id) {
        if (id == null) {
            return Result.fail("评论id不能为空");
        }
        BlogComments comment = getById(id);
        if (comment == null) {
            return Result.fail("评论不存在");
        }
        Long userId = UserHolder.getUser().getId();
        if (!Objects.equals(comment.getUserId(), userId)) {
            return Result.fail("只能删除自己的评论");
        }
        boolean success = removeById(id);
        if (!success) {
            return Result.fail("删除评论失败");
        }
        blogService.update()
                .setSql("comments = IF(comments IS NULL OR comments <= 0, 0, comments - 1)")
                .eq("id", comment.getBlogId())
                .update();
        return Result.ok();
    }

    // 批量转换 VO，并补充评论作者信息。
    private List<BlogCommentVO> toVOList(List<BlogComments> comments) {
        if (comments == null || comments.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> userIds = comments.stream().map(BlogComments::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (left, right) -> left));
        return comments.stream().map(comment -> {
            BlogCommentVO vo = BlogCommentVO.from(comment);
            User user = userMap.get(comment.getUserId());
            if (user != null) {
                vo.setNickName(user.getNickName());
                vo.setIcon(user.getIcon());
            }
            return vo;
        }).collect(Collectors.toList());
    }
}
