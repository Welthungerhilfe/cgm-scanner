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
import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.Relation;
import android.support.annotation.NonNull;

import java.io.Serializable;

import de.welthungerhilfe.cgm.scanner.helper.offline.DbConstants;

/**
 * Created by Emerald on 2/19/2018.
 */

@Entity(tableName = DbConstants.TABLE_PERSON)
public class Person implements Serializable {
    @PrimaryKey
    @NonNull
    private String id;  // firebase or database id
    private String name;
    private String surname;
    private long birthday;
    private String sex;  // female, male, other
    private String guardian;
    private boolean isAgeEstimated;
    private String qrcode;
    private long created;

    @Ignore
    private Loc lastLocation;
    @Ignore
    private Measure lastMeasure;

    public Person() {

    }

    public Person(@NonNull String id, String name, String surname, long birthday, String sex, String guardian, boolean isAgeEstimated, String qrcode, long created) {
        this.id = id;
        this.name = name;
        this.surname = surname;
        this.birthday = birthday;
        this.sex = sex;
        this.guardian = guardian;
        this.isAgeEstimated = isAgeEstimated;
        this.qrcode = qrcode;
        this.created = created;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public long getBirthday() {
        return birthday;
    }

    public void setBirthday(long birthday) {
        this.birthday = birthday;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getGuardian() {
        return guardian;
    }

    public void setGuardian(String guardian) {
        this.guardian = guardian;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public String getQrcode() {
        return qrcode;
    }

    public void setQrcode(String qrcode) {
        this.qrcode = qrcode;
    }

    public Measure getLastMeasure() {
        return lastMeasure;
    }

    public void setLastMeasure(Measure lastMeasure) {
        this.lastMeasure = lastMeasure;
    }

    public Loc getLastLocation() {
        return lastLocation;
    }

    public void setLastLocation(Loc lastLocation) {
        this.lastLocation = lastLocation;
    }

    public boolean isAgeEstimated() {
        return isAgeEstimated;
    }

    public void setAgeEstimated(boolean ageEstimated) {
        isAgeEstimated = ageEstimated;
    }
}
