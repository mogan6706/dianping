// 文件说明：正则表达式常量类，集中保存手机号、验证码、邮箱等格式规则。

package com.hmdp.utils;

public abstract class RegexPatterns {
    // 手机号格式
    public static final String PHONE_REGEX = "^1([38][0-9]|4[579]|5[0-3,5-9]|6[6]|7[0135678]|9[89])\\d{8}$";
    // 邮箱格式
    public static final String EMAIL_REGEX = "^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$";
    // 密码格式
    public static final String PASSWORD_REGEX = "^\\w{4,32}$";
    // 验证码格式
    public static final String VERIFY_CODE_REGEX = "^[a-zA-Z\\d]{6}$";

}
