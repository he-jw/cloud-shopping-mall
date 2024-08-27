# 云购商城
## 项目介绍：
基于Spring Cloud、MyBatis Plus、RabbitMQ、Seata、Sentinel和Elasticsearch的微服务电商商城系统，用户可以在商城内搜索和浏览商品。
在用户登录后，可以将商品添加到购物车，随后确认订单并完成支付，然后查看支付结果。
## 技术亮点：
1.使用Nacos+openFeign组件实现微服务之间的远程调用，方便微服务根据并发需求扩展多实例部署。

2.使用ElasticSearch取代了在Mysql中对商品的模糊搜索，加快了搜索商品的速度，并对商品实现增删改时，采用RabbitMQ异步通知，保证mysql和ES的数据同步。

3.使用Sentinel实现了请求限流、线程隔离、服务熔断，提高服务的健壮性。
