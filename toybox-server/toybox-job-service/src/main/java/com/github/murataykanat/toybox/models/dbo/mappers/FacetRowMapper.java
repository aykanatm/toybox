package com.github.murataykanat.toybox.models.dbo.mappers;

import com.github.murataykanat.toybox.models.dbo.Facet;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class FacetRowMapper implements RowMapper<Facet> {
    @Override
    public Facet mapRow(ResultSet resultSet, int i) throws SQLException {
        Facet facet = new Facet();
        facet.setId(resultSet.getInt("FACET_ID"));
        facet.setName(resultSet.getString("FACET_NAME"));
        facet.setLookupTableName(resultSet.getString("FACET_LOOKUP_TABLE_NAME"));
        return facet;
    }
}
