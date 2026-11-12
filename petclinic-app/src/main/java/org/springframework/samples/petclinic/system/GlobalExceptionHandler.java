/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.system;

import java.net.URI;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.samples.petclinic.common.exception.ConflictException;
import org.springframework.samples.petclinic.common.exception.NotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

/**
 * Translates domain exceptions and validation failures into RFC 9457
 * {@link ProblemDetail} responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(NotFoundException.class)
	ProblemDetail handleNotFound(NotFoundException ex, HttpServletRequest request) {
		return problem(HttpStatus.NOT_FOUND, ex.getMessage(), request);
	}

	@ExceptionHandler({ ConflictException.class, DataIntegrityViolationException.class })
	ProblemDetail handleConflict(Exception ex, HttpServletRequest request) {
		String detail = ex instanceof ConflictException ? ex.getMessage()
				: "The operation conflicts with an existing resource";
		return problem(HttpStatus.CONFLICT, detail, request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail handleInvalidBody(MethodArgumentNotValidException ex, HttpServletRequest request) {
		List<ValidationError> errors = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(GlobalExceptionHandler::toValidationError)
			.toList();
		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Request validation failed", request);
		problem.setProperty("errors", errors);
		return problem;
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	ProblemDetail handleInvalidParameters(HandlerMethodValidationException ex, HttpServletRequest request) {
		return problem(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	ProblemDetail handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
		List<ValidationError> errors = ex.getConstraintViolations()
			.stream()
			.map(violation -> new ValidationError(violation.getPropertyPath().toString(),
					violation.getMessage()))
			.toList();
		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Request validation failed", request);
		problem.setProperty("errors", errors);
		return problem;
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ProblemDetail handleUnreadableBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
		return problem(HttpStatus.BAD_REQUEST, "Malformed JSON request", request);
	}

	private static ValidationError toValidationError(FieldError error) {
		return new ValidationError(error.getField(), error.getDefaultMessage());
	}

	private static ProblemDetail problem(HttpStatus status, String detail, HttpServletRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(status.getReasonPhrase());
		problem.setInstance(URI.create(request.getRequestURI()));
		return problem;
	}

	/**
	 * Field-level validation error included in the {@code errors} property.
	 */
	public record ValidationError(String field, String message) {
	}

}
