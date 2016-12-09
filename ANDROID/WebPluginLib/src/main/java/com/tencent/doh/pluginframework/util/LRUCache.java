package com.tencent.doh.pluginframework.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用于缓存图片和音频的地址
 */
public class LRUCache<K, V> extends LinkedHashMap<K, V> {

    private final int MAX_CACHE_SIZE;

    /**
     * @param cacheSize 最大容量
     */
    public LRUCache(int cacheSize) {
        super((int) Math.ceil(cacheSize / 0.75) + 1, 0.75f, true);//accessOrder = true;按访问顺序排列
        MAX_CACHE_SIZE = cacheSize;
    }

    /**
     * 维护的元素大于指定容量时,返回true,并删除最老元素
     * @param eldest
     * @return
     */
    @Override
    protected boolean removeEldestEntry(Map.Entry eldest) {
        return super.size() > MAX_CACHE_SIZE;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<K, V> entry : entrySet()) {
            sb.append(String.format("%s:%s ", entry.getKey(), entry.getValue()));
        }
        return sb.toString();
    }
}