package com.gee12.mytetroid.data;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

/**
 * Класс для хранения списка строк в виде строки в формате JSON.
 * Источник:
 * https://ariefbayu.xyz/how-to-get-guaranteed-order-of-sharedpreferences-getstringset-50a840ecfbe2
 */
public class StringList extends ArrayList<String> {

    public StringList(String json) {
        super();
        fromJSONString(json);
    }

    public StringList() {
        super();
    }

    public String toString(){
        JSONArray ja = new JSONArray();
        for(int idx = 0; idx < size(); idx++){
            ja.put(get(idx));
        }

        return ja.toString();
    }

    public void fromJSONString(String json){
        clear();
        try{
            JSONArray ja = new JSONArray(json);
            for(int idx = 0; idx < ja.length(); idx++){
                add(ja.getString(idx));
            }
        } catch (JSONException ex){
        }
    }
}
