package de.welthungerhilfe.cgm.scanner.ui.delegators;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.datasource.models.Device;

public interface OnDevicesLoad {
    public void onDevicesLoaded(List<Device> list);
}
