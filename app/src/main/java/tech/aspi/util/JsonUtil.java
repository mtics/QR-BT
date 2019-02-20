package tech.aspi.util;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

public class JsonUtil {

    private Gson gson = new Gson();

    /**
     * 将字符串转为JSON对象，方便后面获取信息
     * @param jsonStr
     * @return
     * @throws JSONException
     */
    public JSONObject strToJson(String jsonStr) throws JSONException {
        return new JSONObject(jsonStr);
    }
}
