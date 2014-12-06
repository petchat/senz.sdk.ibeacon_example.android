package com.senz.core;

import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import com.senz.core.Beacon;
import com.senz.utils.Jsonable;

public class BeaconWithSenz extends Beacon {
    private Senz mSenz;

    public BeaconWithSenz(Beacon beacon, Senz senz) {
        super(beacon);
        this.mSenz = senz;
    }

    public Senz getSenz() {
        return this.mSenz;
    }

    @Override
    public void writeToJson(JsonWriter writer) throws IOException {
        writer.beginObject();
        this.writeToJsonNoBeginEnd(writer);
        writer.endObject();
    }

    public void writeToJsonNoBeginEnd(JsonWriter writer) throws IOException {
        super.writeToJsonNoBeginEnd(writer);
        writer.name("senz");
        this.mSenz.writeToJson(writer);
    }

    public static BeaconWithSenz fromJsonAndSenzById(JsonReader reader, final Map<String, Senz> senzesById) throws IOException {
        Pair<Beacon, String> tmp = beaconSenzIdPairFromJson(reader);
        BeaconWithSenz bws = new BeaconWithSenz(tmp.first, senzesById.get(tmp.second));
        return bws;
    }

    public static Pair<Beacon, String> beaconSenzIdPairFromJson(JsonReader reader) throws IOException {
        reader.beginObject();
        Beacon beacon = new Beacon(reader);
        int remaining = 1;
        String name, id = null;

        while (remaining != 0 && reader.hasNext()) {
            name = reader.nextName();
            remaining--;
            if (name.equals("senz")) {
                id = reader.nextString();

            } else {
                remaining++;
                reader.skipValue();
            }
        }
        reader.endObject();

        return new Pair<Beacon, String>(beacon, id);
    }
}
