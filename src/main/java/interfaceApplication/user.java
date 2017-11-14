package interfaceApplication;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64;
import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import JGrapeSystem.rMsg;
import Model.UserModel;
import apps.appIns;
import apps.appsProxy;
import authority.plvDef.UserMode;
import authority.plvDef.plvType;
import cache.CacheHelper;
import check.checkHelper;
import checkCode.checkCodeHelper;
import checkCode.imageCheckCode;
import interfaceModel.GrapeDBSpecField;
import interfaceModel.GrapeTreeDBModel;
import json.JSONHelper;
import security.codec;
import session.session;
import sms.ruoyaMASDB;
import string.StringHelper;

public class user {
    private UserModel model;
    private GrapeTreeDBModel users;
    private GrapeDBSpecField gDbSpecField;
    private session se;
    private CacheHelper caches = null;
    private JSONObject usersInfo = null;
    private String currentWeb = null;
    private String GrapeSid = null;
    private String userName = null;
    private String userId = null;
    private Integer userType = null;

    public user() {
        model = new UserModel();

        users = new GrapeTreeDBModel();
        gDbSpecField = new GrapeDBSpecField();
        gDbSpecField.importDescription(appsProxy.tableConfig("user"));
        users.descriptionModel(gDbSpecField);
        users.bindApp();
        

        caches = new CacheHelper();
        se = new session();
        usersInfo = se.getDatas();
        GrapeSid = session.getSID();
        if (usersInfo != null && usersInfo.size() != 0) {
            currentWeb = usersInfo.getString("currentWeb"); // 当前用户所属网站id
            userName = usersInfo.getString("name"); // 当前用户姓名
            userId = usersInfo.getString("id"); // 当前用户用户名
            userType = usersInfo.getInt("userType");
        }
    }

    /**
     * 发送验证码至手机，默认6位
     * 
     * @param phone
     *            手机号
     * @param len
     *            验证码长度 长度不为数字，或者长度小于等于0，默认为6位
     * @return
     */
    public String getVerifyCode(String phone, int len) {
        len = (len <= 0) ? 6 : len;
        String Checkcode = checkCodeHelper.getCheckCode(phone, len);
        String code = ruoyaMASDB.sendSMS(phone, "验证码为：" + Checkcode + "有效时间为30秒，请在有效时间内输入验证码");
        return (code != null) ? rMsg.netMSG(0, "验证码发送成功") : rMsg.netMSG(100, "发送失败");
    }

    /**
     * 验证手机验证码
     * 
     * @param phone
     * @param checkCode
     * @return
     */
    public String checkVerifyCode(String phone, String checkCode) {
        boolean flag = checkCodeHelper.checkCode(phone, checkCode);
        return rMsg.netMSG(flag, "验证成功");
    }

    /**
     * 生成图片验证码
     * 
     * @return
     */
    public String getImageCode(int len) {
        len = (len <= 0) ? 6 : len;
        String code = checkCodeHelper.generateVerifyCode(len);
        caches.setget(code.toLowerCase(), code);
        byte[] image = imageCheckCode.getCodeimage(code);
        return "data:image/jpeg;base64," + Base64.encodeBase64String(image);
    }

    /**
     * 验证图片验证码
     * 
     * @param code
     * @return
     */
    public String checkImageCode(String code) {
        int tip = 100;
        code = code.toLowerCase();
        if (StringHelper.InvaildString(code) && (caches.get(code) != null)) {
            caches.delete(code);
            tip = 0;
        }
        return tip == 0 ? rMsg.netMSG(0, "验证成功") : rMsg.netMSG(100, "验证失败");
    }

    /**
     * 用户注册
     * 
     * @param info
     * @return
     */
    public String userRegister(String info) {
        JSONObject object = null;
        JSONObject infos =JSONObject.toJSON(info);
        JSONObject rMode = new JSONObject(plvType.chkType, plvType.powerVal).puts(plvType.chkVal, 100);//设置默认查询权限
    	JSONObject uMode = new JSONObject(plvType.chkType, plvType.powerVal).puts(plvType.chkVal, 200);
    	JSONObject dMode = new JSONObject(plvType.chkType, plvType.powerVal).puts(plvType.chkVal, 300);
    	infos.put("rMode", rMode.toJSONString()); //添加默认查看权限
    	infos.put("uMode", uMode.toJSONString()); //添加默认修改权限
    	infos.put("dMode", dMode.toJSONString()); //添加默认删除权限
    	String infoa = JSONObject.toJSONString(infos);
    	info = CheckParam(infoa);
        if (StringHelper.InvaildString(info)) {
            if (info.contains("errorcode")) {
                return info;
            }
            String id = users.dataEx(info).autoComplete().insertOnce().toString();
            object = find(id);
            AddLog(1, id, "userRegister", "");
        }
        return (object != null) ? rMsg.netMSG(0, object) : rMsg.netMSG(100, "注册失败");
    }

