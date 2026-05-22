// 文件说明：正则校验工具类，提供手机号、验证码、邮箱等常见字段的格式校验。

package com.hmdp.utils;

import cn.hutool.core.util.StrUtil;

public class RegexUtils {
    // 判断手机号格式是否错误
    public static boolean isPhoneInvalid(String phone){
        return mismatch(phone, RegexPatterns.PHONE_REGEX);
    }
    // 判断邮箱格式是否错误
    public static boolean isEmailInvalid(String email){
        return mismatch(email, RegexPatterns.EMAIL_REGEX);
    }

    // 判断验证码格式是否错误
    public static boolean isCodeInvalid(String code){
        return mismatch(code, RegexPatterns.VERIFY_CODE_REGEX);
    }

    // 按正则表达式判断字符串是否不匹配
    private static boolean mismatch(String str, String regex){
        if (StrUtil.isBlank(str)) {
            return true;
        }
        return !str.matches(regex);
    }
}
