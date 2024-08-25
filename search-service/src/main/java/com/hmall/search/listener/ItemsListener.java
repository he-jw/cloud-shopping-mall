package com.hmall.search.listener;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.hmall.search.domain.po.Item;
import com.hmall.search.domain.po.ItemDoc;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ItemsListener {

    @Autowired
    private RestHighLevelClient client;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "add.items.queue", durable = "true"),
            exchange = @Exchange(name = "items.direct",type = ExchangeTypes.DIRECT),
            key = "add.items"
    ))
    public void listenAddItems(Item item) throws IOException {
        // 非空校验
        if (BeanUtil.isEmpty(item)) {
            return;
        }
        // 1.创建Request
        BulkRequest request = new BulkRequest("items");
        // 2.1.转换为文档类型ItemDTO
        ItemDoc itemDoc = BeanUtil.copyProperties(item, ItemDoc.class);
        // 2.2.创建新增文档的Request对象
        request.add(new IndexRequest()
                .id(itemDoc.getId())
                .source(JSONUtil.toJsonStr(itemDoc), XContentType.JSON));
        // 3.发送请求
        client.bulk(request, RequestOptions.DEFAULT);
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "delete.items.queue", durable = "true"),
            exchange = @Exchange(name = "items.direct",type = ExchangeTypes.DIRECT),
            key = "delete.items"
    ))
    public void listenDeleteItems(Long itemId) throws IOException {
        // 非空校验
        if (BeanUtil.isEmpty(itemId)) {
            return;
        }
        // 1.准备Request，两个参数，第一个是索引库名，第二个是文档id
        DeleteRequest request = new DeleteRequest("items", itemId.toString());
        // 2.发送请求
        client.delete(request, RequestOptions.DEFAULT);
    }

}
