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

import com.sif.core.service.OrgaoService;
import com.sif.model.Orgao;
import com.sif.model.custom.OrgaoCustomDTO;

@RestController
@RequestMapping("/orgao")
public class OrgaoController{
	
	@Autowired
	OrgaoService service;
	
	@GetMapping
	@PreAuthorize("hasAuthority('/orgao/list')")
	public Page<Orgao> getAll(Pageable pageable, Orgao orgao) {
		return service.getAll(pageable, orgao);
	}
	
	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('/orgao/list')")
	public ResponseEntity<OrgaoCustomDTO> findById(@PathVariable() Long id) {
		return service.findById(id);
	}
	
	@PostMapping
	@PreAuthorize("hasAuthority('/orgao/new')")
	public ResponseEntity<Orgao> create(@RequestBody OrgaoCustomDTO orgao) {
		return service.create(orgao);
	}
	
	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('/orgao/edit')")
	public ResponseEntity<Orgao> update(@RequestBody OrgaoCustomDTO orgao, @PathVariable() Long id) {
		return service.update(orgao, id);
	}
	
	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('/orgao/delete')")
	public ResponseEntity<Orgao> delete(@PathVariable() Long id) {
		return service.delete(id);
	}
}

