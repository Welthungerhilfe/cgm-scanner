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

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

import java.util.Locale;

import de.welthungerhilfe.cgm.scanner.datasource.repository.CsvExportableModel;

import static de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase.TABLE_MEASURE_RESULT;

@Entity(tableName = TABLE_MEASURE_RESULT)
public class MeasureResult extends CsvExportableModel {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String measure_id;
    private String model_id;
    private String key;
    private float confidence_value;
    private float float_value;
    private String json_value;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMeasure_id() {
        return measure_id;
    }

    public void setMeasure_id(@NonNull String measure_id) {
        this.measure_id = measure_id;
    }

    public String getModel_id() {
        return model_id;
    }

    public void setModel_id(String model_id) {
        this.model_id = model_id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public float getConfidence_value() {
        return confidence_value;
    }

    public void setConfidence_value(float confidence_value) {
        this.confidence_value = confidence_value;
    }

    public float getFloat_value() {
        return float_value;
    }

    public void setFloat_value(float float_value) {
        this.float_value = float_value;
    }

    public String getJson_value() {
        return json_value;
    }

    public void setJson_value(String json_value) {
        this.json_value = json_value;
    }

    @Override
    public String getCsvFormattedString() {
        return String.format(Locale.US, "%d,%s,%s,%s,%f,%f,%s",id,measure_id,model_id,key,confidence_value,float_value,json_value);
    }

    @Override
    public String getCsvHeaderString() {
        return "id,measure_id,model_id,key,confidence_value,float_value,json_value";
    }
}
