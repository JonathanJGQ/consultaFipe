package com.sif.core.exception;


import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;

@ControllerAdvice
public class SifExceptionHandler extends ResponseEntityExceptionHandler {
	
	@Autowired
	private MessageSource messageSource;
	
	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {
		
		
		String title = "Mensagem inválida";
		String message = ex.getCause() != null ? ex.getRootCause().getMessage() : ex.toString();
		return handleExceptionInternal(ex, new Erro(title, message), headers, HttpStatus.BAD_REQUEST, request);
	}
	
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {
		
		List<Erro> erros = criarListaDeErros(ex.getBindingResult());
		return handleExceptionInternal(ex, erros, headers, HttpStatus.BAD_REQUEST, request);
	}
	
	@Override
	protected ResponseEntity<Object> handleBindException(BindException ex, HttpHeaders headers, HttpStatus status,
			WebRequest request) {
		String title = "Operação não permitida";
		String message = "Algum parâmetro passado não pôde ser processado";
//		return super.handleBindException(ex, headers, status, request);
		return handleExceptionInternal(ex, new Erro(title, message), new HttpHeaders(), HttpStatus.NOT_FOUND, request);
	}

	@ExceptionHandler({ EmptyResultDataAccessException.class })
	public ResponseEntity<Object> handleEmptyResultDataAccessException(EmptyResultDataAccessException ex, WebRequest request) {
		String title = "Recurso não encontrado";
		String message = ex.toString();
		return handleExceptionInternal(ex, new Erro(title, message), new HttpHeaders(), HttpStatus.NOT_FOUND, request);
	}
	
	@ExceptionHandler({ InvalidFormatException.class })
	public ResponseEntity<Object> handleInvalidFormatException(InvalidFormatException ex, WebRequest request) {
		String title = "Conversão Inválida";
		String message = "Não realizar a operação de conversão: "+ex.getMessage();
		return handleExceptionInternal(ex, new Erro(title, message), new HttpHeaders(), HttpStatus.NOT_FOUND, request);
	}
	
	@ExceptionHandler({ DataIntegrityViolationException.class } )
	public ResponseEntity<Object> handleDataIntegrityViolationException(DataIntegrityViolationException ex, WebRequest request) {
		String title = "Operação não permitida";
		String message = ExceptionUtils.getRootCauseMessage(ex);
		return handleExceptionInternal(ex, new Erro(title, message), new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
	}
	
	@ExceptionHandler({ IllegalArgumentException.class } )
	public ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request){
		String title = "Operação não permitida";
		String message = "Algum parâmetro passado de forma inesperada: "+"\n"+ExceptionUtils.getRootCauseMessage(ex);
		
		return handleExceptionInternal(ex, new Erro(title, message), new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
		
	}
	
	@ExceptionHandler({ GenericException.class })
	public ResponseEntity<Object> handleGenericException(GenericException ex) {
		String title = ex.getTitulo();
		String message = ex.getMessage();
		return ResponseEntity.badRequest().body(new Erro(title, message));
	}
	
	private List<Erro> criarListaDeErros(BindingResult bindingResult) {
		List<Erro> erros = new ArrayList<>();
		
		for (FieldError fieldError : bindingResult.getFieldErrors()) {
			String mensagemUsuario = messageSource.getMessage(fieldError, LocaleContextHolder.getLocale());
			String mensagemDesenvolvedor = fieldError.toString();
			erros.add(new Erro(mensagemUsuario, mensagemDesenvolvedor));
		}
			
		return erros;
	}
	
	public static class Erro {
		
		private String title;
		private String message;
		
		public Erro(String title, String message) {
			this.title = title;
			this.message = message;
		}

		public String getTitle() {
			return title;
		}

		public String getMessage() {
			return message;
		}
		
	}
}
