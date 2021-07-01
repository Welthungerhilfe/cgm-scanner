package de.welthungerhilfe.cgm.scanner.datasource.models;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;

public class SyncPersonsResponse {

    @Expose
    public ArrayList<Person> persons;
}
