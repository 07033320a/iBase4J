package org.ibase4j.service.impl;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.ibase4j.mapper.SysUserMapper;
import org.ibase4j.mapper.SysUserMenuMapper;
import org.ibase4j.mapper.SysUserThirdpartyMapper;
import org.ibase4j.model.SysUser;
import org.ibase4j.model.SysUserThirdparty;
import org.ibase4j.service.ISysDeptService;
import org.ibase4j.service.ISysDicService;
import org.ibase4j.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.dubbo.config.annotation.Service;
import com.baomidou.mybatisplus.plugins.Page;
import com.weibo.api.motan.config.springsupport.annotation.MotanService;

import top.ibase4j.core.base.BaseService;
import top.ibase4j.core.support.login.ThirdPartyUser;
import top.ibase4j.core.util.CacheUtil;
import top.ibase4j.core.util.SecurityUtil;

/**
 * SysUser服务实现类
 * 
 * @author ShenHuaJie
 * @version 2016-08-27 22:39:42
 */
@CacheConfig(cacheNames = "SysUser")
@Service(interfaceClass = ISysUserService.class)
@MotanService(interfaceClass = ISysUserService.class)
public class SysUserServiceImpl extends BaseService<SysUser> implements ISysUserService {
	@Autowired
	private SysUserThirdpartyMapper thirdpartyMapper;
	@Autowired
	private ISysDicService sysDicService;
	@Autowired
	private ISysDeptService sysDeptService;
	@Autowired
	private SysUserMenuMapper sysUserMenuMapper;

	public Page<SysUser> query(Map<String, Object> params) {
		Map<String, String> userTypeMap = sysDicService.queryDicByDicIndexKey("USERTYPE");
		Page<SysUser> pageInfo = super.query(params);
		for (SysUser userBean : pageInfo.getRecords()) {
			if (userBean.getUserType() != null) {
				userBean.setUserTypeText(userTypeMap.get(userBean.getUserType().toString()));
			}
			if (userBean.getDeptId() != null) {
				userBean.setDeptName(sysDeptService.queryById(userBean.getDeptId()).getDeptName());
			}
			List<String> permissions = sysUserMenuMapper.queryPermission(userBean.getId());
			for (String permission : permissions) {
				if (StringUtils.isBlank(userBean.getPermission())) {
					userBean.setPermission(permission);
				} else {
					userBean.setPermission(userBean.getPermission() + ";" + permission);
				}
			}
		}
		return pageInfo;
	}

	/** 查询第三方帐号用户Id */
	@Cacheable
	public Long queryUserIdByThirdParty(ThirdPartyUser thirdPartyUser) {
		return thirdpartyMapper.queryUserIdByThirdParty(thirdPartyUser.getProvider(), thirdPartyUser.getOpenid());
	}

	// 加密
	public String encryptPassword(String password) {
		return SecurityUtil.encryptMd5(SecurityUtil.encryptSHA(password));
	}

	/** 保存第三方帐号 */
	@Transactional
	public SysUser insertThirdPartyUser(ThirdPartyUser thirdPartyUser) {
		SysUser sysUser = new SysUser();
		sysUser.setSex(0);
		sysUser.setUserType("1");
		sysUser.setPassword(this.encryptPassword("123456"));
		sysUser.setUserName(thirdPartyUser.getUserName());
		sysUser.setAvatar(thirdPartyUser.getAvatarUrl());
		// 初始化第三方信息
		SysUserThirdparty thirdparty = new SysUserThirdparty();
		thirdparty.setOpenId(thirdPartyUser.getOpenid());
		thirdparty.setCreateTime(new Date());

		this.update(sysUser);
		thirdparty.setUserId(sysUser.getId());
		thirdpartyMapper.insert(thirdparty);
		return sysUser;
	}

	public void init() {
		List<Long> list = ((SysUserMapper) mapper).selectIdPage(Collections.<String, Object>emptyMap());
		for (Long id : list) {
			CacheUtil.getCache().set(getCacheKey(id), mapper.selectById(id));
		}
	}
}
