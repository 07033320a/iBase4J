/**
 * 
 */
package org.ibase4j.web;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.ibase4j.model.SysMenu;
import org.ibase4j.model.SysUser;
import org.ibase4j.service.ISysAuthorizeService;
import org.ibase4j.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.plugins.Page;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import top.ibase4j.core.base.BaseController;
import top.ibase4j.core.support.Assert;
import top.ibase4j.core.support.HttpCode;
import top.ibase4j.core.util.SecurityUtil;
import top.ibase4j.core.util.UploadUtil;
import top.ibase4j.core.util.WebUtil;

/**
 * 用户管理控制器
 * 
 * @author ShenHuaJie
 * @version 2016年5月20日 下午3:12:12
 */
@RestController
@Api(value = "用户管理", description = "用户管理")
@RequestMapping(value = "/user")
public class SysUserController extends BaseController {
	@Autowired
	private ISysUserService sysUserService;
	@Autowired
	private ISysAuthorizeService authorizeService;

	@PostMapping
	@ApiOperation(value = "修改用户信息")
	@RequiresPermissions("sys.base.user.update")
	public Object update(ModelMap modelMap, @RequestBody SysUser sysUser) {
		Assert.isNotBlank(sysUser.getAccount(), "ACCOUNT");
		Assert.length(sysUser.getAccount(), 3, 15, "ACCOUNT");
		sysUserService.update(sysUser);
		return setSuccessModelMap(modelMap);
	}

	@ApiOperation(value = "修改个人信息")
	@RequiresPermissions("sys.base.user.update")
	@PostMapping(value = "/update/person")
	public Object updatePerson(ModelMap modelMap, @RequestBody SysUser sysUser) {
		sysUser.setId(WebUtil.getCurrentUser());
		Assert.isNotBlank(sysUser.getAccount(), "ACCOUNT");
		Assert.length(sysUser.getAccount(), 3, 15, "ACCOUNT");
		sysUserService.update(sysUser);
		return setSuccessModelMap(modelMap);
	}

	@ApiOperation(value = "修改用户头像")
	@RequiresPermissions("sys.base.user.update")
	@PostMapping(value = "/update/avatar")
	public Object updateAvatar(HttpServletRequest request, ModelMap modelMap) {
		List<String> fileNames = UploadUtil.uploadImage(request, false);
		if (fileNames.size() > 0) {
			SysUser sysUser = new SysUser();
			sysUser.setId(WebUtil.getCurrentUser());
			String filePath = UploadUtil.getUploadDir(request) + fileNames.get(0);
			// String avatar = UploadUtil.remove2DFS("sysUser", "user" +
			// sysUser.getId(), filePath).getRemotePath();
			// String avatar = UploadUtil.remove2Sftp(filePath, "user" +
			// sysUser.getId());
			sysUser.setAvatar(filePath);
			sysUserService.update(sysUser);
			return setSuccessModelMap(modelMap);
		} else {
			setModelMap(modelMap, HttpCode.BAD_REQUEST);
			modelMap.put("msg", "请选择要上传的文件！");
			return modelMap;
		}
	}

	// 修改密码
	@ApiOperation(value = "修改密码")
	@RequiresPermissions("sys.base.user.update")
	@PostMapping(value = "/update/password")
	public Object updatePassword(ModelMap modelMap, @RequestBody SysUser param) {
		Assert.isNotBlank(param.getOldPassword(), "OLDPASSWORD");
		Assert.isNotBlank(param.getPassword(), "PASSWORD");
		Long userId = getCurrUser();
		String encryptPassword = SecurityUtil.encryptPassword(param.getOldPassword());
		SysUser sysUser = sysUserService.queryById(userId);
		Assert.notNull(sysUser, "USER", param.getId());
		if (!sysUser.getPassword().equals(encryptPassword)) {
			throw new UnauthorizedException("原密码错误.");
		}
		sysUser.setPassword(SecurityUtil.encryptPassword(param.getPassword()));
		sysUserService.update(sysUser);
		return setSuccessModelMap(modelMap);
	}

	// 查询用户
	@ApiOperation(value = "查询用户")
	@RequiresPermissions("sys.base.user.read")
	@PutMapping(value = "/read/list")
	public Object get(ModelMap modelMap, @RequestBody Map<String, Object> sysUser) {
		Page<?> list = sysUserService.query(sysUser);
		return setSuccessModelMap(modelMap, list);
	}

	// 用户详细信息
	@ApiOperation(value = "用户详细信息")
	@RequiresPermissions("sys.base.user.read")
	@PutMapping(value = "/read/detail")
	public Object detail(ModelMap modelMap, @RequestBody SysUser sysUser) {
		sysUser = sysUserService.queryById(sysUser.getId());
		if (sysUser != null) {
			sysUser.setPassword(null);
		}
		return setSuccessModelMap(modelMap, sysUser);
	}

	// 用户详细信息
	@ApiOperation(value = "删除用户")
	@RequiresPermissions("sys.base.user.delete")
	@DeleteMapping
	public Object delete(ModelMap modelMap, @RequestBody SysUser sysUser) {
		sysUserService.delete(sysUser.getId());
		return setSuccessModelMap(modelMap);
	}

	// 当前用户
	@ApiOperation(value = "当前用户信息")
	@GetMapping(value = "/read/promission")
	public Object promission(ModelMap modelMap) {
		Long id = getCurrUser();
		SysUser sysUser = sysUserService.queryById(id);
		if (sysUser != null) {
			sysUser.setPassword(null);
		}
		List<SysMenu> menus = authorizeService.queryAuthorizeByUserId(id);
		modelMap.put("user", sysUser);
		modelMap.put("menus", menus);
		return setSuccessModelMap(modelMap);
	}

	// 当前用户
	@ApiOperation(value = "当前用户信息")
	@GetMapping(value = "/read/current")
	public Object current(ModelMap modelMap) {
		Long id = getCurrUser();
		SysUser sysUser = sysUserService.queryById(id);
		if (sysUser != null) {
			sysUser.setPassword(null);
		}
		return setSuccessModelMap(modelMap, sysUser);
	}
}
