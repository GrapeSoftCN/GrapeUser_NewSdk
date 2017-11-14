package interfaceApplication;

import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import JGrapeSystem.jGrapeFW_Message;
import JGrapeSystem.rMsg;
import Model.UserModel;
import apps.appsProxy;
import authority.plvDef.UserMode;
import authority.plvDef.plvType;
import interfaceModel.GrapeDBSpecField;
import interfaceModel.GrapeTreeDBModel;
import nlogger.nlogger;
import session.session;
import string.StringHelper;

public class roles {
    private UserModel model;
	private GrapeTreeDBModel role;
	private GrapeDBSpecField gDbSpecField;
	private session se;
	private JSONObject userInfo = null;
	private String currentWeb = null;
	 private Integer userType = null;

	public roles() {
	    model = new UserModel();
	    
		role = new GrapeTreeDBModel();
		gDbSpecField = new GrapeDBSpecField();
		gDbSpecField.importDescription(appsProxy.tableConfig("roles"));
		role.descriptionModel(gDbSpecField);
		role.bindApp();

		se = new session();
		userInfo = se.getDatas();
		if (userInfo != null && userInfo.size() != 0) {
			currentWeb = userInfo.getString("currentWeb"); // 当前用户所属网站id
			//currentWeb = "1";
			userType =userInfo.getInt("userType");
		}
	}

	/**
	 * 新增角色
	 * 
	 * @param roleInfo
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public String RoleInsert(String roleInfo) {
		Object obj = null;
		String result = rMsg.netMSG(100, "新增角色失败");
		JSONObject object = JSONObject.toJSON(roleInfo);
		try {
			if (object == null || object.size() <= 0) {
				return rMsg.netMSG(1, "非法参数");
			}
			if (!object.containsKey("wbid") || (object.containsKey("wbid") && object.getString("wbid").equals(""))) {
				object.put("wbid", currentWeb);
			}
			if (!findByName(object.getString("name"))) {
				JSONObject rMode = new JSONObject(plvType.chkType, plvType.powerVal).puts(plvType.chkVal, 500);//设置默认查询权限
		    	JSONObject uMode = new JSONObject(plvType.chkType, plvType.powerVal).puts(plvType.chkVal, 200);
		    	JSONObject dMode = new JSONObject(plvType.chkType, plvType.powerVal).puts(plvType.chkVal, 300);
		    	object.put("rMode", rMode.toJSONString()); //添加默认查看权限
		    	object.put("uMode", uMode.toJSONString()); //添加默认修改权限
		    	object.put("dMode", dMode.toJSONString()); //添加默认删除权限
				obj = role.data(object).autoComplete().insertOnce();
			}
		} catch (Exception e) {
			nlogger.logout(e);
			obj = null;
		}
		return (obj != null) ? rMsg.netMSG(0, "新增成功") : result;
	}

	/**
	 * 修改角色
	 * 
	 * @param id
	 * @param roleInfo
	 * @return
	 */
	public String RoleUpdate(String id, String roleInfo) {
		String result = rMsg.netMSG(100, "修改失败");
		JSONObject object = JSONObject.toJSON(roleInfo);
		if (object == null || object.size() <= 0) {
			return rMsg.netMSG(1, "非法参数");
		}
		role.enableCheck();
		object = role.eq("id", id).data(object).update();
		return result = (object != null) ? rMsg.netMSG(0, "修改成功") : result;
	}
	
	/**
	 * 角色信息搜索
	 * 
	 * @param roleInfo
	 * @return
	 */
	public String RoleSearch(String roleInfo) {
		JSONArray array = null;
		JSONArray condArray = null;
		if (roleInfo != null && !roleInfo.equals("") && !roleInfo.equals("null")) {
			JSONObject object = JSONObject.toJSON(roleInfo);
			condArray = model.buildCond(object);
			condArray = (condArray == null || condArray.size() <= 0) ? JSONArray.toJSONArray(roleInfo) : condArray;
			if (condArray != null && condArray.size() > 0) {
				role.enableCheck();
				array = role.where(condArray).select();
			}
		}
		return rMsg.netMSG(true, ((array != null && array.size() > 0) ? array : new JSONArray()));
	}

	/**
	 * 删除
	 * 
	 * @param id
	 * @return
	 */
	public String RoleDelete(String ids) {
	    String[] value = null;
        String result = rMsg.netMSG(100, "删除失败");
        if (!StringHelper.InvaildString(ids)) {
            value = ids.split(",");
        }
        if (value != null) {
            role.or();
            for (String id : value) {
                if (!StringHelper.InvaildString(id) && ObjectId.isValid(id)) {
                    role.eq("_id", id);
                }
            }
            role.enableCheck();//开启权限检查
            long code = role.deleteAll();
            result = (code >= 0) ? rMsg.netMSG(0, "删除成功") : result;
        }
        return result;
	}

	/**
	 * 分页
	 * 
	 * @param idx
	 * @param pageSize
	 * @return
	 */
	public String RolePage(int idx, int pageSize) {
		return RolePageBy(idx, pageSize, null);
	}

	public String RolePageBy(int idx, int pageSize, String roleInfo) {
		long total = 0;
		JSONArray array = null;
		JSONArray condArray = null;
		System.out.println(role.page(1, 2));
		role.enableCheck();//开启权限检查
		//判断当前用户身份：系统管理员，网站管理员
    	if (UserMode.root>userType && userType>= UserMode.admin) { //判断是否是网站管理员
    		role.eq("wbid", currentWeb);
		}
		if (roleInfo != null && !roleInfo.equals("") && !roleInfo.equals("null")) {
			
			JSONObject object = JSONObject.toJSON(roleInfo);
			condArray = model.buildCondAddWbid(object, currentWeb);
			condArray = (condArray != null && condArray.size() > 0) ? condArray : JSONArray.toJSONArray(roleInfo);
			if (condArray == null || condArray.size() <= 0) {
				return rMsg.netPAGE(idx, pageSize, total, new JSONArray());
			}else{
				role.where(condArray);
			}
				
		}
		array = role.dirty().page(idx, pageSize);
		total = role.count();
		return rMsg.netPAGE(idx, pageSize, total, array);
	}

	/**
	 * 获取角色信息,用于登录成功之后显示角色信息
	 * @param ugid
	 * @return
	 */
	protected JSONObject getRole(String ugid) {
		JSONObject object = null;
		if (StringHelper.InvaildString(ugid)) {
			object = role.eq("_id", ugid).field("name,userType").find();
		}
		return object;
	}

	/**
	 * 验证角色是否存在
	 * 
	 * @param name
	 * @return
	 */
	private boolean findByName(String name) {
		JSONObject object = null;
		object = new JSONObject();
		object = role.eq("name", name).eq("wbid", currentWeb).find();
		return (object != null && object.size() > 0);
	}
	
}
