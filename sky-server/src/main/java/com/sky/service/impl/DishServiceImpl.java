package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Autowired
    private SetmealMapper  setmealMapper;
    /**
     * 新增菜品
     */
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        //向菜品表dish插入1条数据
        dishMapper.insert(dish);

        //获取菜品的主键值
        Long dishId = dish.getId();

        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors != null && flavors.size() > 0){
            //向口味表dish_flavor插入n条
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishId);
            });
            //批量插入
            dishFlavorMapper.insertBatch(flavors);
        }
    }


    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(),dishPageQueryDTO.getPageSize());
        // 此处page为github包下的page
        Page<DishVO> page=dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(),page.getResult());
    }

    /**
     * 菜品批量删除
     * @param ids
     */

    //lambda  表达式的写法
    // ids.forEach(id->{
    //     Dish dish = dishMapper.getById(id);
    //     //判断当前要删除的菜品状态是否为起售中
    //     if(dish.getStatus() == StatusConstant.ENABLE){
    //         //如果是起售中，抛出业务异常
    //         throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
    //
    //     }
    // });
    public void deleteBatch(List<Long> ids) {
        /* 先满足业务条件 */
        /* 判断当前菜品是否在起售中 */

        log.info("开始执行删除");
        for (Long id : ids) {
            Dish dish =dishMapper.getById(id);
            log.info("正在判断菜品是否起售");
            // 菜品是起售时候抛出业务异常
            if (dish.getStatus()== StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }

        //判断当前要删除的菜品是否被套餐关联了
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        /* 判断关联条件 */
        if(setmealIds != null && setmealIds.size() > 0){
            log.info("正在判断菜品是否关联着套餐");
            //如果关联了，抛出业务异常
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        //删除菜品表中的数据
        //这样写sql查询不够精简，可以考虑使用拼接来进行删除，因为使用了for循环，所以是一条一条进行删除
        // ids.forEach(id -> {
        //     log.info("正在删除菜品");
        //     dishMapper.deleteById(id);
        //     //删除口味表中的数据
        //     log.info("正在删除口味");
        //     dishFlavorMapper.deleteByDishId(id);
        // });

        //根据菜品id来进行批量删除菜品数据和关联的口味数据
        dishMapper.deleteByIds(ids);
        //sql :delete from dish_flavor where  dish_id in (?,?,?) 会拼接成这样
        dishFlavorMapper.deleteByDishIds(ids);
    }


    /**
     * 根据id查询菜品和口味数据
     * @param id
     * @return
     */

    public DishVO getByIdWithFlavor(long id) {
        // 根据id查询菜品数据
        Dish dish=dishMapper.getById(id);
        //根据菜品id查询口味数据 一个菜品可对应多个属性
        List<DishFlavor> dishFlavors= dishFlavorMapper.getByDishId(id);
        //将查询到的数据封装到vo

        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish,dishVO);
        dishVO.setFlavors(dishFlavors);
        return dishVO;
    }

    /**
     * 根据id修改菜品和关联的口味
     * @param dishDTO
     */
    @Transactional
    public void updateWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);

        //修改菜品表dish，执行update操作
        dishMapper.update(dish);
        /* 不然要一个一个比对。然后插入数据 */
        //删除当前菜品关联的口味数据，操作dish_flavor，执行delete操作
        dishFlavorMapper.deleteByDishId(dishDTO.getId());

        //插入最新的口味数据，操作dish_flavor，执行insert操作
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors != null && flavors.size() > 0){
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishDTO.getId());
            });
            dishFlavorMapper.insertBatch(flavors);
        }
    }



    @Override
    public List<Dish> list(Long categoryId) {
        Dish dish =Dish.builder()
                .categoryId(categoryId)
                .status(StatusConstant.ENABLE)
                .build();
        return dishMapper.list(dish);
    }

    /**
     * 菜品起售停售
     * @param status
     * @param id
     */
    @Transactional
    public void startOrStop(Integer status, Long id) {
        Dish dish = Dish.builder()
                .id(id)
                .status(status)
                .build();
        dishMapper.update(dish);

        if (status == StatusConstant.DISABLE) {
            // 如果是停售操作，还需要将包含当前菜品的套餐也停售
            List<Long> dishIds = new ArrayList<>();
            dishIds.add(id);
            // select setmeal_id from setmeal_dish where dish_id in (?,?,?)
            List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(dishIds);
            if (setmealIds != null && setmealIds.size() > 0) {
                for (Long setmealId : setmealIds) {
                    Setmeal setmeal = Setmeal.builder()
                            .id(setmealId)
                            .status(StatusConstant.DISABLE)
                            .build();
                    setmealMapper.update(setmeal);
                }
            }
        }
    }


    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish) {
        List<Dish> dishList = dishMapper.list(dish);

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }
}
