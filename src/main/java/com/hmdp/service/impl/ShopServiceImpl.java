package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {


    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryById(long id) {
        String key = RedisConstants.CACHE_SHOP_KEY;

        // 1.从redis中获取数据
        String shopJson = stringRedisTemplate.opsForValue().get(key);

        // 2.如果redis中有数据，直接返回
        if (StrUtil.isNotBlank(shopJson)) {
            return Result.ok(shopJson);
        }

        // 命中redis中的空值，返回错误
        if (shopJson != null) {
            return Result.fail("店铺不存在");
        }

        // 3.如果redis中没有数据，从数据库中获取数据
        Shop shop = getById(id);

        // 4.如果数据库中没有数据，返回失败
        if (shop == null) {
            // 将空值写入redis中
            stringRedisTemplate.opsForValue().set(key+id,"",RedisConstants.CACHE_SHOP_NULL_EXPIRE,TimeUnit.MINUTES);
            return Result.fail("商铺不存在");
        }

        // 5.存在，将数据写入redis
        stringRedisTemplate.opsForValue().set(key+id,shop.toString(),RedisConstants.CACHE_SHOP_EXPIRE, TimeUnit.MINUTES);

        // 6.返回数据
        return Result.ok(shop);
    }

    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺不能为空！");
        }
        // 1.更新数据库
        updateById(shop);
        // 2.删除redis缓存
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY+id);
        return Result.ok();
    }
}
