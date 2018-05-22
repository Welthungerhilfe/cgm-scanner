/*
 * Child Growth Monitor - quick and accurate data on malnutrition
 * Copyright (c) 2018 Markus Matiaschek <mmatiaschek@gmail.com> for Welthungerhilfe
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package de.welthungerhilfe.cgm.scanner.models;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import java.io.Serializable;

import de.welthungerhilfe.cgm.scanner.helper.offline.DbConstants;

/**
 * Created by Emerald on 2/19/2018.
 */

@Entity(tableName = DbConstants.TABLE_MEASURE)
public class Measure implements Serializable {
    @PrimaryKey
    @NonNull
    private String id;
    @ColumnInfo(name = DbConstants.DATE)
    private long date;
    @ColumnInfo(name = DbConstants.TYPE)
    private String type;
    @ColumnInfo(name = DbConstants.AGE)
    private long age; // age from birthday in days
    @ColumnInfo(name = DbConstants.HEIGHT)
    private float height;
    @ColumnInfo(name = DbConstants.WEIGHT)
    private float weight;
    @ColumnInfo(name = DbConstants.MUAC)
    private float muac;
    @ColumnInfo(name = DbConstants.HEAD_CIRCUMFERENCE)
    private float headCircumference;
    @ColumnInfo(name = DbConstants.ARTIFACT)
    private String artifact;
    @ColumnInfo(name = DbConstants.VISIBLE)
    private boolean visible;

    @Ignore
    private Loc location;

    public Measure() {

    }

    public Measure(String id, long date, String type, long age, float height, float weight, float muac, float headCircumference, String artifact, boolean visible) {
        this.id = id;
        this.date = date;
        this.type = type;
        this.age = age;
        this.height = height;
        this.weight = weight;
        this.muac = muac;
        this.headCircumference = headCircumference;
        this.artifact = artifact;
        this.visible = visible;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getAge() {
        return age;
    }

    public void setAge(long age) {
        this.age = age;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }

    public float getMuac() {
        return muac;
    }

    public void setMuac(float muac) {
        this.muac = muac;
    }

    public float getHeadCircumference() {
        return headCircumference;
    }

    public void setHeadCircumference(float headCircumference) {
        this.headCircumference = headCircumference;
    }

    public String getArtifact() {
        return artifact;
    }

    public void setArtifact(String artifact) {
        this.artifact = artifact;
    }

    public Loc getLocation() {
        return location;
    }

    public void setLocation(Loc location) {
        this.location = location;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
