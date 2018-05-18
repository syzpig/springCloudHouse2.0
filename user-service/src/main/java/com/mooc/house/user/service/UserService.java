package com.mooc.house.user.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.mooc.house.user.common.UserException;
import com.mooc.house.user.common.UserException.Type;
import com.mooc.house.user.mapper.UserMapper;
import com.mooc.house.user.model.User;
import com.mooc.house.user.utils.BeanHelper;
import com.mooc.house.user.utils.HashUtils;
import com.mooc.house.user.utils.JwtHelper;

@Service
public class UserService {

    //查询用户会产生高并发，所以引入redis，创建redis，注入redis对象
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MailService mailService;


    @Value("${file.prefix}")
    private String imgPrefix;

    /**
     * 1.首先通过缓存获取
     * 2.不存在将从通过数据库获取用户对象
     * 3.将用户对象写入缓存，设置缓存时间5分钟
     * 4.返回对象
     *
     * @param id
     * @return
     */
    public User getUserById(Long id) {
        String key = "user:" + id;
        String json = redisTemplate.opsForValue().get(key);//以用户id做key
        User user = null;
        if (Strings.isNullOrEmpty(json)) {
            user = userMapper.selectById(id);
            user.setAvatar(imgPrefix + user.getAvatar());//设置一个用户头像
            String string = JSON.toJSONString(user);//序列化
            redisTemplate.opsForValue().set(key, string);//放入缓存中
            redisTemplate.expire(key, 5, TimeUnit.MINUTES);//设置键过期时间
        } else {
            user = JSON.parseObject(json, User.class);
        }
        return user;
    }

    public List<User> getUserByQuery(User user) {
        List<User> users = userMapper.select(user);
        //对用户头像进行处理  在头像路径加上服务武器头路径
        users.forEach(u -> {
            u.setAvatar(imgPrefix + u.getAvatar());
        });
        return users;
    }

    /**
     * 注册
     *
     * @param user
     * @param enableUrl
     * @return
     */
    public boolean addAccount(User user, String enableUrl) {
        user.setPasswd(HashUtils.encryPassword(user.getPasswd()));//对用户密码进行加密加盐
        //BeanHelper工具类将默认值填充进去
        BeanHelper.onInsert(user);//把创建时间添加进去
        userMapper.insert(user);//保存用户到数据库
        registerNotify(user.getEmail(), enableUrl);//发送邮件
        return true;
    }

    /**
     * 发送注册激活邮件
     *
     * @param email     收件人Email地址
     * @param enableUrl 激活请求地址
     */
    private void registerNotify(String email, String enableUrl) {
        //对邮件进行加盐，与随机数拼接为redis key
        String randomKey = HashUtils.hashString(email) + RandomStringUtils.randomAlphabetic(10);
        redisTemplate.opsForValue().set(randomKey, email);//把邮件存入redis
        redisTemplate.expire(randomKey, 1, TimeUnit.HOURS);//对key进行设置过期时间，因为redis缓存是存入内存中的，耗资源
        // 创建邮件内容，也就是激活地址
        String content = enableUrl + "?key=" + randomKey;
        mailService.sendSimpleMail("房产平台激活邮件", content, email);
    }

    //不存在激活失败
    public boolean enable(String key) {
        String email = redisTemplate.opsForValue().get(key);//去Redis取接收人邮件
        if (StringUtils.isBlank(email)) {
            throw new UserException(UserException.Type.USER_NOT_FOUND, "无效的key");//自定义异常
        }
        User updateUser = new User();
        updateUser.setEmail(email);
        updateUser.setEnable(1);
        userMapper.update(updateUser);
        return true;
    }

    /**
     * 校验用户名密码、生成token并返回用户对象
     *
     * @param email
     * @param passwd
     * @return
     */
    public User auth(String email, String passwd) {
        if (StringUtils.isBlank(email) || StringUtils.isBlank(passwd)) {
            throw new UserException(Type.USER_AUTH_FAIL, "User Auth Fail");
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswd(HashUtils.encryPassword(passwd));
        user.setEnable(1);
        List<User> list = getUserByQuery(user);
        if (!list.isEmpty()) {
            User retUser = list.get(0);
            onLogin(retUser);//当用户存在，则给用户创建token
            return retUser;
        }
        throw new UserException(Type.USER_AUTH_FAIL, "User Auth Fail");
    }

    private void onLogin(User user) {
        //通过JWT创建token
        String token = JwtHelper.genToken(ImmutableMap.of("email", user.getEmail(), "name", user.getName(), "ts", Instant.now().getEpochSecond() + ""));
        renewToken(token, user.getEmail());
        user.setToken(token);
    }

    /**
     * 该系统把用户邮件当做唯一键，数据库主键，因此通过把他放入当成redis的key做标识，把token存入redis
     */
    private String renewToken(String token, String email) {
        redisTemplate.opsForValue().set(email, token);
        redisTemplate.expire(email, 30, TimeUnit.MINUTES);
        return token;
    }

    public User getLoginedUserByToken(String token) {
        Map<String, String> map = null;
        try {
            //校验token->先验证token是否被伪造，然后解码token。
            map = JwtHelper.verifyToken(token);
        } catch (Exception e) {
            throw new UserException(Type.USER_NOT_LOGIN, "User not login");
        }
        String email = map.get("email");
        Long expired = redisTemplate.getExpire(email);//获取token中存储token的email 为的key是否过期
        if (expired > 0L) {//不过期
            // TODO: 2018/5/18 这一步作用
            renewToken(token, email);//再一次放入把token放入，redis中
            User user = getUserByEmail(email);//则通过email获取用户信息,因为email为数据库主键
            user.setToken(token);
            return user;
        }
        throw new UserException(Type.USER_NOT_LOGIN, "user not login");

    }

    private User getUserByEmail(String email) {
        User user = new User();
        user.setEmail(email);
        List<User> list = getUserByQuery(user);
        if (!list.isEmpty()) {
            return list.get(0);
        }
        throw new UserException(Type.USER_NOT_FOUND, "User not found for " + email);
    }

    //登出
    public void invalidate(String token) {
        Map<String, String> map = JwtHelper.verifyToken(token);
        redisTemplate.delete(map.get("email"));//删除redis缓存
    }

    @Transactional(rollbackFor = Exception.class)
    public User updateUser(User user) {
        if (user.getEmail() == null) {
            return null;
        }
        if (!Strings.isNullOrEmpty(user.getPasswd())) {
            user.setPasswd(HashUtils.encryPassword(user.getPasswd()));
        }
        userMapper.update(user);
        return userMapper.selectByEmail(user.getEmail());
    }

    /**
     * author:syz
     * describtion:获取缓存redis中d的email
     */
    public void resetNotify(String email, String url) {
        String randomKey = "reset_" + RandomStringUtils.randomAlphabetic(10);
        redisTemplate.opsForValue().set(randomKey, email);
        redisTemplate.expire(randomKey, 1, TimeUnit.HOURS);
        String content = url + "?key=" + randomKey;
        mailService.sendSimpleMail("房产平台重置密码邮件", content, email);

    }

    /**
     * author:syz
     * describtion:获取缓存redis中d的email
     */
    public String getResetKeyEmail(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public User reset(String key, String password) {
        String email = getResetKeyEmail(key);
        User updateUser = new User();
        updateUser.setEmail(email);
        updateUser.setPasswd(HashUtils.encryPassword(password));
        userMapper.update(updateUser);
        return getUserByEmail(email);
    }

}
