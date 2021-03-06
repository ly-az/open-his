package xyz.ly11.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import xyz.ly11.constants.Constants;
import xyz.ly11.domain.User;
import xyz.ly11.dto.UserDTO;
import xyz.ly11.mapper.RoleMapper;
import xyz.ly11.mapper.UserMapper;
import xyz.ly11.service.UserService;
import xyz.ly11.utils.AppMd5Utils;
import xyz.ly11.vo.DataGridView;

import java.util.Arrays;
import java.util.List;

/**
 * @author by 29794
 * @date 2020/10/9 2:00
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    private final RoleMapper roleMapper;


    @Override
    public User queryUserByPhone(String phone) {
        QueryWrapper<User> qw = new QueryWrapper<>();
        qw.eq(User.COL_PHONE, phone);
        return this.userMapper.selectOne(qw);
    }

    @Override
    public User getOne(Long userId) {
        return this.userMapper.selectById(userId);
    }

    @Override
    public DataGridView listUserForPage(UserDTO userDTO) {
        // 构建分页条件
        Page<User> page = new Page<>(userDTO.getPageNum(), userDTO.getPageSize());
        // 构建查询条件
        QueryWrapper<User> qw = new QueryWrapper<>();
        qw.like(StringUtils.isNotBlank(userDTO.getUserName()), User.COL_USER_NAME, userDTO.getUserName());
        qw.like(StringUtils.isNotBlank(userDTO.getPhone()), User.COL_PHONE, userDTO.getPhone());
        qw.eq(StringUtils.isNotBlank(userDTO.getStatus()), User.COL_STATUS, userDTO.getStatus());
        qw.eq(userDTO.getDeptId() != null, User.COL_DEPT_ID, userDTO.getDeptId());
        qw.ge(null != userDTO.getBeginTime(), User.COL_CREATE_TIME, userDTO.getBeginTime());
        qw.le(null != userDTO.getEndTime(), User.COL_CREATE_TIME, userDTO.getEndTime());
        qw.orderByAsc(User.COL_USER_ID);
        this.userMapper.selectPage(page, qw);
        return new DataGridView(page.getTotal(), page.getRecords());

    }

    @Override
    public int addUser(UserDTO userDTO) {
        User user = new User();
        BeanUtil.copyProperties(userDTO, user);
        // 设置用户类型
        user.setUserType(Constants.USER_NORMAL);
        // 设置默认密码
        String defaultPwd = user.getPhone().substring(5);
        user.setCreateBy(userDTO.getSimpleUser().getUserName());
        user.setCreateTime(DateUtil.date());
        user.setSalt(AppMd5Utils.createSalt());
        // md5 加盐加密（如果不想自己实现也可以使用HuTool工具类）
        user.setPassword(AppMd5Utils.md5(defaultPwd, user.getSalt(), 2));
        return this.userMapper.insert(user);
    }

    @Override
    public int updateUser(UserDTO userDTO) {
        User user = this.userMapper.selectById(userDTO.getUserId());
        if (null == user) {
            return 0;
        }
        BeanUtil.copyProperties(userDTO, user);
        // 设置修改人
        user.setUpdateBy(userDTO.getSimpleUser().getUserName());
        return this.userMapper.updateById(user);
    }

    @Override
    public int deleteUserByIds(Long[] userIds) {
        List<Long> ids = Arrays.asList(userIds);
        //根据用户IDS删除sys_role_user里面的关联数据（实际开发需要做逻辑删除的）
        this.roleMapper.deleteRoleUserByUserIds(ids);
        return this.userMapper.deleteBatchIds(ids);
    }

    @Override
    public List<User> getAllUsers() {
        // g构建查询条件
        QueryWrapper<User> qw = new QueryWrapper<>();
        qw.eq(User.COL_STATUS, Constants.STATUS_TRUE);
        qw.eq(User.COL_USER_TYPE, Constants.USER_NORMAL);
        qw.orderByAsc(User.COL_USER_ID);
        return this.userMapper.selectList(qw);
    }

    @Override
    public void resetPassWord(Long[] userIds) {
        Arrays.asList(userIds).forEach(userId -> {
            // 这里没有判空是因为数据正常进来是正确的
            User user = this.userMapper.selectById(userId);
            String defaultPwd = "";
            // 超级管理猿的密码不是手机号码的后六位
            if (user.getUserType().equals(Constants.USER_ADMIN)) {
                defaultPwd = "123456";
            } else {
                defaultPwd = user.getPhone().substring(5);
            }
            user.setSalt(AppMd5Utils.createSalt());
            user.setPassword(AppMd5Utils.md5(defaultPwd, user.getSalt(), 2));
            this.userMapper.updateById(user);
        });
    }

    @Override
    public List<User> querySchedulingUsers(Long userId, Long deptId) {
        QueryWrapper<User> qw = new QueryWrapper<>();
        qw.eq(null != deptId, User.COL_DEPT_ID, deptId);
        qw.eq(null != userId, User.COL_USER_ID, userId);
        qw.eq(User.COL_SCHEDULING_FLAG, Constants.SCHEDULING_FLAG_TRUE);
        return this.userMapper.selectList(qw);
    }
}