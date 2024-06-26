package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.enumeration.OperationType;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface SetmealMapper {

    /**
     * 根据分类id查询套餐的数量
     * @param id
     * @return
     */
    @Select("select count(id) from setmeal where category_id = #{categoryId}")
    Integer countByCategoryId(Long id);
    /**
     * 新增套餐
     * @param setmeal
     */

    @AutoFill(value = OperationType.INSERT)
    @Insert("INSERT INTO setmeal (category_id, name, price, status, description, image, create_time, update_time, create_user, update_user) " +
            "VALUES (#{categoryId}, #{name}, #{price}, #{status}, #{description}, #{image}, #{createTime}, #{updateTime}, #{createUser}, #{updateUser})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    Integer addMeal(Setmeal setmeal);

    /**
     * 分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    Page<SetmealVO> pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    /**
     * 通过id查询套餐
     * @param id
     * @return
     */
    @Select("select * from setmeal where id=#{id};")
    Setmeal getMealById(Long id);

    /**
     * 通过套餐id修改套餐属性
     * @param setmeal

     */
    @AutoFill(value = OperationType.UPDATE)
    @Update("UPDATE setmeal " +
            "SET category_id = #{categoryId}, " +
            "name = #{name}, " +
            "price = #{price}, " +
            "status = #{status}, " +
            "description = #{description}, " +
            "image = #{image}, " +
            "update_time = #{updateTime}, " +
            "update_user = #{updateUser} " +
            "WHERE id = #{id}")
    void upMeal(Setmeal setmeal);

    /**
     * 删除套餐表数据
     * @param ids
     */
    void deleteBatch(List<Long> ids);

    /**
     * 根据id修改套餐
     *
     * @param setmeal
     */
    @AutoFill(OperationType.UPDATE)
    void update(Setmeal setmeal);

    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    List<Setmeal> list(Setmeal setmeal);


    /**
     * 根据套餐id查询菜品选项
     * @param setmealId
     */
    @Select("select sd.name, sd.copies, d.image, d.description " +
            "from setmeal_dish sd left join dish d on sd.dish_id = d.id " +
            "where sd.setmeal_id = #{setmealId}")
    List<DishItemVO> getDishItemBySetmealId(Long setmealId);



    /**
     * 根据条件统计套餐数量
     * @param map
     * @return
     */
    Integer countByMap(Map map);
}
