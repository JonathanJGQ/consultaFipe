package com.sif.core.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sif.core.service.CidadeService;
import com.sif.core.service.ConciliacaoService;
import com.sif.model.Administracao;
import com.sif.model.Cidade;
import com.sif.model.Conciliacao;
import com.sif.model.ConciliacaoErro;
import com.sif.model.ItensFolha;
import com.sif.model.custom.CidadeCustomDTO;
import com.sif.repository.ConciliacaoErroRepository;
import com.sif.repository.ConciliacaoRepository;


@RestController
@RequestMapping("/conciliacao")
public class ConciliacaoController {

	@Autowired
	ConciliacaoRepository conciliacaoRepository;
	
	@Autowired
	ConciliacaoService conciliacaoService;
	
	@Autowired
	ConciliacaoErroRepository conciliacaoErroRepository;
	
	@GetMapping
	@PreAuthorize("hasAuthority('/conciliacao/list')")
	public Page<Conciliacao> findAll(Pageable pageable, Conciliacao conciliacao) {
		return conciliacaoService.getAll(pageable, conciliacao);
	}
	
	@GetMapping("/error/{idConciliacao}")
	public Page<ConciliacaoErro> findErrorByConciliacao(@PathVariable("idConciliacao") Long idConciliacao, Pageable pageable) {
		return conciliacaoService.getErrorByConciliacao(idConciliacao, pageable);
	}
}