    /**
     * 用户登录
     * 
     * @param info
     * @return
     */
    public String userLogin(String info) {
        JSONArray arrays = null;
        String usersname = "";
        String password = "";
        String _checkField = "";
        String field = "password";
        info = CheckLogin(info);
        JSONObject object = JSONObject.toJSON(info);
        if (object == null || object.size() <= 0) {
            return rMsg.netMSG(100, "登录失败");
        }
        if (object.containsKey("errorcode")) {
            return info;
        }
        int mode = 0;
        if (object.containsKey("loginmode")) {
            mode = Integer.parseInt(object.getString("loginmode"));
        }
        usersname = object.getString("usersname");
        password = object.getString("password");
        switch (mode) {
        case 0:
            _checkField = "id";
            break;
        case 1:
            _checkField = "email";
            break;
        case 2:
            _checkField = "mobphone";
            break;
        case 3:
        case 4:
            _checkField = "IDcard";
            field = "name";
            break;
        }
        if (field.equals("password")) {
            password = codec.md5(password);
        }
        if (_checkField.equals("IDcard")) {
            users.eq(_checkField, (StringHelper.InvaildString(usersname)) ? usersname : null);
        } else {
            users.eq(_checkField, usersname);
        }
        users.eq(field, password);
        arrays = users.select();
        return returnLogin(usersname, arrays, mode);
    }

    /**
     * 退出系统
     * 
     * @param usersName
     * @return
     */
    public String userLogout() {
        String result = rMsg.netMSG(100, "该用户已退出");
        if (StringHelper.InvaildString(GrapeSid)) {
            se.deleteSession();
            return rMsg.netMSG(0, "退出成功");
        }
        return result;
    }

    /**
     * 根据不同的登录模式修改当前用户修改密码
     * 
     * @param usersName
     *            登录名
     * @param oldPW
     *            原密码
     * @param newPW
     *            新密码
     * @param loginmode
     *            登录模式
     * @return
     */
    @SuppressWarnings("unchecked")
    public String userChangePW(String usersName, String oldPW, String newPW, int loginmode) {
        JSONObject object = null;
        String result = rMsg.netMSG(100, "密码修改失败");
        String _checkField = "";
        switch (loginmode) {
        case 0:
            _checkField = "id";
            break;
        case 1:
            _checkField = "email";
            break;
        case 2:
            _checkField = "mobphone";
            break;
        case 3:
        case 4:
            _checkField = "name";
            break;
        }

        if (checkusers(_checkField, usersName, oldPW)) {
            return rMsg.netMSG(17, "原密码错误");
        }
        JSONObject obj = new JSONObject();
        obj.put("password", codec.md5(newPW));
        object = users.eq(_checkField, usersName).eq("password", codec.md5(oldPW)).data(obj).update();
        result = (object != null && object.size() > 0) ? rMsg.netMSG(0, "密码修改成功") : result;
        return result;
    }

    /**
     * 管理员重置用户密码
     * 
     * @param uid
     *            null：重置所有用户密码，不为null：重置指定用户密码
     * @param newPwd
     * @return
     */
    public String resetPwd(String uid, String newPwd) {
        String[] value = null;
        long code = 0;
        String result = rMsg.netMSG(18, "您当前没有权限重置密码");
        if (true) { // 当前用户为系统管理员
            if (StringHelper.InvaildString(uid)) { // uid不为空，重置指定用户密码
                value = uid.split(",");
            }
            if (value != null) {
                users.or();
                for (String id : value) {
                    users.eq("_id", id);
                }
            }
            // 重置所有用户密码
            JSONObject obj = new JSONObject("password", codec.md5(newPwd));
            code = users.data(obj).updateAll();
            result = code > 0 ? rMsg.netMSG(0, "密码重置成功") : rMsg.netMSG(100, "密码重置失败");
            if (code > 0) {
                // 操作成功，添加操作日志
                AddLog(StringHelper.InvaildString(uid) ? 5 : 6, uid, "resetPwd", "");
            }
        }
        return result;
    }

