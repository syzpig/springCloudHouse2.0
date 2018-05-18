package com.mooc.house.user.utils;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import com.google.common.base.Throwables;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
/**
 *密码加盐工具类，防止明文出现
 */
public class HashUtils {


  private static final HashFunction FUNCTION = Hashing.md5();//借助gava中的加密类
  
  private static final HashFunction MURMUR_FUNC = Hashing.murmur3_128();//创建盐，因为防止md5被暴力破解，所以加盐
  
  private static final String       SALT     = "mooc.com";


  public static String encryPassword(String password){
    HashCode code = FUNCTION.hashString(password+SALT, Charset.forName("UTF-8"));
    return code.toString();
  }

  public static String hashString(String input){
    HashCode code = null;
    try {
      code = MURMUR_FUNC.hashBytes(input.getBytes("utf-8"));
    } catch (UnsupportedEncodingException e) {
      Throwables.propagate(e);
    }
    return code.toString();
  }
  
  public static void main(String[] args) {
    System.out.println(encryPassword("111111"));
  }
}
