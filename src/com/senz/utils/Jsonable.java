package com.senz.utils;

import android.util.JsonWriter;
import android.util.JsonReader;
import java.io.IOException;

public interface Jsonable {
    public void writeToJson(JsonWriter writer) throws IOException;

    public interface Creator<T> {
        public T createFromJson(JsonReader reader) throws IOException;
    }
}
