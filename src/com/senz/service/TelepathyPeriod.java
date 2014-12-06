package com.senz.service;

import android.os.Parcelable;
import android.os.Parcel;

public class TelepathyPeriod implements Parcelable {
    public final long scanMillis;
    public final long waitMillis;
    public final long GPSMillis;

    public static final Parcelable.Creator<TelepathyPeriod> CREATOR
        = new Parcelable.Creator<TelepathyPeriod>() {
        @Override
        public TelepathyPeriod createFromParcel(Parcel in) {
            return new TelepathyPeriod(in);
        }

        @Override
        public TelepathyPeriod[] newArray(int size) {
            return new TelepathyPeriod[size];
        }
    };

    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(scanMillis);
        out.writeLong(waitMillis);
        out.writeLong(GPSMillis);
    }

    public int describeContents() {
        return 0;
    }

    public String toString() {
        return String.format("scan:%d wait:%d GPS:%d", scanMillis, waitMillis, GPSMillis);
    }

    public TelepathyPeriod(long scanMillis, long waitMillis, long GPSMillis) {
        this.scanMillis = scanMillis;
        this.waitMillis = waitMillis;
        this.GPSMillis = GPSMillis;
    }

    public TelepathyPeriod(Parcel in) {
        scanMillis = in.readLong();
        waitMillis = in.readLong();
        GPSMillis = in.readLong();
    }
}
