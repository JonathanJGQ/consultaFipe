package com.consulta.fipe.model;

import java.math.BigDecimal;

public class ConsultaFipe {
	
	private String ano;
	
	private String valor;
	
	private String diferencaValor;
	
	private String porcentagem;

	public String getAno() {
		return ano;
	}

	public void setAno(String ano) {
		this.ano = ano;
	}

	public String getPorcentagem() {
		return porcentagem;
	}

	public void setPorcentagem(String porcentagem) {
		this.porcentagem = porcentagem;
	}

	public String getValor() {
		return valor;
	}

	public void setValor(String valor) {
		this.valor = valor;
	}

	public String getDiferencaValor() {
		return diferencaValor;
	}

	public void setDiferencaValor(String diferencaValor) {
		this.diferencaValor = diferencaValor;
	}
	
}	
