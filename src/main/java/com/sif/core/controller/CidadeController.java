package com.sif.core.controller;

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
import com.sif.model.Cidade;
import com.sif.model.custom.CidadeCustomDTO;


@RestController
@RequestMapping("/cidade")
public class CidadeController {

	@Autowired
	CidadeService cidadeService;
	
	@GetMapping
	@PreAuthorize("hasAuthority('/cidade/list')")
	public Page<Cidade> getAll(Pageable pageable, Cidade cidade) {
		return cidadeService.getAll(pageable, cidade);
	}
	
	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('/cidade/list')")
	public ResponseEntity<CidadeCustomDTO> findById(@PathVariable() Long id) {
		return cidadeService.findById(id);
	}
	
	@PostMapping
	@PreAuthorize("hasAuthority('/cidade/new')")
	public ResponseEntity<Cidade> create(@RequestBody CidadeCustomDTO cidade) {
		return cidadeService.create(cidade);
	}
	
	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('/cidade/edit')")
	public ResponseEntity<Cidade> update(@RequestBody CidadeCustomDTO cidade, @PathVariable() Long id) {
		return cidadeService.update(cidade, id);
	}
	
	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('/cidade/delete')")
	public ResponseEntity<Cidade> delete(@PathVariable() Long id) {
		return cidadeService.delete(id);
	}
}