    /**
     * 编辑用户信息
     * 
     * @param id
     * @param usersInfo
     * @return
     */
    public String userEdit(String id, String usersInfo) {
        usersInfo = CheckParam(usersInfo);
        JSONObject temp = null;
        if (StringHelper.InvaildString(usersInfo)) {
            if (usersInfo.contains("errorcode")) {
                return usersInfo;
            }
            JSONObject object = JSONObject.toJSON(usersInfo);
            if (object != null && object.size() > 0) {
                if (object.containsKey("password")) {
                    object.remove("password");
                }
                temp = users.eq("_id", id).data(object).update();
            }
        }
        if (temp != null) {
            AddLog(3, id, "userEdit", "");
        }
        return (temp != null) ? rMsg.netMSG(0, "修改成功") : rMsg.netMSG(100, "修改失败");
    }

    /**
     * 查询用户信息
     * 
     * @param usersinfo
     * @return
     */
    public String userSearch(String usersinfo) {
        JSONArray array = null;
        JSONObject object = JSONObject.toJSON(usersinfo);
        JSONArray condArray = model.buildCond(object);
        condArray = (condArray == null || condArray.size() <= 0) ? JSONArray.toJSONArray(usersinfo) : condArray;
        if (condArray != null && condArray.size() > 0) {
            array = users.where(condArray).select();
        }
        return rMsg.netMSG(true, (array != null && array.size() > 0) ? array : new JSONArray());
    }

    /**
     * id查询
     * 
     * @param id
     * @return
     */
    public String userFind(String id) {
        JSONObject object = users.eq("id", id).find();
        return rMsg.netMSG(0, (object != null && object.size() > 0) ? object.toJSONString() : new JSONObject().toJSONString());
    }

    /* ------------------------ 分页 -------------------- */
    public String userPageFront(String wbid, int idx, int pageSize) {
        AddLog(7, "", "userPageFront", null);
        return Page(wbid, idx, pageSize, null);
    }

    public String userPageByFront(String wbid, int idx, int pageSize, String usersinfo) {
        AddLog(7, "", "userPageByFront", usersinfo);
        return Page(null, idx, pageSize, usersinfo);
    }

    public String userPage(int idx, int pageSize) {
        AddLog(7, "", "userPage", null);
        return Page(null, idx, pageSize, null);
    }

    public String userPageBy(int idx, int pageSize, String usersinfo) {
        AddLog(7, "", "userPageBy", usersinfo);
        return Page(null, idx, pageSize, usersinfo);
    }

    /**
     * 分页操作
     * @param wbid
     * @param idx
     * @param pageSize
     * @param usersinfo
     * @return
     */
    private String Page(String wbid, int idx, int pageSize, String usersinfo) {
        long total = 0;
        JSONArray array = null;
    	if (UserMode.root>userType && userType>= UserMode.admin) { //判断是否是网站管理员
    		wbid = currentWeb;
            users.eq("deleteable", 0).eq("visable", 0);
		}
    	if (UserMode.root<=userType) { //判断是否是系统管理员
    		wbid = null;
		}
        if ((wbid != null) && (!wbid.equals(""))) {
            String[] value = model.getAllWeb(wbid); // 获取下级所有网站，包含本站
            if (value != null && !value.equals("")) {
                users.or();
                for (String wid : value) {
                    users.eq("wbid", wid);
                }
            }
        }
        if (usersinfo != null) {
            JSONArray condarray = JSONArray.toJSONArray(usersinfo);
            if ((condarray != null) && (condarray.size() != 0)) {
                users.where(condarray);
            } else {
                return rMsg.netPAGE(idx, pageSize, total, new JSONArray());
            }
        }
        array = users.dirty().mask("password,IDcard").page(idx, pageSize);
        total = users.count();
        return rMsg.netPAGE(idx, pageSize, total, array);
    }

