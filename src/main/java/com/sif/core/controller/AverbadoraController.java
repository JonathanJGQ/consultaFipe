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

import com.sif.core.service.AverbadoraService;
import com.sif.model.Averbadora;
import com.sif.model.custom.AverbadoraCustomDTO;

@RestController
@RequestMapping("/averbadora")
public class AverbadoraController{
	
	@Autowired
	AverbadoraService service;
	
	
	@GetMapping
	@PreAuthorize("hasAuthority('/averbadora/list')")
	public Page<Averbadora> getAll(Pageable pageable, Averbadora averbadora) {
		return service.getAll(pageable, averbadora);
	}
	
	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('/averbadora/list')")
	public ResponseEntity<AverbadoraCustomDTO> findById(@PathVariable() Long id) {
		return service.findById(id);
	}
	
	@PostMapping
	@PreAuthorize("hasAuthority('/averbadora/new')")
	public ResponseEntity<Averbadora> create(@RequestBody AverbadoraCustomDTO averbadora) {
		return service.create(averbadora);
	}
	
	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('/averbadora/edit')")
	public ResponseEntity<Averbadora> update(@RequestBody AverbadoraCustomDTO averbadora, @PathVariable() Long id) {
		return service.update(averbadora, id);
	}
	
	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('/averbadora/delete')")
	public ResponseEntity<Averbadora> delete(@PathVariable() Long id) {
		return service.delete(id);
	}
	
	@PostMapping("/blockDisblock/{id}")
	@PreAuthorize("hasAuthority('/averbadora/block')")
	public String blockDisblock(@PathVariable("id") Long id) {
		return service.blockDisblock(id);
	}
}

