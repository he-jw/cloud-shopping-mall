package com.hmall.item.controller;


import cn.hutool.core.thread.ThreadUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.api.dto.ItemDTO;
import com.hmall.api.dto.OrderDetailDTO;
import com.hmall.common.domain.PageDTO;
import com.hmall.common.domain.PageQuery;
import com.hmall.common.utils.BeanUtils;
import com.hmall.item.domain.po.Item;
import com.hmall.item.service.IItemService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "商品管理相关接口")
@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
@Slf4j
public class ItemController {

    private final RabbitTemplate rabbitTemplate;

    private final IItemService itemService;

    @ApiOperation("分页查询商品")
    @GetMapping("/page")
    public PageDTO<ItemDTO> queryItemByPage(PageQuery query) {
        // 1.分页查询
        Page<Item> result = itemService.page(query.toMpPage("update_time", false));
        // 2.封装并返回
        return PageDTO.of(result, ItemDTO.class);
    }

    @ApiOperation("根据id批量查询商品")
    @GetMapping
    public List<ItemDTO> queryItemByIds(@RequestParam("ids") List<Long> ids){
        ThreadUtil.sleep(500);
        return itemService.queryItemByIds(ids);
    }

    @ApiOperation("根据id查询商品")
    @GetMapping("{id}")
    public ItemDTO queryItemById(@PathVariable("id") Long id) {
        return BeanUtils.copyBean(itemService.getById(id), ItemDTO.class);
    }

    @ApiOperation("新增商品")
    @PostMapping
    public void saveItem(@RequestBody ItemDTO itemDTO) {
        // 新增
        Item item = BeanUtils.copyBean(itemDTO, Item.class);
        itemService.save(item);
        // 同步es数据库中数据更新
        try {
            rabbitTemplate.convertAndSend("items.direct", "add.items", item);
        } catch (Exception e) {
            log.error("同步es数据库中数据更新的消息发送失败{}", e);
        }
    }

    @ApiOperation("更新商品状态")
    @PutMapping("/status/{id}/{status}")
    public void updateItemStatus(@PathVariable("id") Long id, @PathVariable("status") Integer status){
        Item item = new Item();
        item.setId(id);
        item.setStatus(status);
        itemService.updateById(item);
    }

    @ApiOperation("更新商品")
    @PutMapping
    public void updateItem(@RequestBody ItemDTO itemDTO) {
        // 不允许修改商品状态，所以强制设置为null，更新时，就会忽略该字段
        itemDTO.setStatus(null);
        // 更新
        Item item = BeanUtils.copyBean(itemDTO, Item.class);
        itemService.updateById(BeanUtils.copyBean(item, Item.class));
        // 同步es数据库中数据更新
        try {
            rabbitTemplate.convertAndSend("items.direct", "add.items", item);
        } catch (Exception e) {
            log.error("同步es数据库中数据更新的消息发送失败{}", e);
        }
    }

    @ApiOperation("根据id删除商品")
    @DeleteMapping("{id}")
    public void deleteItemById(@PathVariable("id") Long id) {
        itemService.removeById(id);
        // 同步es数据库中数据更新
        try {
            rabbitTemplate.convertAndSend("items.direct", "delete.items", id);
        } catch (Exception e) {
            log.error("同步es数据库中数据更新的消息发送失败{}", e);
        }
    }

    @ApiOperation("批量扣减库存")
    @PutMapping("/stock/deduct")
    public void deductStock(@RequestBody List<OrderDetailDTO> items){
        itemService.deductStock(items);
    }
}
