package com.example.demo.repository;

import com.example.demo.DTO.FrequentDishStats;
import com.example.demo.model.OrderRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRecordRepository extends JpaRepository<OrderRecord, Long> {

    /** 按用户ID查询所有订单记录 */
    Page<OrderRecord> findByUserIdOrderByOrderedAtDesc(String userId, Pageable pageable);

    /** 按订单编号查询 */
    List<OrderRecord> findByOrderId(String orderId);

    /** 查询用户最常点的菜品（按点单次数降序） */
    @Query("SELECT new com.example.demo.DTO.FrequentDishStats(" +
           "o.dishId, d.name, COUNT(o.dishId), MAX(o.orderedAt)) " +
           "FROM OrderRecord o " +
           "JOIN Dish d ON o.dishId = d.id " +
           "WHERE o.userId = :userId " +
           "GROUP BY o.dishId, d.name " +
           "ORDER BY COUNT(o.dishId) DESC")
    List<FrequentDishStats> findFrequentDishesByUserId(@Param("userId") String userId, Pageable pageable);
}
