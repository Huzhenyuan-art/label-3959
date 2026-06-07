package com.example.demo.util;

import com.example.demo.entity.OrderItem;
import com.example.demo.mapper.OrderItemMapper;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderItemBatchUtil {

    private final SqlSessionFactory sqlSessionFactory;

    public OrderItemBatchUtil(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    public void batchInsert(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        try (SqlSession session = sqlSessionFactory.openSession(ExecutorType.BATCH, false)) {
            OrderItemMapper mapper = session.getMapper(OrderItemMapper.class);
            for (OrderItem item : items) {
                mapper.insert(item);
            }
            session.commit();
            session.flushStatements();
        }
    }
}
