package org.dlands.ConnectionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class JSONInterface extends JSONArray{

    JSONInterface(String JSONString) throws JSONException {
        super(new JSONObject(JSONString).getJSONArray("devices").toString());
        System.out.println("Data : " + this.toString());
    }

    public JSONObject index(int index){
        try {
            return this.getJSONObject(index);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public <T> void setData(int i, String name, T value){
        try {
            this.getJSONObject(i).put(name,value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String toString(){
        try {
            return new JSONObject().put("devices", this).toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void addNewObject(String title, String ip, int port, int drawable){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("title", title);
            jsonObject.put("ip",  ip);
            jsonObject.put("port",  port);
            jsonObject.put("icon", drawable);
            this.put(jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
}
