package com.sif.core.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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

import com.sif.core.service.ConfiguracaoService;
import com.sif.model.Configuracao;

@RestController
@RequestMapping("/configuracao")
public class ConfiguracaoController {

	@Autowired
	ConfiguracaoService configuracaoService;
	
	@GetMapping
	@PreAuthorize("hasAuthority('/configuracao/list')")
	public ResponseEntity<List<Configuracao>> getAll() {
		return configuracaoService.getAll();
	}
	
	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('/configuracao/list')")
	public ResponseEntity<Configuracao> findById(@PathVariable() Long id) {
		return configuracaoService.findById(id);
	}
	
	@PostMapping
	@PreAuthorize("hasAuthority('/configuracao/new')")
	public ResponseEntity<Configuracao> create(@RequestBody Configuracao configuracao) {
		return configuracaoService.create(configuracao);
	}
	
	@PutMapping()
	@PreAuthorize("hasAuthority('/configuracao/edit')")
	public ResponseEntity<String> update(@RequestBody List<Configuracao> configuracao) {
		return configuracaoService.update(configuracao);
	}
	
	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('/configuracao/delete')")
	public ResponseEntity<Configuracao> delete(@PathVariable() Long id) {
		return configuracaoService.delete(id);
	}
}
