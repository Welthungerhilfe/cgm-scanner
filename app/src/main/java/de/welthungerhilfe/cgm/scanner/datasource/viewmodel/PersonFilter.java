/*
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
package de.welthungerhilfe.cgm.scanner.datasource.viewmodel;

import de.welthungerhilfe.cgm.scanner.datasource.models.Loc;
import de.welthungerhilfe.cgm.scanner.AppConstants;

public class PersonFilter {
    private boolean isQuery;
    private boolean isOwn;
    private boolean isDate;
    private boolean isLocation;

    private int page;

    private int sortType;

    private long fromDate;
    private long toDate;

    private Loc fromLOC;
    private int radius;

    private String query;

    public PersonFilter() {
        isQuery = false;
        isOwn = false;
        isDate = false;
        isLocation = false;

        sortType = AppConstants.SORT_DATE;

        page = 0;

        fromDate = 0;
        toDate = 0;

        radius = 0;
        query = "";
    }

    public boolean isQuery() {
        return isQuery;
    }

    public boolean isOwn() {
        return isOwn;
    }

    public boolean isDate() {
        return isDate;
    }

    public boolean isLocation() {
        return isLocation;
    }

    public void clearFilterOwn() {
        isOwn = false;
    }

    public void clearFilterQuery() {
        isQuery = false;

        query = "";
    }

    public void clearFilterDate() {
        isDate = false;

        fromDate = 0;
        toDate = 0;
    }

    public void clearFilterLocation() {
        isLocation = false;

        fromLOC = null;
        radius = 0;
    }

    public int getSortType() {
        return sortType;
    }

    public void setSortType(int sortType) {
        this.sortType = sortType;
    }

    public void setFilterOwn() {
        isOwn = true;
    }

    public long getFromDate() {
        return fromDate;
    }

    public long getToDate() {
        return toDate;
    }

    public void setFilterDate(long fromDate, long toDate) {
        isDate = true;

        this.fromDate = fromDate;
        this.toDate = toDate;
    }

    public Loc getFromLOC() {
        return fromLOC;
    }

    public int getRadius() {
        return radius;
    }

    public void setFilterLocation(Loc fromLOC, int radius) {
        isLocation = true;

        this.fromLOC = fromLOC;
        this.radius = radius;
    }

    public void setLocation(Loc fromLOC) {
        this.fromLOC = fromLOC;
    }

    public String getQuery() {
        return query;
    }

    public void setFilterQuery(String query) {
        isQuery = true;

        this.query = query;
    }

    public void setFilterNo() {
        isOwn = false;
        isDate = false;
        isLocation = false;

        fromDate = 0;
        toDate = 0;

        fromLOC = null;
        radius = 0;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }
}