    /**
     * 删除用户
     * 
     * @param id
     * @return
     */
    public String userDelete(String id) {
        String[] value = null;
        users.or();
        if (StringHelper.InvaildString(id)) {
            value = id.split("");
        }
        if (value != null) {
            for (String string : value) {
                if (ObjectId.isValid(string)) {
                    users.eq("_id", string);
                }
            }
        }
        
        long code = users.deleteAll();
        code = Integer.parseInt(String.valueOf(code)) == value.length ? 0 : 99;
        if (code > 0) {
            AddLog(2, id, "userDelete", "");
        }
        return code == 0 ? rMsg.netMSG(0, "删除成功") : rMsg.netMSG(99, "删除失败");
    }

    /**
     * 获取水印图片
     * 
     * @return 返回图片的base64
     */
    public String getuserImage() {
        String name = "";
        String _id = "";
        String IDcard = "";
        JSONObject obj;
        List<String> data = new ArrayList<>();
        if ((this.usersInfo != null) && (this.usersInfo.size() != 0)) {
            name = usersInfo.getString("name");
            _id = usersInfo.getMongoID("_id");
            obj = users.eq("_id", _id).field("IDcard").find();
            if (obj != null && obj.size() != 0) {
                IDcard = obj.getString("IDcard");
            }
        }
        data.add(name);
        data.add(IDcard);
        return imageCheckCode.CreateTextWaterMark(data);
    }

    /**
     * 设置审核权限
     * 
     * @param uid
     *            用户id，即_id
     * @return
     */
    public String setReviewer(String uid) {
        String result = rMsg.netMSG(100, "设置失败");
        if (!StringHelper.InvaildString(uid)) {
            return rMsg.netMSG(19, "非法参数");
        }
        JSONObject object = Reviewer(uid, 1);
        return (object != null) ? rMsg.netMSG(0, "设置成功") : result;
    }

    /**
     * 取消审核权
     * 
     * @param uid
     * @param isreview
     * @return
     */
    public String cancelReviewer(String uid) {
        String result = rMsg.netMSG(100, "取消审核权失败");
        if (!StringHelper.InvaildString(uid)) {
            return rMsg.netMSG(19, "非法参数");
        }
        JSONObject object = Reviewer(uid, 1);
        return (object != null) ? rMsg.netMSG(0, "取消审核权成功") : result;
    }

    /**
     * 审核设置
     * 
     * @param uid
     * @param state
     * @return
     */
    private JSONObject Reviewer(String uid, int state) {
        JSONObject object = new JSONObject("isreview", state);
        object = users.eq("_id", uid).data(object).update();
        return object;
    }

    /**
     * 用户信息验证
     * 
     * @param field
     * @param value
     * @param pw
     * @return
     */
    private boolean checkusers(String field, String value, String pw) {
        JSONObject object = null;
        object = users.eq(field, value).eq("password", codec.md5(pw)).find();
        return (object == null || object.size() <= 0);
    }

    /**
     * 登录返回值
     * 
     * @param array
     * @return
     */
    @SuppressWarnings("unchecked")
    private String returnLogin(String username, JSONArray array, int mode) {
        JSONObject object = null;
        JSONArray arrays = null;
        if (array != null && array.size() > 0) {
            object = getWbid(array);
            if (object != null && object.size() != 0) {
            	String roleName = "";
            	int userType = 0;
                String wbid = object.get("wbid").toString(); // 获取站点id
                String ugid = object.get("ugid").toString(); // 获取角色信息
                JSONObject roleObj = getRoleName(ugid);
                if (roleObj != null && roleObj.size() != 0) {
                	roleName = roleObj.getString("name");
                	userType = roleObj.getInt("userType");
				}
                arrays = getWbID(wbid); // 获取网站信息,同时返回当前网站包含的下级网站信息
                object.remove("wbid");
                object.remove("password");
                object.remove("IDcard");
                wbid = (StringHelper.InvaildString(wbid)) ? wbid.split(",")[0] : "";
                object.put("currentWeb", wbid);
                object.put("webinfo", (arrays != null && arrays.size() > 0) ? arrays : new JSONArray());
                object.put("rolename", roleName); // 获取角色信息
                object.put("userType", userType); // 获取用户身份
                String sid = session.createSession(username, object, 86400)._getSID();
                object.put("sid", sid);
                // 添加登陆日志
                AddLog(2, "", "userLogin", "");
                // 登录次数统计,添加本次登录时间【时间戳】
            }
        }
        return result(object, mode);
    }

