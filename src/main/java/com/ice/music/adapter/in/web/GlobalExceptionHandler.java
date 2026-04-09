package com.ice.music.adapter.in.web;

import com.ice.music.domain.model.ArtistNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

/**
 * Global exception handler producing RFC 7807 Problem Details.
 *
 * Maps domain and infrastructure exceptions to machine-readable
 * error responses. No stack traces leak to consumers.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ArtistNotFoundException.class)
    public ProblemDetail handleArtistNotFound(ArtistNotFoundException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Artist Not Found");
        problem.setType(URI.create("https://ice.com/problems/artist-not-found"));
        problem.setProperty("artistId", ex.getArtistId());
        return problem;
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "The resource was modified by another request. Please re-read and retry.");
        problem.setTitle("Concurrent Modification Conflict");
        problem.setType(URI.create("https://ice.com/problems/concurrent-modification"));
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid Request");
        problem.setType(URI.create("https://ice.com/problems/invalid-request"));
        return problem;
    }
}
