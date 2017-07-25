package fi.testee.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "test")
public class TestEntity {
    @Id
    private long id;
    private String stringValue;

    public TestEntity() {
    }

    public TestEntity(final long id, final String stringValue) {
        this.id = id;
        this.stringValue = stringValue;
    }

    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(final String stringValue) {
        this.stringValue = stringValue;
    }
}
