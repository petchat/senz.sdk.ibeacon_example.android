package com.senz.network;

import android.location.Location;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Pair;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.senz.core.*;
import com.senz.service.DeviceInfo;
import com.senz.utils.L;

/***********************************************************************************************************************
 * @ClassName:   Network
 * @Author:      zhzhzoo
 * @CommentBy:   Woodie
 * @CommentAt:   Tue, Oct 27, 2014
 * @Description: This class is similar to a LruCache's subclass. It packages the LruCache's put() and get() for BeaconWithSenz.
 ***********************************************************************************************************************/

public class Network {
    private static String queryUrl = "https://leancloud.cn/1.1/functions/";
    private static int timeout = (int) TimeUnit.SECONDS.toMillis(10);
    private static String AVOS_ID = "vigxpgtjk8w6ruxcfaw4kju3ssyttgcqz38y6y6uablqivjd";
    private static String AVOS_KEY = "dxbawm2hh0338hb37wap59gticgr92dpajd80tzekrgv1ptw";

    // Write Location info into JsonWriter
    private static void writeLocation(JsonWriter writer, Location location) throws IOException {
        writer.beginObject();
        writer.name("latitude").value(location.getLatitude());
        writer.name("longitude").value(location.getLongitude());
        writer.name("accuracy").value(location.getAccuracy());
        writer.name("time").value(location.getTime());
        writer.name("speed").value(location.getSpeed());
        writer.endObject();
    }

    // Write App list into JsonWriter
    private static void writeDeviceInfo(JsonWriter writer, DeviceInfo device) throws IOException {
        writer.beginObject();
        writer.name("wifi_mac").value(device.getWIFImac());
        writer.name("firmware_version").value(device.getFirmwareVersion());
        writer.name("kernel_version").value(device.getKernelVersion());
        writer.name("system_version").value(device.getSysVersion());
        writer.name("system_model").value(device.getSysModel());
        writer.name("cpu_frequency").value(device.getCPUfreq());
        writer.name("cpu_model").value(device.getCPUmodel());
        writer.name("sdcard_size").value(device.getSdCardSize());
        writer.name("sdcard_left").value(device.getSdCardLeft());
        writer.name("memory").value(device.getDeviceMemory());
        writer.endObject();
    }

    // Write Beacons info into JsonWriter and ready to send request.
    private static void writeBeaconsQueryPost(JsonWriter writer, Collection<Beacon> toQuery, Location lastBeen) throws IOException {
        writer.beginObject();
        if (lastBeen != null) {
            writer.name("location");
            writeLocation(writer, lastBeen);
        }
        writer.name("beacons");
        Utils.writeToJsonArray(writer, toQuery);
        writer.endObject();
        writer.close();
    }

    // Write Beacons info into JsonWriter and ready to send request.
    private static void writeLocationQueryPost(JsonWriter writer, Location location) throws IOException {
        writer.beginObject();
        writer.name("location");
        writeLocation(writer, location);
        writer.endObject();
        writer.close();
    }

    // Write Basic info into JsonWriter and ready to send request.
    private static void writeBasicInfoQueryPost(JsonWriter writer, Collection<App> apps, DeviceInfo device) throws IOException {
        writer.beginObject();
        writer.name("app_list");
        Utils.writeToJsonArray(writer, apps);
        writer.name("device_info");
        writeDeviceInfo(writer, device);
        writer.endObject();
        writer.close();
    }

    private static ArrayList<Senz> readSenzesFromJson(JsonReader reader) throws IOException {
        ArrayList<Senz> senzes = new ArrayList<Senz>();
        reader.beginArray();
        while (reader.hasNext()) {
            Senz senz = new Senz(reader);
            senzes.add(senz);
        }
        reader.endArray();
        return senzes;
    }

    private static POI readPOIFromJson(JsonReader reader) throws IOException {
        String name, _at = "", _poi_group = "";
        reader.beginObject();
        while (reader.hasNext()) {
            name = reader.nextName();
            //reader.nextString();
            if (name.equals("at")) {
                _at = reader.nextString();
            }
            else if (name.equals("poi_group")) {
                _poi_group = reader.nextString();
            }
            else {
                reader.skipValue();
            }
        }
        reader.endObject();

        return new POI(_at, _poi_group);
    }

