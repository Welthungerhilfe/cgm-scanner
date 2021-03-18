/*
 * Child Growth Monitor - quick and accurate data on malnutrition
 * Copyright (c) 2018 Markus Matiaschek <mmatiaschek@gmail.com>
 * Copyright (c) 2018 Welthungerhilfe Innovation
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.welthungerhilfe.cgm.scanner.datasource.models;

import java.util.ArrayList;

public class RemoteConfig {
    private boolean measure_visibility;
    private int time_to_allow_editing;
    private ArrayList<String> admins;
    private boolean allow_edit;
    private boolean allow_delete;
    private boolean debug;
    private int sync_period;

    public RemoteConfig() {
        this.measure_visibility = true;
        this.time_to_allow_editing = 24;
        this.admins = new ArrayList<>();
        this.allow_edit = true;
        this.allow_delete = false;
        this.debug = false;
        this.sync_period = 5;
    }

    public boolean isMeasure_visibility() {
        return measure_visibility;
    }

    public void setMeasure_visibility(boolean measure_visibility) {
        this.measure_visibility = measure_visibility;
    }

    public int getTime_to_allow_editing() {
        return time_to_allow_editing;
    }

    public void setTime_to_allow_editing(int time_to_allow_editing) {
        this.time_to_allow_editing = time_to_allow_editing;
    }

    public ArrayList<String> getAdmins() {
        return admins;
    }

    public void setAdmins(ArrayList<String> admins) {
        this.admins = admins;
    }

    public boolean isAllow_edit() {
        return allow_edit;
    }

    public void setAllow_edit(boolean allow_edit) {
        this.allow_edit = allow_edit;
    }

    public boolean isAllow_delete() {
        return allow_delete;
    }

    public void setAllow_delete(boolean allow_delete) {
        this.allow_delete = allow_delete;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public int getSync_period() {
        return sync_period;
    }

    public void setSync_period(int sync_period) {
        this.sync_period = sync_period;
    }
}
