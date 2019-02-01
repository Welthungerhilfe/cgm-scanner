package de.welthungerhilfe.cgm.scanner.ui.delegators;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.datasource.models.Person;

public interface OnPersonLoad {
    void onPersonLoaded(List<Person> list);
}
