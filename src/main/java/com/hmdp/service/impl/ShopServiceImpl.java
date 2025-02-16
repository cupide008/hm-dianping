package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
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


    // 根据id查询店铺信息
    @Override
    public Result queryById(Long id) {

        // 缓存穿透解决方案
//        Shop shop = queryWithThough(id);
        Shop shop = queryWithMutex(id);
        if (shop == null) {
            return Result.fail("店铺不存在");
        }
        // 6.返回数据
        return Result.ok(shop);
    }


    // 更新商铺
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

    public Shop queryWithThough(Long id){
        String key = RedisConstants.CACHE_SHOP_KEY;

        // 1.从redis中获取数据
        String shopJson = stringRedisTemplate.opsForValue().get(key);

        // 2.如果redis中有数据，直接返回
        if (StrUtil.isNotBlank(shopJson)) {
            return JSONUtil.toBean(shopJson, Shop.class);
        }

        // 命中redis中的空值，返回错误
        if (shopJson != null) {
            return null;
        }

        // 3.如果redis中没有数据，从数据库中获取数据
        Shop shop = getById(id);

        // 4.如果数据库中没有数据，返回失败
        if (shop == null) {
            // 将空值写入redis中
            stringRedisTemplate.opsForValue().set(key+id,"",RedisConstants.CACHE_SHOP_NULL_EXPIRE,TimeUnit.MINUTES);
            return null;
        }

        // 5.存在，将数据写入redis
        stringRedisTemplate.opsForValue().set(key+id,shop.toString(),RedisConstants.CACHE_SHOP_EXPIRE, TimeUnit.MINUTES);

        // 6.返回数据
        return shop;
    }

    public Shop queryWithMutex(Long id) {
        String key = RedisConstants.CACHE_SHOP_KEY;

        // 1.从redis中获取数据
        String shopJson = stringRedisTemplate.opsForValue().get(key);

        // 2.如果redis中有数据，直接返回
        if (StrUtil.isNotBlank(shopJson)) {
            return JSONUtil.toBean(shopJson, Shop.class);
        }

        // 命中redis中的空值，返回错误
        if (shopJson != null) {
            return null;
        }

        String LockKey = "lock:shop:" + id;


        // 缓存重建
        // 1.获取互斥锁
        boolean isLock = tryLock(LockKey);
        Shop shop = null;
        try {
            if (!isLock) {
                // 获取锁失败，休眠等待重试
                Thread.sleep(50);
                return queryWithMutex(id);
            }
            // 3.如果redis中没有数据，从数据库中获取数据
            shop = getById(id);

            // 4.如果数据库中没有数据，返回失败
            if (shop == null) {
                // 将空值写入redis中
                stringRedisTemplate.opsForValue().set(key + id, "", RedisConstants.CACHE_SHOP_NULL_EXPIRE, TimeUnit.MINUTES);
                return null;
            }

            // 5.存在，将数据写入redis
            stringRedisTemplate.opsForValue().set(key + id, shop.toString(), RedisConstants.CACHE_SHOP_EXPIRE, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.fillInStackTrace();
        } finally {
            // 释放锁
            unLock(LockKey);
        }

        // 6.返回数据
        return shop;
    }

    // 获取锁
    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.MINUTES);
        return BooleanUtil.isTrue(flag);
    }


    // 释放锁
    private void unLock(String key){
        stringRedisTemplate.delete(key);
    }
}
