package de.welthungerhilfe.cgm.scanner.ui.delegators;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;

public interface OnMeasuresLoad {
    void onMeasuresLoaded(List<Measure> measureList);
}
