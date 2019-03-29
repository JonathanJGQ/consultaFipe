package com.sif.core.controller;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sif.core.exception.GenericException;
import com.sif.core.service.LogAcaoService;
import com.sif.model.Log;
import com.sif.model.LogAcao;

@RestController
@RequestMapping("/logacao")
public class LogAcaoController {
	
	@Autowired
	LogAcaoService logAcaoService;
	
	@GetMapping
	@PreAuthorize("hasAuthority('/logacao/list')")
	public Page<LogAcao> getAll(Pageable pageable, LogAcao logAcao) {
		return logAcaoService.getAll(pageable, logAcao);
	}
	
	@GetMapping("/auditoria/{id}")
	@PreAuthorize("hasAuthority('/log/list')")
	public ResponseEntity<List<Log>> getAuditoriaByLogAcao(@PathVariable("id") String id){
		return logAcaoService.getAuditoriaByLogAcao(id);
	}
	
	@GetMapping("/exportar")
	@PreAuthorize("hasAuthority('/log/list')")
	public void exportar(LogAcao logAcao,  HttpServletResponse response) {
		logAcaoService.gerarPDF(logAcao, response);
	}
	
	@GetMapping("/detalhe/{id}")
	@PreAuthorize("hasAuthority('/log/list')")
	public void exportarDetalhe(@PathVariable("id") String id,  HttpServletResponse response) {
		
		if(id == null || id.isEmpty()) {
			throw new GenericException("Erro", "O id não pode estar vazio!");
		}
		
		Long idLogAcao = Long.parseLong(id);
		LogAcao logAcao = logAcaoService.findById(idLogAcao).getBody();
		
		if(logAcao != null) {
			logAcaoService.gerarPDFDetalhes(logAcao, response);
		} else {
			throw new GenericException("Erro", "Este log não foi encontrado!");
		}
		
	}
	
}
