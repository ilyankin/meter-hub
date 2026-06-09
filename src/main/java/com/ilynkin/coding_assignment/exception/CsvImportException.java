package com.ilynkin.coding_assignment.exception;

import lombok.Getter;

import java.util.List;


@Getter
public class CsvImportException extends RuntimeException {

    private final transient List<RowError> errors;

    public CsvImportException(List<RowError> errors) {
        super("CSV import failed with " + errors.size() + " error(s)");
        this.errors = errors;
    }

    public record RowError(long row, String column, String message) {
    }
}