    /**
     * 显示角色名称
     * 
     * @param roleId
     * @return
     */
    @SuppressWarnings("unchecked")
	private JSONObject getRoleName(String roleId) {
        JSONObject object = new roles().getRole(roleId);
        if (object != null && object.size() > 0) {
            if (!object.containsKey("name")) {
				object.put("name", "");
			}
            if (!object.containsKey("userType")) {
				object.put("userType", 0);
			}
        }
        return object;
    }

    /**
     * 合并当前用户所属的所有网站id
     * 
     * @param array
     * @return
     */
    @SuppressWarnings("unchecked")
    private JSONObject getWbid(JSONArray array) {
        JSONObject robj = new JSONObject();
        String wbid = "", temp;
        if (array != null && array.size() != 0) {
            for (Object object : array) {
                robj = (JSONObject) object;
                temp = robj.getString("wbid");
                if (StringHelper.InvaildString(temp) && !wbid.contains(temp)) {
                    wbid += temp + ",";
                }
            }
            robj.put("wbid", StringHelper.fixString(wbid, ','));
        }
        return robj;
    }

    /**
     * 获取网站信息，同时包含下级所有站点id
     * 
     * @param wbid
     * @return
     */
    @SuppressWarnings("unchecked")
    private JSONArray getWbID(String wbid) {
        JSONArray webArray = null, temp;
        JSONObject objTemp, obj = null;
        ;
        if (StringHelper.InvaildString(wbid) && wbid.equals("0")) {
            return new JSONArray();
        }
        String webInfo = (String) appsProxy.proxyCall("/WebInfo/WebInfo/WebFindId/" + wbid);
        if (StringHelper.InvaildString(webInfo)) {
            temp = JSONArray.toJSONArray(webInfo);
            if (temp != null && temp.size() > 0) {
                webArray = new JSONArray();
                for (Object object : temp) {
                    obj = new JSONObject();
                    objTemp = (JSONObject) object;
                    obj.put("wbid", objTemp.getMongoID("_id"));
                    obj.put("wbname", objTemp.getString("title"));
                    obj.put("wbgid", objTemp.getString("wbgid"));
                    obj.put("itemfatherID", objTemp.getString("itemfatherID"));
                    webArray.add(obj);
                }
            }
        }
        return webArray;
    }

    /**
     * 登录失败，根据不同的登录模式，返回不同的失败提示信息
     * 
     * @param object
     * @param mode
     * @return
     */
    private String result(JSONObject object, int mode) {
        String result = "";
        if (object == null || object.size() <= 0) {
            switch (mode) {
            case 0:
                result = rMsg.netMSG(12, "用户名密码不匹配");
                break;
            case 1:
                result = rMsg.netMSG(13, "邮箱密码不匹配");
                break;
            case 2:
                result = rMsg.netMSG(14, "手机号密码不匹配");
                break;
            case 3:
                result = rMsg.netMSG(15, "姓名身份证号不匹配");
                break;
            case 4:
                result = rMsg.netMSG(16, "姓名密码不匹配");
                break;
            }
        } else {
            result = rMsg.netMSG(0, object);
        }
        return result;
    }

