package newOs.component.cpu.cache;


public interface Cache {
    Object get(String key);         // 获取缓存
    void put(String key, Object value, long ttl); // 支持TTL
    void invalidate(String key);
}