package de.welthungerhilfe.cgm.scanner.models;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.support.annotation.NonNull;

import de.welthungerhilfe.cgm.scanner.helper.offline.DbConstants;

/**
 * Child Growth Monitor - quick and accurate data on malnutrition
 * Copyright (c) 2018 Markus Matiaschek <mmatiaschek@gmail.com>
 * Copyright (c) 2018 Welthungerhilfe Innovation
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

@Entity(tableName = DbConstants.TABLE_REL_PERSON_LOC,
        primaryKeys = { "personId", "locId" },
        foreignKeys = {
                @ForeignKey(entity = Person.class,
                        parentColumns = "id",
                        childColumns = "locId"),
                @ForeignKey(entity = Loc.class,
                        parentColumns = "id",
                        childColumns = "locId")
        })
public class PersonLoc {
    @NonNull
    private String personId;
    @NonNull
    private String locId;

    public PersonLoc(@NonNull String personId, @NonNull String locId) {
        this.personId = personId;
        this.locId = locId;
    }

    @NonNull
    public String getPersonId() {
        return personId;
    }

    public void setPersonId(@NonNull String personId) {
        this.personId = personId;
    }

    @NonNull
    public String getLocId() {
        return locId;
    }

    public void setLocId(@NonNull String locId) {
        this.locId = locId;
    }
}
