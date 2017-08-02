package fi.testee.maven;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Created by dajudge on 24.07.2017.
 */
@Entity
public class TestEntity {
    @Id
    private long id;
    private String stringValue;

    public TestEntity() {
    }

    public TestEntity(long id, String stringValue) {
        this.id = id;
        this.stringValue = stringValue;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }
}
