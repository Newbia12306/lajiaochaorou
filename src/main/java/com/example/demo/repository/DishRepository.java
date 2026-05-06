package com.example.demo.repository;

import com.example.demo.model.Dish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
public interface DishRepository extends JpaRepository<Dish,Long> {
    boolean existsByName(String name);

    @Query("SELECT d FROM Dish d ORDER BY " +
            "CASE  WHEN d.spicy = 'EXTRA_SPICY' THEN 1 " +
            "WHEN d.spicy = 'MEDIUM_SPICY' THEN 2" +
            "WHEN d.spicy = 'NOT_SPICY' THEN 3" +
            "ELSE 4 END,d.name ASC")
    Page<Dish> findAllOrderBySpicyLevel(Pageable pageable);

    @Query("SELECT d FROM Dish d ORDER BY d.isSignature DESC,d.name ASC")
    Page<Dish>findAllOrderBySignature(Pageable pageable);

    List<Dish> findByIsDeletedFalse();

}




