package com.mooc.house.user.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.AvailabilityFilteringRule;
import com.netflix.loadbalancer.IPing;
import com.netflix.loadbalancer.IRule;
import com.netflix.loadbalancer.PingUrl;
//todo  用法
/**
 *　配置模块的核心是实现ribbon管理实时配置化。配置模块主要有三个部分：配置管理器客户端（IClientConfig），
 * 配置管理器工场（ClientConfigFactory），配置key对象（IClientConfigKey）。IClientConfig定义了获取配置的方法
 * ，ClientConfigFactory是IClientConfig的工厂类，子类DefaultClientConfigFactory创建DefaultClientConfigImpl配置管
 * 理器。IClientConfigKey为配置的key对象，本质上内部是一个string类型字段。IClientConfig也实现了builder模式，
 * 也可以通过builder方式进行初始化。
 */
public class NewRuleConfig {
	
	@Autowired
	private IClientConfig ribbonClientConfig;
	
	@Bean
	public IPing ribbonPing(IClientConfig config){
		return new PingUrl(false,"/health");
	}
	
	@Bean
	public IRule ribbonRule(IClientConfig config){
		return new AvailabilityFilteringRule();
	}

}
