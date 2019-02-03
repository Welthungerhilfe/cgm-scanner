package de.welthungerhilfe.cgm.scanner.ui.delegators;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.datasource.models.FileLog;

public interface OnFileLogsLoad {
    void onFileLogsLoaded(List<FileLog> list);
}
