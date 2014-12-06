package com.senz.core;

import android.util.JsonReader;
import android.util.JsonWriter;
import android.os.Parcel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.lang.reflect.Method;
import com.senz.utils.Jsonable;

public class Utils {
    public static long longFrom8Bytes(byte b7, byte b6, byte b5, byte b4, byte b3, byte b2, byte b1, byte b0) {
        long l7 = unsignedByteToLong(b7);
        long l6 = unsignedByteToLong(b6);
        long l5 = unsignedByteToLong(b5);
        long l4 = unsignedByteToLong(b4);
        long l3 = unsignedByteToLong(b3);
        long l2 = unsignedByteToLong(b2);
        long l1 = unsignedByteToLong(b1);
        long l0 = unsignedByteToLong(b0);

        return (l7 << 56) + (l6 << 48) + (l5 << 40) + (l4 << 32) + (l3 << 24) + (l2 << 16) + (l1 << 8) + l0;
    }

    public static long unsignedByteToLong(byte b) {
        return b & 0xFFL;
    }

    public static int intFrom2Bytes(byte b1, byte b0) {
        int i1 = unsignedByteToInt(b1);
        int i0 = unsignedByteToInt(b0);

        return (i1 << 8) + i0;
    }

    public static int unsignedByteToInt(byte b) {
        return b & 0xFF;
    }

    public static String capitalize(String s) {
        if (s == null)
            throw new NullPointerException();
        return s.substring(0, 1).toUpperCase().concat(s.substring(1));
    }

    public static <T extends Jsonable> void writeToJsonArray(JsonWriter writer, Collection<T> ts) throws IOException {
        writer.beginArray();
        for (T t : ts)
            t.writeToJson(writer);
        writer.endArray();
    }

    public static <T extends Jsonable> ArrayList<T> readFromJsonArray(JsonReader reader, Jsonable.Creator<T> creator) throws IOException {
        ArrayList<T> ts = new ArrayList<T>();

        reader.beginArray();
        while (reader.hasNext())
            ts.add(creator.createFromJson(reader));
        reader.endArray();

        return ts;
    }

    public static void writeParcelStringMap(Parcel out, Map<String, String> map) {
        Collection<Entry<String, String>> entrySet = map.entrySet();
        out.writeInt(entrySet.size());
        for (Entry<String, String> entry : entrySet) {
            out.writeString(entry.getKey());
            out.writeString(entry.getValue());
        }
    }

    public static void readParcelStringMap(Map<String, String> map, Parcel in) {
        int n = in.readInt();
        for (int i = 0; i < n; i++) {
            String k = in.readString();
            String v = in.readString();
            map.put(k, v);
        }
    }

    public static void writeStringMapAsJsonObject(JsonWriter writer, Map<String, String> map) throws IOException {
        writer.beginObject();
        for (Entry<String, String> entry : map.entrySet()) {
            writer.name(entry.getKey()).value(entry.getValue());
        }
        writer.endObject();
    }

    public static void readJsonStringMap(JsonReader reader, Map<String, String> map) throws IOException {
        reader.beginObject();
        while(reader.hasNext())
            map.put(reader.nextName(), reader.nextString());
        reader.endObject();
    }

    public static void skipProperties(JsonReader reader) throws IOException {
        while (reader.hasNext()) {
            reader.nextName();
            reader.skipValue();
        }
    }
};
