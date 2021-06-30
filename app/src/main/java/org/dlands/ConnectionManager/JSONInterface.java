package org.dlands.ConnectionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class JSONInterface extends JSONArray{

    JSONInterface(String JSONString) throws JSONException {
        super(JSONString);
    }

    public String getString(int i, String name) {
        try {
            return this.getJSONObject(i).getString(name);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    void setString(int i, String name, String value){
        try {
            this.getJSONObject(i).put(name,value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void deleteObject(int index){
        this.remove(index);
    }


    public String toString(){
        try {
            return new JSONObject().put("devices", this).toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void addNewObject(String title, String ip, int drawable){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("title", title);
            jsonObject.put("ip",  ip);
            jsonObject.put("icon", drawable);
            this.put(jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
}
