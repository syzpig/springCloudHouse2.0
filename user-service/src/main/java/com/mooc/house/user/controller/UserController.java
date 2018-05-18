package com.mooc.house.user.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mooc.house.user.common.RestResponse;
import com.mooc.house.user.model.User;
import com.mooc.house.user.service.UserService;

@RestController
@RequestMapping("user")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;
    //-------------------查询---------------------

    /**
     * 根据id查询用户
     */
    @RequestMapping("getById")
    public RestResponse<User> getUserById(Long id) {
        User user = userService.getUserById(id);
        return RestResponse.success(user);
    }

    /**
     * 查询用户列表
     */
    @RequestMapping("getList")
    public RestResponse<List<User>> getUserList(@RequestBody User user) {
        List<User> users = userService.getUserByQuery(user);
        return RestResponse.success(users);
    }


    //----------------------注册----------------------------------

    /**
     * 用户注册
     */
    @RequestMapping("add")
    public RestResponse<User> add(@RequestBody User user) {
        // TODO: 2018/5/18  user.getEnableUrl()
        userService.addAccount(user, user.getEnableUrl());
        return RestResponse.success();
    }

    /**
     * author:syz
     * 主要激活key的验证
     */
    @RequestMapping("enable")
    public RestResponse<Object> enable(String key) {
        userService.enable(key);
        return RestResponse.success();
    }

    //------------------------登录/鉴权--------------------------

    /**
     * author:syz
     * 验证用户是否存在
     * 不存在，返回异常
     * 存在，则返回该用户，并生成token,存入token
     */
    @RequestMapping("auth")
    public RestResponse<User> auth(@RequestBody User user) {
        User finalUser = userService.auth(user.getEmail(), user.getPasswd());
        return RestResponse.success(finalUser);
    }

    /**
     * author:syz
     * 根据token获取用户信息
     * 其实是根据email
     * 验证token  解析，获取email
     */
    @RequestMapping("get")
    public RestResponse<User> getUser(String token) {
        User finalUser = userService.getLoginedUserByToken(token);
        return RestResponse.success(finalUser);
    }

    /**
     * author: syz
     * describtion: 退出
     */
    @RequestMapping("logout")
    public RestResponse<Object> logout(String token) {
        userService.invalidate(token);
        return RestResponse.success();
    }
    /**
     * author: syz
     * describtion: 更新用户信息
     */
    @RequestMapping("update")
    public RestResponse<User> update(@RequestBody User user) {
        User updateUser = userService.updateUser(user);
        return RestResponse.success(updateUser);
    }
    /**
     * author:syz
     * describtion:重置密码
     */
    @RequestMapping("reset")
    public RestResponse<User> reset(String key, String password) {
        User updateUser = userService.reset(key, password);
        return RestResponse.success(updateUser);
    }
    /**
     * author:syz
     * describtion:获取缓存redis中d的email
     */
    @RequestMapping("getKeyEmail")
    public RestResponse<String> getKeyEmail(String key) {
        String email = userService.getResetKeyEmail(key);
        return RestResponse.success(email);
    }
    /**
     * author:syz
     * describtion:重置密码 发送给邮箱
     */
    @RequestMapping("resetNotify")
    public RestResponse<User> resetNotify(String email, String url) {
        userService.resetNotify(email, url);
        return RestResponse.success();
    }


}
