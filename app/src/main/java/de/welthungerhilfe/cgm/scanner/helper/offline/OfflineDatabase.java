package de.welthungerhilfe.cgm.scanner.helper.offline;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

import de.welthungerhilfe.cgm.scanner.models.Consent;
import de.welthungerhilfe.cgm.scanner.models.Loc;
import de.welthungerhilfe.cgm.scanner.models.Measure;
import de.welthungerhilfe.cgm.scanner.models.Person;
import de.welthungerhilfe.cgm.scanner.models.PersonConsent;
import de.welthungerhilfe.cgm.scanner.models.PersonLoc;
import de.welthungerhilfe.cgm.scanner.models.PersonMeasure;
import de.welthungerhilfe.cgm.scanner.models.dao.ConsentDao;
import de.welthungerhilfe.cgm.scanner.models.dao.LocDao;
import de.welthungerhilfe.cgm.scanner.models.dao.MeasureDao;
import de.welthungerhilfe.cgm.scanner.models.dao.PersonConsentDao;
import de.welthungerhilfe.cgm.scanner.models.dao.PersonDao;
import de.welthungerhilfe.cgm.scanner.models.dao.PersonLocDao;
import de.welthungerhilfe.cgm.scanner.models.dao.PersonMeasureDao;

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

@Database(entities = {Person.class, Consent.class, Measure.class, Loc.class, PersonConsent.class, PersonMeasure.class, PersonLoc.class}, version = 1)
public abstract class OfflineDatabase extends RoomDatabase {
    public abstract PersonDao personDao();
    public abstract ConsentDao consentDao();
    public abstract MeasureDao measureDao();
    public abstract LocDao locDao();

    public abstract PersonConsentDao personConsentDao();
    public abstract PersonMeasureDao personMeasureDao();
    public abstract PersonLocDao personLocDao();
}
