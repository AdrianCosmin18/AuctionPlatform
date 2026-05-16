package org.nedelcu.cosmin.auction.api.common.exception;

import jakarta.persistence.OptimisticLockException;
import java.net.URI;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Resource not found");
        problem.setType(URI.create("https://auction-platform/errors/not-found"));
        return problem;
    }

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusiness(BusinessException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Business rule violation");
        problem.setType(URI.create("https://auction-platform/errors/business-rule"));
        return problem;
    }

    @ExceptionHandler({
            OptimisticLockException.class,
            OptimisticLockingFailureException.class
    })
    public ProblemDetail handleOptimisticLock(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "The auction was updated by another user. Please refresh and try again."
        );
        problem.setTitle("Concurrent modification");
        problem.setType(URI.create("https://auction-platform/errors/concurrent-modification"));
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Request validation failed"
        );
        problem.setTitle("Validation error");
        problem.setType(URI.create("https://auction-platform/errors/validation"));
        problem.setProperty(
                "errors",
                ex.getBindingResult().getFieldErrors().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                fieldError -> fieldError.getField(),
                                fieldError -> fieldError.getDefaultMessage(),
                                (left, right) -> left
                        ))
        );
        return problem;
    }
}
