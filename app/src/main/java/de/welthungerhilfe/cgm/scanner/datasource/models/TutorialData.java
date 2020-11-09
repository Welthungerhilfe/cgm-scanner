package de.welthungerhilfe.cgm.scanner.datasource.models;

import android.os.Parcel;
import android.os.Parcelable;

public class TutorialData implements Parcelable {
    String title;
    String instruction1;
    String instruction2;
    int image;
    int position;

    public TutorialData(int image, String title, String instruction1, String instruction2, int position)
    {
        this.image = image;
        this.title = title;
        this.instruction1 = instruction1;
        this.instruction2 = instruction2;
        this.position = position;
    }

    public String getTitle() {
        return title;
    }

    public String getInstruction1() {
        return instruction1;
    }

    public String getInstruction2() {
        return instruction2;
    }

    public int getImage() {
        return image;
    }

    public int getPosition() {
        return position;
    }

    protected TutorialData(Parcel in) {
        title = in.readString();
        instruction1 = in.readString();
        instruction2 = in.readString();
        image = in.readInt();
        position = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(instruction1);
        dest.writeString(instruction2);
        dest.writeInt(image);
        dest.writeInt(position);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<TutorialData> CREATOR = new Parcelable.Creator<TutorialData>() {
        @Override
        public TutorialData createFromParcel(Parcel in) {
            return new TutorialData(in);
        }

        @Override
        public TutorialData[] newArray(int size) {
            return new TutorialData[size];
        }
    };
}