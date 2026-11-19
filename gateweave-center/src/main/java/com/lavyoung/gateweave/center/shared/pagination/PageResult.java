package com.lavyoung.gateweave.center.shared.pagination;

import java.util.List;
import java.util.Objects;

/**
 * 表示控制面应用查询返回的不可变分页结果。
 *
 * <p>分页结果只承载当前页内容和数量信息，不携带 HTTP、数据库或具体业务能力的技术对象。</p>
 *
 * @param content       当前页内容
 * @param pageRequest   当前分页参数
 * @param totalElements 符合查询条件的记录总数
 * @param <T>           当前页元素类型
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/19
 */
public record PageResult<T>(List<T> content, PageRequest pageRequest, long totalElements) {

    /**
     * 建立当前页内容的不可变快照并校验分页结果。
     *
     * @throws NullPointerException     content、pageRequest 为 null，或 content 包含 null 元素时抛出
     * @throws IllegalArgumentException 总数为负数、当前页内容超过页大小或超过总数时抛出
     */
    public PageResult {
        Objects.requireNonNull(content, "content must not be null");
        Objects.requireNonNull(pageRequest, "pageRequest must not be null");
        content = List.copyOf(content);

        if (totalElements < 0) {
            throw new IllegalArgumentException("totalElements must not be negative");
        }
        if (content.size() > pageRequest.pageSize()) {
            throw new IllegalArgumentException("content size must not exceed pageSize");
        }
        if (content.size() > totalElements) {
            throw new IllegalArgumentException("content size must not exceed totalElements");
        }
    }

    /**
     * 计算总页数。
     *
     * @return 总页数，没有数据时返回 0
     */
    public long totalPages() {
        if (this.totalElements == 0) {
            return 0;
        }
        return (this.totalElements - 1) / this.pageRequest.pageSize() + 1;
    }
}
