package de.welthungerhilfe.cgm.scanner.ui.delegators;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;

public interface OnMeasureLoad {
    void onMeasureLoaded(List<Measure> measureList);
}
