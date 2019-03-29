package com.sif.core.exception;

public class GenericException extends RuntimeException{

	private static final long serialVersionUID = 1L;
	
	private String titulo;
	
	public GenericException(String titulo, String mensagem) {
		super(mensagem);
		this.titulo = titulo;
	}
	
	public String getTitulo() {
		return titulo;
	}
}
