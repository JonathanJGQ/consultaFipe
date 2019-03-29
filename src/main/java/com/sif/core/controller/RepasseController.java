package com.sif.core.controller;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sif.core.utils.Funcoes;
import com.sif.model.Contrato;
import com.sif.model.ItensFolha;
import com.sif.model.Usuario;
import com.sif.model.utils.TipoPerfil;
import com.sif.repository.ItensFolhaRepository;
import com.sif.repository.specification.ItensFolhaSpecification;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@RestController
public class RepasseController {

	@Autowired
	Funcoes funcoes;
	@Autowired
	ItensFolhaRepository itensFolhaRepository;
	
	@GetMapping(value = "/folha/download/{periodo}", produces = MediaType.TEXT_PLAIN_VALUE)
	@ApiOperation(value = "/folha/download/MM-YYYY")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "periodo", value = "Período da folha no formato MM-YYYY.", paramType = "path"),
	})
	@ApiResponses({
		@ApiResponse(message = "OK", responseContainer = "list", code = 200),
		@ApiResponse(message = "ERRO - Folha não encontrada", responseContainer = "list", code = 1),
		@ApiResponse(message = "ERRO - Esta folha ainda não foi fechada", responseContainer = "list", code = 2),
		@ApiResponse(message = "ERRO - Formato do período inválido", responseContainer = "list", code = 3),
	})
	public ResponseEntity<byte[]> getArquivo(@PathVariable String periodo, HttpServletRequest request, HttpServletResponse response) {
		
		if(periodo.indexOf("-") < 1) {
			return ResponseEntity.status(3).build();
		}
		
		try {
			Usuario usuario = funcoes.getLoggedUser();
			
			List<ItensFolha> itens = new ArrayList<ItensFolha>();
			ItensFolhaSpecification spec = new ItensFolhaSpecification();
			
			if(usuario.getPerfil().getId().equals(TipoPerfil.ADMINISTRADOR)) {
			} else if(usuario.getPerfil().getId().equals(TipoPerfil.AVERBADORA)) {
				itens = itensFolhaRepository.findAll(Specification
						.where(spec.idAverbadoraEq(usuario.getEntidade()))
						.and(spec.findByStatus())
						.and(spec.periodoFolhaEq(periodo)));
			} else if(usuario.getPerfil().getId().equals(TipoPerfil.CONSIGNATARIA)) {
				itens = itensFolhaRepository.findAll(Specification
						.where(spec.idConsignatariaEq(usuario.getEntidade()))
						.and(spec.findByStatus())
						.and(spec.periodoFolhaEq(periodo)));
			} else if(usuario.getPerfil().getId().equals(TipoPerfil.ORGAO)) {
				itens = itensFolhaRepository.findAll(Specification
						.where(spec.idOrgaoEq(usuario.getEntidade()))
						.and(spec.findByStatus())
						.and(spec.periodoFolhaEq(periodo)));
			} else if(usuario.getPerfil().getId().equals(TipoPerfil.SUPREMO)) {
			}
			
			String result = "id_itens_folha;periodo_folha;nunfunc;nunvinc;nunpens;matricula;cpf;verba;data_inicial;data_fim;tipo_pagamento;valor_parcela\r\n";
			
			SimpleDateFormat sdfPeriodo = new SimpleDateFormat("MM/yyyy");
			SimpleDateFormat sdfDTC = new SimpleDateFormat("dd/MM/yyyy");
			
			if(itens != null && !itens.isEmpty()) {
				for(ItensFolha item : itens) {
					result = result.concat(item.getId().toString()).concat(";")
							.concat(sdfPeriodo.format(item.getFolha().getPeriodo())).concat(";")
							.concat(item.getContrato().getFuncionario().getNumeroFuncional()).concat(";")
							.concat(item.getContrato().getFuncionario().getNumeroVinculo()).concat(";")
							.concat(item.getContrato().getFuncionario().getNumeroPensionista()).concat(";")
							.concat(item.getContrato().getFuncionario().getMatricula()).concat(";")
							.concat(item.getContrato().getFuncionario().getCpf()).concat(";")
							.concat(item.getContrato().getVerba().getDescricao()).concat(";")
							.concat(sdfDTC.format(item.getContrato().getDataLancamento())).concat(";")
							.concat(getDataFimContratoFor(item.getContrato())).concat(";")
							.concat(item.getContrato().getTipoPagamento().toString()).concat(";")
							.concat(item.getContrato().getValorParcela().toString()).concat("\r\n");
				}
			}
						
			return ResponseEntity.ok(result.getBytes());
		} catch(Exception e) {
			return null;
		}
	}
	
	private String getDataFimContratoFor(Contrato contrato) {
		if(contrato == null)
			return null;
		Calendar cal = Calendar.getInstance();
		cal.setTime(contrato.getDataLancamento());
		cal.add(Calendar.MONTH, contrato.getQuantidadeParcelas().intValue());
		
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
		return sdf.format(cal.getTime());
	}
}