    private static TOI readTOIFromJson(JsonReader reader) throws IOException {
        String name, _while = "", _when = "";
        reader.beginObject();
        while (reader.hasNext()) {
            name = reader.nextName();
            //reader.nextString();
            if (name.equals("while")) {
                _while = reader.nextString();
            }
            else if (name.equals("when")) {
                _when = reader.nextString();
            }
            else {
                reader.skipValue();
            }
        }
        reader.endObject();
        return new TOI(_while, _when);
    }

    // Read Senz result from a JsonReader.
    private static ArrayList<Senz> readSenzResult(JsonReader reader) throws IOException {
        String name, result = null;
        ArrayList<Senz> senzes = new ArrayList<Senz>();
        TOI toi = new TOI("null", "null");
        POI poi = new POI("null", "null");
        // read result from reader
        reader.beginObject();
        while (reader.hasNext()) {
            name = reader.nextName();
            // Get result's item.
            if (name.equals("result")) {
                result = reader.nextString();
                L.i("[Network] The 'result' is: " + result);
            }
            else {
                reader.skipValue();
            }
        }
        reader.endObject();
        reader.close();
        // If there is no result, It will throw an exception.
        if (result == null) {
            //L.e("[Network] Analysis result error");
            throw new ResultNotPresentException();
        }

        // Analysis the result, and
        // Pick up senz object from result.
        reader = new JsonReader(new StringReader(result));
        reader.beginObject();
        while (reader.hasNext()) {
            name = reader.nextName();
            if (name.equals("senz")) {
                senzes = readSenzesFromJson(reader);
            }
            else if (name.equals("POI")) {
                poi = readPOIFromJson(reader);
            }
            else if (name.equals("TOI")){
                toi = readTOIFromJson(reader);
            }
            else{
                reader.skipValue();
            }
        }
        reader.endObject();
        reader.close();
        // TOI and POI
        Senz senz = new Senz("null", "null", toi._while, toi._when, poi._at, "null", "null", poi._poi_group);
        senzes.add(senz);
        return senzes;
    }

    // Read Static Info result from a JsonReader.
    private static StaticInfo readStaticInfoResult(JsonReader reader) throws IOException {
        StaticInfo staticInfo = null;
        String name, result = null;
        // read result from reader
        reader.beginObject();
        while (reader.hasNext()) {
            name = reader.nextName();
            // Get result's item.
            if (name.equals("result")) {
                result = reader.nextString();
                L.i("[Network] The 'result' is: " + result);
            }
            else {
                reader.skipValue();
            }
        }
        reader.endObject();
        reader.close();
        // If there is no result, It will throw an exception.
        if (result == null) {
            //L.e("[Network] Analysis result error");
            throw new ResultNotPresentException();
        }

        // Analysis the result, and
        // Pick up static info object from result.
        reader = new JsonReader(new StringReader(result));
        reader.beginObject();
        while (reader.hasNext()) {
            name = reader.nextName();
            if (name.equals("static_info")) {
                staticInfo = new StaticInfo(reader);
            }
            else{
                reader.skipValue();
            }
        }
        reader.endObject();
        reader.close();
        return staticInfo;
    }

    private interface QueryWriter {
        public void write(OutputStream os) throws IOException;
    }

    private interface ResultReader<T> {
        public T read(InputStream is) throws IOException;
    }

