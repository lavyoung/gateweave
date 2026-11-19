package com.lavyoung.gateweave.center.shared.pagination;

/**
 * 表示控制面应用查询使用的分页参数。
 *
 * <p>页码统一从 1 开始，每页最多读取 100 条记录。该值对象集中维护分页边界和
 * 数据库偏移量计算，业务查询只保留自身筛选条件。</p>
 *
 * @param pageNumber 页码，从 1 开始
 * @param pageSize   每页数量
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/19
 */
public record PageRequest(int pageNumber, int pageSize) {

    private static final int MAX_PAGE_SIZE = 100;

    /**
     * 校验控制面统一分页边界。
     *
     * @throws IllegalArgumentException 页码小于 1，或每页数量不在 1 至 100 之间时抛出
     */
    public PageRequest {
        if (pageNumber < 1) {
            throw new IllegalArgumentException("pageNumber must be greater than zero");
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("pageSize must be between 1 and 100");
        }
    }

    /**
     * 计算数据库分页查询使用的行偏移量。
     *
     * @return 从零开始的数据库行偏移量
     */
    public long offset() {
        return (long) (this.pageNumber - 1) * this.pageSize;
    }
}
