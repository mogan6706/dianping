// 文件说明：UserServiceImpl 业务实现类，真正编排 User 模块的业务流程。

package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.USER_SIGN_KEY;

// 业务类：负责处理当前模块的核心业务逻辑
@Service
@Slf4j
// 业务实现类：真正编排当前模块的业务流程
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    // 注入 stringRedisTemplate（StringRedisTemplate）
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    // 发送登录验证码
    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 1. 校验手机号格式。
        if(RegexUtils.isPhoneInvalid(phone)) {
            // 2. 格式不对，直接返回。
            return Result.fail("手机号格式错误");
        }

        // 3. 生成 6 位验证码。
        String code = RandomUtil.randomNumbers(6);
        // 4. 把验证码存到 Redis。
       stringRedisTemplate.opsForValue().set(RedisConstants.LOGIN_CODE_KEY +phone,code,2, TimeUnit.MINUTES);
        // 5. 当前项目把验证码打印到日志里。
        log.info("短信验证码发送成功：{}",code);

        return Result.ok();

    }


    // 登录并返回 token
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String code = loginForm.getCode();
        String phone = loginForm.getPhone();
        // 1. 校验手机号格式。
        if(RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }
        // 2. 去 Redis 校验验证码。
        String cacheCode = stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY+phone);
        if(cacheCode==null||!cacheCode.equals(code)){
           return Result.fail("验证码不一致，请重新输入");
       }

        // 3. 根据手机号查询用户。
        User user = query().eq("phone",phone).one();

        // 4. 不存在则创建新用户。
        if(user==null){
           user=createUserWithPhone(phone);
        }
        // 5. 生成登录 token。
        String token = UUID.randomUUID().toString(true);

        // 6. 把用户信息转成 UserDTO。
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        // Redis Hash 只存字符串，这里把 UserDTO 字段统一转成字符串，避免类型序列化问题。
        Map<String, Object> map = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create().setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
        // 7. 把 token 和用户信息存到 Redis。
        String tokenKey=RedisConstants.LOGIN_USER_KEY+token;
        stringRedisTemplate.opsForHash().putAll(tokenKey,map);
        stringRedisTemplate.expire(tokenKey, RedisConstants.LOGIN_USER_TTL, TimeUnit.DAYS);
        // 8. 返回 token。
        return Result.ok(token);
    }

    // 退出登录并删除 Redis 里的 token
    @Override
    public Result logout(String token) {
        // 1. 没带 token 时，直接视为退出完成。
        if (StrUtil.isBlank(token)) {
            UserHolder.removeUser();
            return Result.ok();
        }
        // 2. 删除 Redis 里的登录信息。
        stringRedisTemplate.delete(RedisConstants.LOGIN_USER_KEY + token);
        // 3. 清理当前线程里的用户信息。
        UserHolder.removeUser();
        return Result.ok();
    }

    // 记录当天签到
    @Override
    public Result sign() {
        // 1. 获取当前登录用户。
        Long userId = UserHolder.getUser().getId();
        // 2. 拼接签到位图 key。
        LocalDateTime now = LocalDateTime.now();
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;
        // 3. 把今天对应的 bit 置为 1。
        int dayOfMonth = now.getDayOfMonth();
        stringRedisTemplate.opsForValue().setBit(key,dayOfMonth-1,true);
        return Result.ok();
    }

    // 统计连续签到天数
    @Override
    public Result signCount() {
        // 1. 获取当前登录用户和当前年月。
        Long userId = UserHolder.getUser().getId();
        LocalDateTime now = LocalDateTime.now();
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;
        // 2. 取出从 1 号到今天的签到 bit。
        int dayOfMonth = now.getDayOfMonth();
        // 使用 unsigned(dayOfMonth) 一次性取出本月 1 号到今天的签到位图。
        List<Long> result = stringRedisTemplate.opsForValue()
                .bitField(key, BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth))
                        .valueAt(0));
        if(result==null||result.isEmpty()){
            //没有任何签到结果
            return Result.ok(0);
        }
        Long num = result.get(0);
        if(num==0||num==null){
            return Result.ok(0);
        }
        // 3. 统计连续签到天数。
        int count=0;
        // 从最低位开始检查，最低位表示今天；遇到第一个 0 就说明连续签到中断。
        while (true) {
            //让这个数字与1做与运算，得到数字的最后一个bit位，判断这个bit是否为0
            if((num&1)==0) {
                //如果为0，未签到
                break;
            }else {
                //如果不为0，已签到，计算器+1
                count++;
                //把数字右移一位，抛弃最后一个bit位，继续下一个bit位
                num>>>=1;
            }
        }
        return Result.ok(count);

    }

    // 根据手机号创建新用户
    private User createUserWithPhone(String phone) {
        // 首次登录自动注册，只保存手机号和随机昵称。
        User user = new User();
        user.setPhone(phone);
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX +RandomUtil.randomString(10));
        //保存
        save(user);
        return user;

    }
}
