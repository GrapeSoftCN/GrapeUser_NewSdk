package Model;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import apps.appsProxy;
import database.dbFilter;
import string.StringHelper;

public class UserModel {
	/**
	 * 整合参数，将JSONObject类型的参数封装成JSONArray类型,包含额外参数wbid
	 * 
	 * @param object
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public JSONArray buildCondAddWbid(JSONObject object, String currentWeb) {
		JSONArray condArray = null;
		if (!StringHelper.InvaildString(currentWeb)) {
			if (object != null && object.size() > 0) {
				object.put("wbid", "currentWeb");
				condArray = buildCond(object);
			}
		}
		return condArray;
	}

	/**
	 * 整合参数，将JSONObject类型的参数封装成JSONArray类型
	 * 
	 * @param object
	 * @return
	 */
	public JSONArray buildCond(JSONObject object) {
		String key;
		Object value;
		JSONArray condArray = null;
		dbFilter filter = new dbFilter();
		if (object != null && object.size() > 0) {
			for (Object object2 : object.keySet()) {
				key = object2.toString();
				value = object.get(key);
				filter.eq(key, value);
			}
			condArray = filter.build();
		}
		return condArray;
	}

	/**
	 * 获取下级所有网站，包含本站
	 * 
	 * @param wbid
	 * @return
	 */
	public String[] getAllWeb(String wbid) {
		String[] value = null;
		String wbids = (String) appsProxy.proxyCall("/GrapeWebInfo/WebInfo/getWebTree/" + wbid);
		if (!wbids.equals("")) {
			value = wbids.split(",");
		}
		return value;
	}
}