    /**
     * 验证登录参数
     * 
     * @param usersInfo
     * @return
     */
    @SuppressWarnings("unchecked")
    private String CheckLogin(String usersInfo) {
        String usersname = "";
        String password = "";
        JSONObject object = JSONObject.toJSON(usersInfo);
        if (object == null || object.size() <= 0) {
            return rMsg.netMSG(100, "登录失败");
        }
        int mode = 0;
        if (object.containsKey("loginmode")) {
            mode = Integer.parseInt(object.getString("loginmode"));
        }
        switch (mode) {
        case 0: // id + password 用户名+密码
            if (!object.containsKey("id") || !object.containsKey("password")) {
                return rMsg.netMSG(6, "请填写用户名或者密码");
            }
            usersname = object.getString("id");
            password = object.getString("password");
            if (!checkusersName(usersname)) {
                return rMsg.netMSG(1, "用户名格式不合法");
            }
            object.remove("id");
            break;
        case 1: // email + password 邮箱 + 密码
            if (!object.containsKey("email") || !object.containsKey("password")) {
                return rMsg.netMSG(7, "请填写邮箱或者密码");
            }
            usersname = object.getString("email");
            password = object.getString("password");
            if (usersname != null && !usersname.equals("") && !usersname.equals("null")) {
                if (!checkHelper.checkEmail(usersname)) {
                    return rMsg.netMSG(3, "邮箱格式不合法");
                }
            }
            object.remove("email");
            break;
        case 2: // mobphone + password 手机号 + 密码
            if (!object.containsKey("mobphone") || !object.containsKey("password")) {
                return rMsg.netMSG(8, "请填写手机号或者密码");
            }
            usersname = object.getString("mobphone");
            password = object.getString("password");
            if (usersname != null && !usersname.equals("") && !usersname.equals("null")) {
                if (!checkHelper.checkMobileNumber(usersname)) {
                    return rMsg.netMSG(4, "手机号格式不合法");
                }
            }
            object.remove("mobphone");
            break;
        case 3: // name + IDcard 姓名 + 身份证号
            if (!object.containsKey("name") || !object.containsKey("IDcard")) {
                return rMsg.netMSG(5, "请填写真实姓名或者身份证号");
            }
            password = object.getString("name"); // 真实姓名
            usersname = object.getString("IDcard"); // 身份证号
            if (checkHelper.checkRealName(password)) {
                return rMsg.netMSG(9, "请填写正确的姓名");
            }
            if (!checkHelper.checkPersonCardID(usersname)) {
                return rMsg.netMSG(10, "请填写正确的身份证号");
            }
            usersname = codec.md5(usersname.toLowerCase()); // 身份证号MD5加密
            object.remove("name");
            object.remove("IDcard");
            break;
        case 4: // name + password 姓名 + 密码
            if (!object.containsKey("name") || !object.containsKey("password")) {
                return rMsg.netMSG(11, "请填写真实姓名或者密码");
            }
            String name = object.getString("name");
            password = object.getString("password");
            if (checkHelper.checkRealName(usersname)) {
                return rMsg.netMSG(9, "请填写正确的姓名");
            }
            // 根据姓名和密码获取身份证号
            JSONObject obj = getIDCard(name, password);
            if (obj == null || obj.size() <= 0) {
                return object.toJSONString();
            }
            usersname = obj == null ? null : obj.get("IDcard").toString(); // 身份证号
            password = name; // 真实姓名
            object.remove("name");
            break;
        }
        object.put("usersname", usersname);
        object.put("password", password);
        return object.toJSONString();
    }

    /**
     * 根据姓名和密码获取身份证号
     * 
     * @param name
     * @param password
     * @return
     */
    private JSONObject getIDCard(String name, String password) {
        JSONObject object = users.eq("name", name).eq("password", !password.equals("") ? codec.md5(password) : password).field("IDcard").find();
        return object != null ? object : null;
    }

    /**
     * 查询用户信息
     * 
     * @param _id
     *            唯一标识符
     * @return
     */
    private JSONObject find(String _id) {
        JSONObject object = null;
        if (StringHelper.InvaildString(_id) && ObjectId.isValid(_id)) {
            object = users.eq("_id", _id).find();
        }
        return (object != null && object.size() > 0) ? object : null;
    }

    /**
     * 新增用户，修改用户，参数验证
     * 
     * @param _usersInfo
     * @return
     */
    @SuppressWarnings("unchecked")
    private String CheckParam(String _usersInfo) {
        JSONObject object = JSONObject.toJSON(_usersInfo);
        if (object != null && object.size() > 0) {
            if (object.containsKey("id")) {
                String usersName = object.get("id").toString();
                if (!checkusersName(usersName)) {
                    return rMsg.netMSG(1, "用户名格式不合法");
                }
                // 验证用户名是否已存在
                if (findUserID(usersName)) {
                    return rMsg.netMSG(2, "该用户名已被注册过");
                }
            }
            if (object.containsKey("email")) {
                String email = object.get("email").toString();
                if (StringHelper.InvaildString(email)) {
                    if (!checkHelper.checkEmail(email)) {
                        return rMsg.netMSG(3, "邮箱格式不合法");
                    }
                }
            }
            if (object.containsKey("mobphone")) {
                String phoneno = object.get("mobphone").toString();
                if (StringHelper.InvaildString(phoneno)) {
                    if (!checkHelper.checkMobileNumber(phoneno)) {
                        return rMsg.netMSG(4, "手机号格式不合法");
                    }
                }
            }
            if (object.containsKey("IDcard")) {
                String Idcard = object.get("IDcard").toString();
                if (StringHelper.InvaildString(Idcard)) {
                    if (!checkHelper.checkPersonCardID(Idcard)) {
                        return rMsg.netMSG(5, "身份证号不合法");
                    }
                    object.put("IDcard", codec.md5(Idcard.toLowerCase())); // 身份证号MD5加密
                }
            }
            if (object.containsKey("password")) { // 注册数据包含密码字段，则进行md5加密
                String pwd = object.get("password").toString();
                if (StringHelper.InvaildString(pwd)) {
                    object.put("password", codec.md5(pwd));
                }
            }
        }
        return (object != null && object.size() > 0) ? object.toJSONString() : null;
    }

