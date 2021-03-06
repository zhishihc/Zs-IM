package com.zs.server.config;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import feign.Feign;
import feign.Request;
import feign.Retryer;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.I0Itec.zkclient.ZkClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author zhishi
 * @version 1.0
 * @date 2022/4/11 10:38 上午
 */
@Slf4j
@Configuration
public class AppBeanConfig {

    @Autowired
    private AppGlobalConfig appConfig;

    @Value("${server.port}")
    private String httpServerPort;

    /**
     * zkClient
     * @return
     */
    @Bean
    ZkClient zkClient () {
        return new ZkClient(appConfig.getZkAddress(), appConfig.getZkConnectionTimeout());
    }

    /**
     * 启动后向zk根节点注册自身服务器信息
     * @return
     */
    @Bean
    CommandLineRunner registerCommand() {
        return args -> {
            //节点名称
            String addr = InetAddress.getLocalHost().getHostAddress();
            String nodeName = "ip-" + addr + ":" + appConfig.getNettyPort() + ":" + httpServerPort + ":" + appConfig.getWeight();

            //声明thread
            Registration registration = new Registration(nodeName);

            //使用线程池调度
            ExecutorService pool = Executors.newFixedThreadPool(1);
            pool.execute(registration);
        };
    }

    /**
     * route api
     * @return
     */
    @Bean
    RouteAPI routeAPI() {
        return Feign.builder()
                .encoder(new JacksonEncoder())
                .decoder(new JacksonDecoder())
                .options(new Request.Options(1000, 3500))
                .retryer(new Retryer.Default(5000, 5000, 3))
                .target(RouteAPI.class, appConfig.getRouteUrl());
    }

    /**
     * 本地缓存
     * @return
     */
    @Bean(name = "localSessionStore")
    public LoadingCache<String, Session> loadingCache() {
        return CacheBuilder.newBuilder()
                .maximumSize(10000)
//                .expireAfterAccess()
//                .expireAfterWrite()
                .build(new CacheLoader<String, Session>() {
                    @Override
                    public Session load(String s) throws Exception {
                        return null;
                    }
                });
    }


    /**
     * session store
     * @return
     */
    @Bean
    @SneakyThrows
    SessionHolder memorySessionStore() {
        return new InMemorySessionHolder();
    }
}
