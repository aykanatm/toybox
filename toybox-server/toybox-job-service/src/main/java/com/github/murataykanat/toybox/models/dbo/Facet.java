package com.github.murataykanat.toybox.models.dbo;

import javax.persistence.*;

@Entity
@Table(name = "facets")
public class Facet {
    @Id
    @GeneratedValue
    @Column(name = "facet_id")
    private int id;
    @Column(name = "facet_name")
    private String name;
    @Column(name = "facet_lookup_table_name")
    private String lookupTableName;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLookupTableName() {
        return lookupTableName;
    }

    public void setLookupTableName(String lookupTableName) {
        this.lookupTableName = lookupTableName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
