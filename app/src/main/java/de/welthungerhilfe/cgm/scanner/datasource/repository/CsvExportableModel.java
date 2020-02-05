package de.welthungerhilfe.cgm.scanner.datasource.repository;

public abstract class CsvExportableModel {

    public abstract String getCsvFormattedString();

    public abstract String getCsvHeaderString();
}
