package com.senz.core;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.JsonReader;

import java.io.IOException;

/**
 * Created by woodie on 14/12/5.
 */
public class StaticInfo implements Parcelable {

    // Gender      : "male" or "female"
    private String  gender      = "unknown";
    // Age         : "-18" or "19-25" or "26-35" or "36-45" or "46-"
    private String  age         = "unknown";
    // Occupation  : "unemployed" or "student" and so on..
    private String  occupation  = "unknown";
    // Income      : "-5" or "6-15" or "16-35" or "35-"
    private String  income      = "unknown";
    // Education   : "junior" or "senior" or "academician"
    private String  education   = "unknown";
    // Ethnicity   : "China" or "Japen" or "American" and so on..
    private String  ethnicity   = "unknown";
    // Marriage    : "single" or "dater" or "married"
    private String  marriage    = "unknown";
    // HasChildren : yes or no
    private String  hasChildren = "unknown";
    // HasPets     : yes or no
    private String  hasPets     = "unknown";
    // IsPregnant  : yes or no
    private String  isPregnant  = "unknown";

    public StaticInfo(JsonReader reader) throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
            String s = reader.nextName();
            if (s.equals("gender")) {
                this.gender = reader.nextString();

            } else if (s.equals("age")) {
                this.age = reader.nextString();

            } else if (s.equals("occupation")) {
                this.occupation = reader.nextString();

            } else if (s.equals("income")) {
                this.income = reader.nextString();

            } else if (s.equals("education")) {
                this.education = reader.nextString();

            } else if (s.equals("ethnicity")) {
                this.ethnicity = reader.nextString();

            } else if (s.equals("marriage")) {
                this.marriage = reader.nextString();

            } else if (s.equals("hasChildren")) {
                this.hasChildren = reader.nextString();

            } else if (s.equals("hasPets")) {
                this.hasPets = reader.nextString();

            } else if (s.equals("isPregnant")) {
                this.isPregnant = reader.nextString();

            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
    }

    // The user interface.
    public String getGender() {
        return gender;
    }
    public String getAge() {
        return age;
    }
    public String getOccupation() {
        return occupation;
    }
    public String getIncome() {
        return income;
    }
    public String getEducation() {
        return education;
    }
    public String getEthnicity() {
        return ethnicity;
    }
    public String getMarriage() {
        return marriage;
    }
    public boolean hasChildren() {
        if(hasChildren == "yes")
            return true;
        else
            return false;
    }
    public boolean hasPets() {
        if(hasPets == "yes")
            return true;
        else
            return false;
    }
    public boolean isPregnant() {
        if(isPregnant == "yes")
            return true;
        else
            return false;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(gender);
        dest.writeString(age);
        dest.writeString(occupation);
        dest.writeString(income);
        dest.writeString(education);
        dest.writeString(ethnicity);
        dest.writeString(marriage);
        dest.writeString(hasChildren);
        dest.writeString(hasPets);
        dest.writeString(isPregnant);
    }
}
