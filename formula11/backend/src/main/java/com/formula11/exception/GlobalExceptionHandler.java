package com.formula11.exception;

import com.formula11.dto.ErrorResponse;
import com.formula11.adapter.FootballDataException;
import com.formula11.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> accesoDenegado(AccessDeniedException exception, HttpServletRequest request) {
        return responder(HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta operación", request);
    }

    @ExceptionHandler(java.util.NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(java.util.NoSuchElementException exception, HttpServletRequest request) {
        return responder(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException exception, HttpServletRequest request) {
        String mensaje = exception.getBindingResult().getFieldErrors().stream().findFirst()
                .map(error -> error.getDefaultMessage()).orElse("La solicitud no es válida");
        return responder(HttpStatus.BAD_REQUEST, mensaje, request);
    }

    @ExceptionHandler({IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> argumento(IllegalArgumentException exception, HttpServletRequest request) {
        return responder(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> conflicto(IllegalStateException exception, HttpServletRequest request) {
        return responder(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> tipoDeArgumento(MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        return responder(HttpStatus.BAD_REQUEST, "El parámetro " + exception.getName() + " no es válido", request);
    }

    @ExceptionHandler(UsuarioService.EmailDuplicadoException.class)
    public ResponseEntity<ErrorResponse> duplicado(UsuarioService.EmailDuplicadoException exception, HttpServletRequest request) {
        return responder(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ErrorResponse> credencialesInvalidas(CredencialesInvalidasException exception, HttpServletRequest request) {
        return responder(HttpStatus.UNAUTHORIZED, exception.getMessage(), request);
    }

    @ExceptionHandler(FootballDataException.class)
    public ResponseEntity<ErrorResponse> proveedor(FootballDataException exception, HttpServletRequest request) {
        HttpStatus status = switch (exception.getTipo()) {
            case NO_CONFIGURADO, TIMEOUT -> HttpStatus.SERVICE_UNAVAILABLE;
            case LIMITE -> HttpStatus.TOO_MANY_REQUESTS;
            case NO_AUTORIZADO, PROVEEDOR, RESPUESTA_INVALIDA -> HttpStatus.BAD_GATEWAY;
        };
        return responder(status, exception.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> inesperado(Exception exception, HttpServletRequest request) {
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error interno", request);
    }

    private ResponseEntity<ErrorResponse> responder(HttpStatus status, String mensaje, HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ErrorResponse(mensaje, Instant.now(), MDC.get("correlationId")));
    }
}
