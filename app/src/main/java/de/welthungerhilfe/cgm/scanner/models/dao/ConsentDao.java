package de.welthungerhilfe.cgm.scanner.models.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.helper.offline.DbConstants;
import de.welthungerhilfe.cgm.scanner.models.Consent;
import de.welthungerhilfe.cgm.scanner.models.Measure;
import de.welthungerhilfe.cgm.scanner.models.Person;

/**
 * Child Growth Monitor - quick and accurate data on malnutrition
 * Copyright (c) 2018 Markus Matiaschek <mmatiaschek@gmail.com>
 * Copyright (c) 2018 Welthungerhilfe Innovation
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, orf
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

@Dao
public interface ConsentDao {
    @Query("SELECT * FROM " + DbConstants.TABLE_CONSENTS)
    List<Consent> loadAll();

    @Query("SELECT * FROM " + DbConstants.TABLE_CONSENTS + " WHERE id =:consentId")
    Consent getById(String consentId);

    @Query("SELECT * FROM " + DbConstants.TABLE_CONSENTS + " WHERE qrcode =:qrCode")
    List<Consent> getByQrCode(String qrCode);

    @Query("UPDATE " + DbConstants.TABLE_CONSENTS + " SET id =:onlineId WHERE id =:offlineId")
    void syncConsentId(String offlineId, String onlineId);

    @Update
    void update(Consent... consents);

    @Insert
    void insertAll(Consent... consents);

    @Delete
    void delete(Consent consent);
}
