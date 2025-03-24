package newOs.kernel.memory.virtual.replacement;

import newOs.kernel.memory.virtual.paging.Page;
import newOs.kernel.memory.virtual.paging.PageFrame;

import java.util.List;
import java.util.Optional;

/**
 * 页面置换策略接口
 * 定义不同的页面置换算法接口规范
 */
public interface PageReplacementStrategy {

    /**
     * 页面置换算法类型枚举
     */
    enum ReplacementType {
        /**
         * 最近最少使用算法
         */
        LRU,
        
        /**
         * 先进先出算法
         */
        FIFO,
        
        /**
         * 最不常用算法
         */
        LFU,
        
        /**
         * 时钟算法
         */
        CLOCK,
        
        /**
         * 改进的时钟算法
         */
        ENHANCED_CLOCK,
        
        /**
         * 最优页面置换算法（理论算法）
         */
        OPTIMAL,
        
        /**
         * 随机页面置换算法
         */
        RANDOM
    }
    
    /**
     * 获取页面置换策略类型
     * @return 策略类型
     */
    ReplacementType getType();
    
    /**
     * 选择要被置换的页面
     * @param candidatePages 可供置换的页面列表
     * @param processPid 进程ID
     * @return 被选中置换的页面，若没有可置换页面则返回空
     */
    Optional<Page> selectVictimPage(List<Page> candidatePages, int processPid);
    
    /**
     * 记录页面被访问
     * @param page 被访问的页面
     * @param isWrite 是否为写操作
     */
    void recordPageAccess(Page page, boolean isWrite);
    
    /**
     * 记录页面被分配
     * @param page 被分配的页面
     * @param frame 分配的页帧
     */
    void recordPageAllocation(Page page, PageFrame frame);
    
    /**
     * 记录页面被换出
     * @param page 被换出的页面
     */
    void recordPageEviction(Page page);
    
    /**
     * 获取所有当前正在使用的页面列表
     * @param processPid 进程ID，如果为负数则返回所有进程的页面
     * @return 页面列表
     */
    List<Page> getAllPages(int processPid);
    
    /**
     * 获取特定进程的页面命中率
     * @param processPid 进程ID
     * @return 页面命中率（0-1.0之间的值）
     */
    double getHitRate(int processPid);
    
    /**
     * 获取特定进程的页面置换计数
     * @param processPid 进程ID
     * @return 置换次数
     */
    int getEvictionCount(int processPid);
    
    /**
     * 初始化置换策略
     * @param maxPages 最大页面数
     */
    void initialize(int maxPages);
    
    /**
     * 重置置换策略统计信息
     * @param processPid 进程ID，如果为负数则重置所有进程的统计信息
     */
    void resetStats(int processPid);
    
    /**
     * 获取策略状态信息
     * @return 状态信息字符串
     */
    String getStatsInfo();
    
    /**
     * 判断页面是否应该被置换（例如检查是否为锁定页面）
     * @param page 要判断的页面
     * @return 是否可以被置换
     */
    boolean isPageReplaceable(Page page);
    
    /**
     * 更新页面置换策略相关参数
     * @param paramName 参数名
     * @param value 参数值
     * @return 是否成功更新
     */
    boolean updateParameter(String paramName, Object value);
    
    /**
     * 检查页面访问模式，可用于自适应置换策略
     * @param processPid 进程ID
     * @return 访问模式描述
     */
    String analyzeAccessPattern(int processPid);
} 