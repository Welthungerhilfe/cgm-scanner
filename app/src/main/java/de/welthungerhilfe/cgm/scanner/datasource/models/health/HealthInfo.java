package de.welthungerhilfe.cgm.scanner.datasource.models.health;

public class HealthInfo {
    private long create_timestamp;
    private String owner;
    private String uuid;
    private OwnData own_data;
    private TotalData total_data;

    public long getCreate_timestamp() {
        return create_timestamp;
    }

    public void setCreate_timestamp(long create_timestamp) {
        this.create_timestamp = create_timestamp;
    }

    public OwnData getOwn_data() {
        return own_data;
    }

    public void setOwn_data(OwnData own_data) {
        this.own_data = own_data;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public TotalData getTotal_data() {
        return total_data;
    }

    public void setTotal_data(TotalData total_data) {
        this.total_data = total_data;
    }
}
