package com.zmansiv.fbstats.misc;

import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import com.restfb.JsonMapper;

import java.util.List;

public class JsonToMongoMapper implements JsonMapper {

    @Override
    @SuppressWarnings("unchecked")
    public <T> T toJavaObject(String json, Class<T> type) {
        if (type.equals(DBObject.class)) {
            return (T) JSON.parse(json);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public <T> List<T> toJavaList(String json, Class<T> type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toJson(Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toJson(Object object, boolean ignoreNullValuedProperties) {
        throw new UnsupportedOperationException();
    }
}
