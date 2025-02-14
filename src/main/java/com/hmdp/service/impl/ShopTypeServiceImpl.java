package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryList() {

        String key = RedisConstants.CACHE_SHOP_TYPE_KEY;
        // 1.从redis中获取数据
        String shopTypeJson = stringRedisTemplate.opsForValue().get(key);

        // 2.如果redis中有数据，直接返回
        if (StrUtil.isNotBlank(shopTypeJson)) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                List<ShopType> typeList = objectMapper
                        // 将 JSON 字符串（content）反序列化为指定类型的 Java 对象（valueType）
                        .readValue(shopTypeJson,
                                objectMapper
                                        // 构建一个集合类型，用于反序列化为集合对象
                                        .getTypeFactory()
                                        .constructCollectionType(List.class, ShopType.class));
                return Result.ok(typeList);
            } catch (JsonProcessingException e) {
                e.fillInStackTrace();
                return Result.fail("商铺类型解析失败");
            }
        }
        // 3.如果redis中没有数据，从数据库中获取数据
         List<ShopType> typeList = query().orderByAsc("sort").list();

        // 4.如果数据库中没有数据，返回失败
        if (typeList == null) {
            return Result.fail("商铺类型不存在");
        }

        // 5.存在，将数据写入redis
        try {
            stringRedisTemplate.opsForValue().set(key,
                    // 将传入的对象（value）转换为 JSON 格式的字符串
                    new ObjectMapper().writeValueAsString(typeList));
        } catch (JsonProcessingException e) {
            e.fillInStackTrace();
            return Result.fail("存入redis失败");
        }
        // 5.返回数据
        return Result.ok(typeList);
    }
}
