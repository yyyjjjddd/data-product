package com.example.metrics.mapper;

import com.example.metrics.model.entity.Asset;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 素材数据访问层
 *
 * <p>提供素材数据的数据库操作：
 * <ul>
 *   <li>selectAll: 查询所有素材</li>
 *   <li>selectById: 根据ID查询素材</li>
 *   <li>selectByPagination: 分页查询素材</li>
 *   <li>count: 统计素材总数</li>
 * </ul>
 *
 * @see com.example.metrics.model.entity.Asset
 */
@Mapper
public interface AssetMapper {

    /**
     * 查询所有素材
     *
     * @return 素材列表，按上传时间倒序
     */
    List<Asset> selectAll();

    /**
     * 根据素材ID查询
     *
     * @param assetId 素材ID
     * @return 素材详情，不存在返回null
     */
    Asset selectById(@Param("assetId") String assetId);

    /**
     * 统计素材总数
     *
     * @return 素材数量
     */
    long count();

    /**
     * 分页查询素材
     *
     * @param offset 偏移量
     * @param limit  返回数量
     * @return 素材列表
     */
    List<Asset> selectByPagination(@Param("offset") int offset, @Param("limit") int limit);
}

