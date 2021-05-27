package com.zero.repository.rowmapper;

import com.zero.domain.Point;
import com.zero.service.ColumnConverter;
import io.r2dbc.spi.Row;
import java.util.function.BiFunction;
import org.springframework.stereotype.Service;

/**
 * Converter between {@link Row} to {@link Point}, with proper type conversions.
 */
@Service
public class PointRowMapper implements BiFunction<Row, String, Point> {

    private final ColumnConverter converter;

    public PointRowMapper(ColumnConverter converter) {
        this.converter = converter;
    }

    /**
     * Take a {@link Row} and a column prefix, and extract all the fields.
     * @return the {@link Point} stored in the database.
     */
    @Override
    public Point apply(Row row, String prefix) {
        Point entity = new Point();
        entity.setId(converter.fromRow(row, prefix + "_id", Long.class));
        entity.setTitle(converter.fromRow(row, prefix + "_title", String.class));
        entity.setDescription(converter.fromRow(row, prefix + "_description", String.class));
        return entity;
    }
}
