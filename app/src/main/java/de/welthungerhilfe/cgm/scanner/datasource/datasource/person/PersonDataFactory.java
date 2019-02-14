package de.welthungerhilfe.cgm.scanner.datasource.datasource.person;

import android.arch.paging.DataSource;

import de.welthungerhilfe.cgm.scanner.datasource.dao.PersonDao;

public class PersonDataFactory extends DataSource.Factory {
    private PersonDataSource dataSource;

    public PersonDataFactory(PersonDao dao) {
        dataSource = new PersonDataSource(dao);
    }

    @Override
    public DataSource create() {
        return dataSource;
    }
}
