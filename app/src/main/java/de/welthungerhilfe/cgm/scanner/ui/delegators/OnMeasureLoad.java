package de.welthungerhilfe.cgm.scanner.ui.delegators;

import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;

public interface OnMeasureLoad {
    void onMeasureLoad(Measure measure);
}
