package com.mooc.house.user.utils;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Map;

import org.apache.commons.lang3.time.DateUtils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.collect.Maps;
/**
 *创建token工具类
 *
 * HMAC，全称为“Hash Message Authentication Code”，中文名“散列消息鉴别码”，主要是利用哈希算法，以一个密钥和一个消息为输入
 * ，生成一个消息摘要作为输出。一般的，消息鉴别码用于验证传输于两个共 同享有一个密钥的单位之间的消息。
 * HMAC 可以与任何迭代散列函数捆绑使用。MD5 和 SHA-1 就是这种散列函数。HMAC 还可以使用一个用于计算和确认消息鉴别值的密钥。

 *HMAC，散列消息鉴别码，是基于密钥的 Hash 算法的认证协议。它的实现原理是，用公开函数和密钥产生一个固定长度的值作为认证标识，
 *用这个标识鉴别消息的完整性。使用一个密钥生成一个固定大小的小数据块，即 MAC，并将其加入到消息中，然后传输。接收方利用与发送方
 *共享的密钥进行鉴别认证等。
 */
public class JwtHelper {
  
  private static final String  SECRET = "session_secret";
  
  private static final String  ISSUER = "mooc_user";//发布者
  
  //生成token
  public static String genToken(Map<String, String> claims){
    try {
      Algorithm algorithm = Algorithm.HMAC256(SECRET);//定义生成token算法
      //创建一个JWTCreator      withExpiresAt创建过期时间
      JWTCreator.Builder builder = JWT.create().withIssuer(ISSUER).withExpiresAt(DateUtils.addDays(new Date(), 1));
      claims.forEach((k,v) -> builder.withClaim(k, v));//把claims值存储到token
      return builder.sign(algorithm).toString();//
    } catch (IllegalArgumentException | UnsupportedEncodingException e) {
      throw new RuntimeException(e);//如果Claim不能转换为JSON，或者在签名过程中使用的密钥无效，那么将会抛出JWTCreationException异常。
    }
  }

  // TODO: 2018/5/18
  //校验验证token ==验证令牌   先验证token是否被伪造，然后解码token。
  public static Map<String, String> verifyToken(String token)  {
    Algorithm algorithm = null;
    try {
      algorithm = Algorithm.HMAC256(SECRET);//定义生成token算法  这边需要注意的地方是解密的秘钥必须跟加密时是相同的，不然解密必然失败，就是bug了
    } catch (IllegalArgumentException | UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
    //首先需要通过调用jwt.require()和传递算法实例来创建一个JWTVerifier实例。如果您要求令牌具有特定的Claim值，请使用构建器来定义它们。
    // 方法build()返回的实例是可重用的，因此您可以定义一次，并使用它来验证不同的标记。最后调用verifier.verify()来验证token
    JWTVerifier verifier = JWT.require(algorithm).withIssuer(ISSUER).build();
    DecodedJWT jwt =  verifier.verify(token); //解密后的DecodedJWT对象，可以读取token中的数据。
    Map<String, Claim> map = jwt.getClaims();
    Map<String, String> resultMap = Maps.newHashMap();
    map.forEach((k,v) -> resultMap.put(k, v.asString()));
    return resultMap;
  }
/**
 *Claim 索赔类是索赔值的包装器。它允许您将索赔作为不同的类类型。
 */
}
