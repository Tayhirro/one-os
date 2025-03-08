package newOs.component.cpu.cache.CacheImpl;


import newOs.component.cpu.cache.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

//@Component
//public class RedisCache implements Cache {
////
////    @Autowired
////    private RedisTemplate<String, Object> redisTemplate;
////
////    @Value("${cache.l1.namespace}")
////    private String namespace;
////
////    @Override
////    public Object get(String key) {
////        return redisTemplate.opsForValue().get(namespace + key);
////    }
////
////    @Override
////    public void put(String key, Object value, long ttl) {
////        redisTemplate.opsForValue().set(namespace + key, value, ttl, TimeUnit.MILLISECONDS);
////    }
////
////    @Override
////    public void invalidate(String key) {
////        redisTemplate.delete(namespace + key);
////    }
//}