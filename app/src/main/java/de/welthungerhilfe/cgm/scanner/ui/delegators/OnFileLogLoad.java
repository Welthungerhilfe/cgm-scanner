package de.welthungerhilfe.cgm.scanner.ui.delegators;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.datasource.models.FileLog;

public interface OnFileLogLoad {
    void onFileLogLoaded(List<FileLog> list);
}
