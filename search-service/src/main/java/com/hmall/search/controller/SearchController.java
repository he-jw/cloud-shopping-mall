package com.hmall.search.controller;

import cn.hutool.json.JSONUtil;
import com.hmall.common.domain.PageDTO;
import com.hmall.search.domain.po.ItemDoc;
import com.hmall.search.domain.query.ItemPageQuery;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Api(tags = "搜索相关接口")
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    @Autowired
    private RestHighLevelClient client;

    @ApiOperation("搜索商品")
    @GetMapping("/list")
    public PageDTO<ItemDoc> search(ItemPageQuery query) throws IOException {
        // 1.创建Request
        SearchRequest request = new SearchRequest("items");
        // 2.组织请求参数
        // 2.1.准备bool查询
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        // 2.2.关键字搜索
        if (StringUtils.hasText(query.getKey())){
            bool.must(QueryBuilders.matchQuery("name", query.getKey()));
        }
        if (StringUtils.hasText(query.getBrand())){
            // 2.3.品牌过滤
            bool.filter(QueryBuilders.termQuery("brand", query.getBrand()));
        }
        if (StringUtils.hasText(query.getCategory())){
            // 2.3.品牌过滤
            bool.filter(QueryBuilders.termQuery("category", query.getCategory()));
        }
        if (query.getMinPrice()!=null){
            // 2.4.价格过滤
            bool.filter(QueryBuilders.rangeQuery("price").gte(query.getMinPrice()));
        }
        if (query.getMaxPrice()!=null){
            // 2.4.价格过滤
            bool.filter(QueryBuilders.rangeQuery("price").lte(query.getMaxPrice()));
        }
        request.source().query(bool);

        //分页查询
        request.source().from((query.getPageNo() - 1) * query.getPageSize()).size(query.getPageSize());
        // 3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        // 4.解析响应
        return handleResponse(response,query);
        // 分页查询
        // Page<Item> result = itemService.lambdaQuery()
        //         .like(StrUtil.isNotBlank(query.getKey()), Item::getName, query.getKey())
        //         .eq(StrUtil.isNotBlank(query.getBrand()), Item::getBrand, query.getBrand())
        //         .eq(StrUtil.isNotBlank(query.getCategory()), Item::getCategory, query.getCategory())
        //         .eq(Item::getStatus, 1)
        //         .between(query.getMaxPrice() != null, Item::getPrice, query.getMinPrice(), query.getMaxPrice())
        //         .page(query.toMpPage("update_time", false));
        // // 封装并返回
    }

    private PageDTO<ItemDoc> handleResponse(SearchResponse response,ItemPageQuery query) {
        PageDTO<ItemDoc> itemDocPageDTO = new PageDTO<>();
        List<ItemDoc> itemDocList = new ArrayList();
        SearchHits searchHits = response.getHits();
        // 1.获取总条数
        long total = searchHits.getTotalHits().value;
        System.out.println("共搜索到" + total + "条数据");
        itemDocPageDTO.setTotal(total);
        // 2.遍历结果数组
        SearchHit[] hits = searchHits.getHits();
        for (SearchHit hit : hits) {
            // 3.得到_source，也就是原始json文档
            String source = hit.getSourceAsString();
            // 4.反序列化
            ItemDoc item = JSONUtil.toBean(source, ItemDoc.class);
            itemDocList.add(item);
            // // 5.获取高亮结果
            // Map<String, HighlightField> hfs = hit.getHighlightFields();
            // if (CollUtils.isNotEmpty(hfs)) {
            //     // 5.1.有高亮结果，获取name的高亮结果
            //     HighlightField hf = hfs.get("name");
            //     if (hf != null) {
            //         // 5.2.获取第一个高亮结果片段，就是商品名称的高亮值
            //         String hfName = hf.getFragments()[0].string();
            //         item.setName(hfName);
            //     }
            // }
            // System.out.println(item);
        }
        itemDocPageDTO.setList(itemDocList);
        itemDocPageDTO.setPages(query.getPageSize().longValue());
        return itemDocPageDTO;
    }
}
