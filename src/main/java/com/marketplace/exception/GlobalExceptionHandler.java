package com.marketplace.exception;

import com.marketplace.invetory.reservation.exception.InsufficientStockException;
import com.marketplace.product.exception.ProductNotFoundException;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(
            MethodArgumentNotValidException ex
    ) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);

        pd.setTitle("Validation Failed");
        pd.setDetail("Invalid request fields");
        List<Map<String, String>> errors =
                ex.getFieldErrors()
                        .stream()
                        .map(err -> Map.of(
                                "field", err.getField(),
                                "message", err.getDefaultMessage()
                        ))
                        .toList();

        pd.setProperty("fieldErrors", errors);
        return pd;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(
            ConstraintViolationException ex
    ) {
       ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
      pd.setTitle("Constraint Violation");
      pd.setDetail("Invalid request parameters");

      List<Map<String, String>> violations = ex.getConstraintViolations()
               .stream()
              .map(violation -> Map.of(
                      "field", violation.getPropertyPath().toString().split("\\.")[1],
                      "message", violation.getMessage()
                      )).toList();

       pd.setProperty("violations", violations);
       return pd;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMalformedRequest(
            HttpMessageNotReadableException ex
    ) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Malformed Request");

        var specificException = ex.getMostSpecificCause();
        if (specificException instanceof UnrecognizedPropertyException upe) {
            pd.setDetail("Unknown field " + upe.getPropertyName());
        } else if (specificException instanceof MismatchedInputException mie) {
            String field = mie.getPath().stream()
                              .map(fieldRef -> fieldRef.getPropertyName())
                                      .collect(Collectors.joining());

             pd.setDetail("Field '%s' expects %s".formatted(field, mie.getTargetType().getSimpleName()));
        }

        return pd;
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail handleProductNotFound(
            ProductNotFoundException ex
    ) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setTitle("Product not found");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ProblemDetail handleProductNotFound(
            InsufficientStockException ex
    ) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setTitle("Insufficient stock");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(OptimisticLockException.class)
    public ProblemDetail handle(
            OptimisticLockException ex
    ) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setTitle("Product version conflict");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handle(
            Exception ex
    ) {
        logger.error("Unhandled exception", ex);

        ProblemDetail pd =
                ProblemDetail.forStatus(
                        HttpStatus.INTERNAL_SERVER_ERROR
                );

        pd.setTitle("Internal Server Error");
        pd.setDetail("An unexpected error occurred");

        return pd;
    }
}