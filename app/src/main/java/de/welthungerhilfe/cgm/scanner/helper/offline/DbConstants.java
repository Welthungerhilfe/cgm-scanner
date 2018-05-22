package de.welthungerhilfe.cgm.scanner.helper.offline;

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

public class DbConstants {
    public static final String DATABASE = "offline_db";

    public static final String TABLE_PERSON = "persons";
    public static final String TABLE_MEASURE = "measures";
    public static final String TABLE_CONSENTS = "consents";
    public static final String TABLE_LOC = "locs";
    public static final String TABLE_REL_PERSON_MEASURE = "person_measure";
    public static final String TABLE_REL_PERSON_CONSENT = "person_consent";
    public static final String TABLE_REL_PERSON_LOC = "person_loc";

    public static final String ID = "id";
    public static final String CREATED = "created";

    // -------------- PERSON DATA ------------------//
    public static final String NAME = "name";
    public static final String SURNAME = "surname";
    public static final String BIRTHDAY = "birthday";
    public static final String SEX = "sex";
    public static final String GUARDIAN = "guardian";
    public static final String AGE_ESTIMATED = "isAgeEstimated";
    public static final String QRCODE = "qrcode";
    public static final String LAST_LOCATION = "last_location";
    public static final String LAST_MEASURE = "last_measure";


    // -------------- MEASURE DATA ------------------//
    public static final String DATE = "date";
    public static final String TYPE = "type";
    public static final String AGE = "age";
    public static final String HEIGHT = "height";
    public static final String WEIGHT = "weight";
    public static final String HEAD_CIRCUMFERENCE = "headCircumference";
    public static final String ARTIFACT = "artifact";
    public static final String VISIBLE = "visible";
    public static final String MUAC = "muac";
    public static final String LOCATION = "location";


    // -------------- CONSENT DATA ------------------//
    public static final String CONSENT = "consent";


    // ------------ SQL GET CONSENTS BY PERSON ------------//
    public static final String QUERY_CONSENTS_BY_PERSON = "SELECT * FROM consents INNER JOIN person_consent ON consents.id=person_consent.consentId WHERE person_consent.personId=:personId";

    // ------------ SQL GET MEASURES BY PERSON ------------//
    public static final String QUERY_MEASURES_BY_PERSON = "SELECT * FROM measures INNER JOIN person_measure ON measures.id=person_measure.measureId WHERE person_measure.personId=:personId";

    // ------------ SQL GET LOCS BY PERSON ------------//
    public static final String QUERY_LOCS_BY_PERSON = "SELECT * FROM locs INNER JOIN person_loc ON locs.id=person_loc.locId WHERE person_loc.personId=:personId";
}