    // It's the main function for sending http request.
    public static <T> T doQuery(URL url, QueryWriter w, ResultReader<T> r) throws IOException {
        // According to url's type, url.openConnection will return different object of URLConnection's subclass.
        // Here it will return a object of HttpURLConnection, because of url's head is "http".
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        // Set http's header.
        urlConnection.setConnectTimeout(timeout);
        urlConnection.setRequestProperty("Content-Type", "application/json");
        urlConnection.setRequestProperty("charset", "utf-8");
        urlConnection.setRequestProperty("X-AVOSCloud-Application-Id", AVOS_ID);
        urlConnection.setRequestProperty("X-AVOSCloud-Application-Key", AVOS_KEY);
        urlConnection.setRequestProperty("X-AVOSCloud-Application-Production", "0");
        T t = null;

        try {
            // writer is not allowed be null.
            if (w != null) {
                // Url's connection can be used to output(or input), if you want the connection output, then set it true.
                urlConnection.setDoOutput(true);
                urlConnection.setChunkedStreamingMode(0);
            }
            // Write the sending message.
            w.write(urlConnection.getOutputStream());
            // Read the receiving message.
            t = r.read(urlConnection.getInputStream());
        }
        finally {
            urlConnection.disconnect();
        }

        return t;
    }

    // Query with Location info.
    public static ArrayList<Senz> queryLocation(final Location location) throws IOException {
        return doQuery(
                new URL(queryUrl + "beacons"),
                new QueryWriter() {
                    @Override
                    public void write(OutputStream os) throws IOException {
                        // Init the StringWriter sized fo 100
                        StringWriter sw = new StringWriter(100);
                        // Write the beacons info and location into StringWriter.
                        writeLocationQueryPost(new JsonWriter(sw), location);
                        L.i("[Network] The 'message' is: " + sw.toString());
                        // Write location info into a JsonWriter,
                        // which Creates a new instance that writes a JSON-encoded stream to os.
                        // The os will return to be the post's para
                        writeLocationQueryPost(new JsonWriter(new OutputStreamWriter(os)), location);
                    }
                },
                new ResultReader<ArrayList<Senz>>() {
                    @Override
                    public ArrayList<Senz> read(InputStream is) throws IOException {
                        return readSenzResult(new JsonReader(new InputStreamReader(is)));
                    }
                });
    }

    // Query with Beacons info.
    public static ArrayList<Senz> queryBeacons(final Collection<Beacon> toQuery, final Location lastBeen) throws IOException {
        return doQuery(
                new URL(queryUrl + "beacons"),
                new QueryWriter() {
                    @Override
                    // This callback will write the location and beacons info into os.
                    public void write(OutputStream os) throws IOException {
                        // Init the StringWriter sized fo 100
                        StringWriter sw = new StringWriter(100);
                        // Write the beacons info and location into StringWriter.
                        writeBeaconsQueryPost(new JsonWriter(sw), toQuery, lastBeen);
                        L.i("[Network] The sending message is: " + sw.toString());
                        // Write location and beacons info into a JsonWriter,
                        // which Creates a new instance that writes a JSON-encoded stream to os.
                        // The os will return to be the post's para
                        writeBeaconsQueryPost(new JsonWriter(new OutputStreamWriter(os)), toQuery, lastBeen);
                    }
                },
                new ResultReader<ArrayList<Senz>>() {
                    @Override
                    public ArrayList<Senz> read(InputStream is) throws IOException {
                        return readSenzResult(new JsonReader(new InputStreamReader(is)));
                    }
                });
    }

    // Query with Basic info。
    public static StaticInfo queryBasicInfo(final Collection<App> apps, final DeviceInfo device) throws IOException {
        return doQuery(
                new URL(queryUrl + "beacons"),
                new QueryWriter() {
                    @Override
                    // This callback will write the location and beacons info into os.
                    public void write(OutputStream os) throws IOException {
                        // Init the StringWriter sized fo 100
                        StringWriter sw = new StringWriter(200);
                        // Write the beacons info and location into StringWriter.
                        writeBasicInfoQueryPost(new JsonWriter(sw), apps, device);
                        L.i("[Network] The sending message is: " + sw.toString());
                        // Write location and beacons info into a JsonWriter,
                        // which Creates a new instance that writes a JSON-encoded stream to os.
                        // The os will return to be the post's para
                        writeBasicInfoQueryPost(new JsonWriter(new OutputStreamWriter(os)), apps, device);
                    }
                },
                new ResultReader<StaticInfo>() {
                    @Override
                    public StaticInfo read(InputStream is) throws IOException {
                        return readStaticInfoResult(new JsonReader(new InputStreamReader(is)));
                    }
                });
    }

    public static class ResultNotPresentException extends IOException {
    }
}