    /**
     * 用户名格式验证，只含有数字，字母，且长度为7-15
     * 
     * @param usersName
     * @return
     */
    private boolean checkusersName(String usersName) {
        String regex = "([a-z]|[A-Z]|[0-9]|[\\u4e00-\\u9fa5])+";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(usersName);
        return (usersName.length() >= 7) && (usersName.length() <= 15) && (m.matches());
    }

    /**
     * 验证用户名是否已存在
     * 
     * @param id
     * @return
     */
    private boolean findUserID(String id) {
        JSONObject object = null;
        object = users.eq("id", id).find();
        return object != null && object.size() > 0;
    }

    /**
     * 添加操作日志
     * 
     * @param type
     *            类型
     *            0:登录系统；1:新增用户,用户名：XXX，姓名：XXX；2:删除用户,用户名：XXX，姓名：XXX；3:修改用户,用户名：
     *            XXX，姓名：XXX；4:修改密码；5：重置用户密码，用户名为：XXX，姓名为：XXX
     *            6：重置所有用户密码；7:查询用户信息，查询条件：XXX
     * @param _id
     *            操作对象用户id
     * @param func
     *            调用接口
     * @param condString
     *            查询条件
     */
    private void AddLog(int type, String _id, String func, String condString) {
        String action = "";
        String temp = getIDUserName(_id);
        switch (type) {
        case 0:
            action = "登录系统";
            break;
        case 1:
            action = "新增用户" + temp;
            break;
        case 2:
            action = "删除用户" + temp;
            break;
        case 3:
            action = "修改用户" + temp;
            break;
        case 4:
            action = "修改密码";
            break;
        case 5:
            action = "重置用户密码，" + temp;
            break;
        case 6:
            action = "重置所有用户密码";
            break;
        case 7:
            action = "查询用户信息，查询条件：" + condString;
            break;
        }
        System.out.println("用户操作：" + action);
        appsProxy.proxyCall("/Logs/Logs/AddLogs/" + userId + "/" + userName + "/" + action + "/" + func, appsProxy.getCurrentAppInfo());
    }

    /**
     * 获取用户信息
     * 
     * @param uid
     * @return {_id:id,name,_id:id,name,_id:id,name}
     */
    @SuppressWarnings("unchecked")
    private JSONObject getUserInfo(String uid) {
        JSONObject object = new JSONObject(), temp;
        String[] value = null;
        String tempString = "";
        try {
            if (StringHelper.InvaildString(uid)) {
                value = uid.split(",");
            }
            if (value != null) {
                users.or();
                for (String _id : value) {
                    users.eq("_id", _id);
                }
                JSONArray array = users.field("_id,id,name").select();
                if (array != null && array.size() > 0) {
                    for (Object object2 : array) {
                        temp = (JSONObject) object2;
                        tempString = temp.getString("id") + "," + temp.getString("name");
                        object.put(temp.getMongoID("_id"), tempString);
                    }
                }
            }
        } catch (Exception e) {
            object = new JSONObject();
        }
        return object;
    }

    /**
     * 获取用户名和姓名
     * 
     * @param _id
     * @return 用户名为：" + id + ",姓名为：" + name；用户名为：" + id + ",姓名为：" + name
     */
    private String getIDUserName(String _id) {
        String temp = "", id = "", name = "";
        JSONObject obj = getUserInfo(_id);
        String key, value;
        String[] tempString = null;
        for (Object objs : obj.keySet()) {
            key = objs.toString();
            value = obj.getString(key);
            if (StringHelper.InvaildString(value)) {
                tempString = value.split(",");
                id = tempString[0];
                name = (tempString.length >= 2) ? tempString[0] : name;
            }
            temp += "用户名为：" + id + ",姓名为：" + name + "；";
        }
        return StringHelper.fixString(temp, '；');
    }
}
