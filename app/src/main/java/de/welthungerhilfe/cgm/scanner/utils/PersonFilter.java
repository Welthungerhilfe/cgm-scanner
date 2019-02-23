package de.welthungerhilfe.cgm.scanner.utils;

import com.google.android.gms.maps.model.LatLng;

import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SORT_DATE;

public class PersonFilter {
    private boolean isQuery;
    private boolean isOwn;
    private boolean isDate;
    private boolean isLocation;

    private int sortType;

    private long fromDate;
    private long toDate;

    private LatLng fromLOC;
    private int radius;

    private String query;

    public PersonFilter() {
        isQuery = false;
        isOwn = false;
        isDate = false;
        isLocation = false;

        sortType = SORT_DATE;
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

    public int getSortType() {
        return sortType;
    }

    public void setSortType(int sortType) {
        this.sortType = sortType;
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

    public LatLng getFromLOC() {
        return fromLOC;
    }

    public int getRadius() {
        return radius;
    }

    public void setFilterLocation(LatLng fromLOC, int radius) {
        isLocation = true;

        this.fromLOC = fromLOC;
        this.radius = radius;
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
}
