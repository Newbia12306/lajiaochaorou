package com.example.demo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import com.example.demo.repository.DishRepository;
import com.example.demo.model.Dish;
import static org.mockito.Mockito.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class DishServiceTest {

    @Mock
    private DishRepository dishRepository;

    @InjectMocks
    private DishService dishService;

    private Dish testDish;

    @BeforeEach
    void setUp(){
        testDish = new Dish();
        testDish.setId(1);
        testDish.setName("辣椒炒肉");
        testDish.setSpicy(Dish.SpicyLevel.MEDIUM_SPICY);
        testDish.setIsSignature(true);
        testDish.setIsDeleted(false);
    }



    @Test
    void testNormalName(){
        assertDoesNotThrow(() -> dishService.validateDishName("辣椒炒肉"));
    }
    @Test
    void testEmptyName() {
        assertThrows(IllegalArgumentException.class,
                () -> dishService.validateDishName(null));
    }

    @Test
     void testShortName() {
        assertThrows(IllegalArgumentException.class,
                () -> dishService.validateDishName("测试"));
    }

    @Test
    void testNumberName() {
        assertThrows(IllegalArgumentException.class,
                () -> dishService.validateDishName("麻辣香锅123"));
    }
    @Test
    void testCreateDish_true() {
        when(dishRepository.existsByName("test")).thenReturn(false);
        dishService.createDish("test", Dish.SpicyLevel.MEDIUM_SPICY, true);
        verify(dishRepository).save(any(Dish.class));
    }

    @Test
    void testCreateDish_false() {
        when(dishRepository.existsByName("test")).thenReturn(true);
        assertThrows(IllegalArgumentException.class,
                () -> dishService.createDish("test", Dish.SpicyLevel.MEDIUM_SPICY, true));
    }
    @Test
    void testGetAllDishes(){
        List<Dish> dishes = Arrays.asList(testDish,new Dish());
        when(dishRepository.findByIsDeletedFalse()).thenReturn(dishes);

        List<Dish> result = dishService.getAllDishes();

        assertEquals(dishes.size(),result.size());
        verify(dishRepository).findByIsDeletedFalse();
    }
    @Test
    void testDeleteDish(){
        when(dishRepository.findById(1L)).thenReturn(Optional.of(testDish));
        when(dishRepository.save(any(Dish.class))).thenReturn(testDish);

        dishService.deleteDish(1L);

        assertTrue(testDish.getIsDeleted());
        verify(dishRepository).findById(1L);
        verify(dishRepository).save(testDish);
    }

    @Test
    void testDeleteDishes(){
        List<Long> ids = Arrays.asList(1L, 2L);
        when(dishRepository.findById(anyLong())).thenReturn(Optional.of(testDish));

        dishService.deleteDishes(ids);

        verify(dishRepository, times(ids.size())).findById(anyLong());
        verify(dishRepository, times(ids.size())).save(any(Dish.class));
    }

    @Test
    void testGetDishById(){
        when(dishRepository.findById(1L)).thenReturn(Optional.of(testDish));

        Dish result = dishService.getDishById(1L);
        assertEquals("辣椒炒肉",result.getName());
        verify(dishRepository).findById(1L);
    }

    @Test
    void testUpdateDish(){
        Dish update = new Dish();
        update.setName("辣椒炒鸡蛋");
        update.setSpicy(Dish.SpicyLevel.EXTRA_SPICY);
        update.setIsSignature(false);

        when(dishRepository.findById(1L)).thenReturn(Optional.of(testDish));
        when(dishRepository.existsByName("辣椒炒鸡蛋")).thenReturn(false);
        when(dishRepository.save(any(Dish.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Dish result = dishService.updateDish(1L, update);

        assertEquals("辣椒炒鸡蛋",result.getName());
        assertEquals(Dish.SpicyLevel.EXTRA_SPICY,result.getSpicy());
        assertFalse(result.getIsSignature());
        verify(dishRepository).save(testDish);
    }
    @Test
    void testGetDishesWithPagination() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Dish> dishes = Arrays.asList(testDish);
        Page<Dish> dishPage = new PageImpl<>(dishes, pageable, 1);


        when(dishRepository.findAllOrderBySpicyLevel(pageable)).thenReturn(dishPage);

        Map<String, Object> result = dishService.getDishesWithPagination("spicy", 1, 10);

        assertEquals(1,result.get("currentPage"));
        assertEquals(1,result.get("totalPages"));
        assertEquals(1,((List)result.get("dishes")).size());
        verify(dishRepository).findAllOrderBySpicyLevel(pageable);
    }






}
