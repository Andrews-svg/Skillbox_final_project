package com.example.searchengine.exceptions;

import com.example.searchengine.dto.statistics.response.ErrorResponseDTO;
import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);


    private static volatile boolean manualRestartInProgress = false;


    public static void startManualRestart() {
        manualRestartInProgress = true;
    }


    public static void finishManualRestart() {
        manualRestartInProgress = false;
    }


    public static boolean isManualRestartInProgress() {
        return manualRestartInProgress;
    }


    @ExceptionHandler(ClientAbortException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    @ResponseBody
    public ErrorResponseDTO handleClientAbortException(ClientAbortException ex) {
        if (isManualRestartInProgress()) {
            return null;
        }


        logger.error("Ошибка обработки запроса:", ex);
        return new ErrorResponseDTO(
                LocalDateTime.now(),
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "Сервис временно недоступен."
        );
    }


    @ExceptionHandler(CustomNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public ErrorResponseDTO handleNotFound(CustomNotFoundException ex) {
        return new ErrorResponseDTO(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage()
        );
    }

    @ExceptionHandler(IndexingStartFailureException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public ErrorResponseDTO handleIndexingStartFailure(IndexingStartFailureException ex) {
        return new ErrorResponseDTO(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ex.getMessage()
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    @RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ErrorResponseDTO handleGenericErrors(Exception ex) {
        logger.error("Внутренняя ошибка сервера:", ex);
        return new ErrorResponseDTO(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Произошла внутренняя ошибка сервера: " + ex.getMessage()
        );
    }
}