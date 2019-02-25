package de.welthungerhilfe.cgm.scanner.utils;

import com.google.android.gms.maps.model.LatLng;

import de.welthungerhilfe.cgm.scanner.datasource.models.Loc;

import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SORT_DATE;

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

        sortType = SORT_DATE;

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
