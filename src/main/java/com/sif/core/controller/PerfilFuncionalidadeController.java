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

import com.sif.core.service.PerfilFuncionalidadeService;
import com.sif.model.PerfilFuncionalidade;

@RestController
@RequestMapping("/perfilfuncionalidade")
public class PerfilFuncionalidadeController{
	
	@Autowired
	PerfilFuncionalidadeService service;
	
	@GetMapping
	@PreAuthorize("hasAuthority('/perfilfuncionalidade/list')")
	public List<PerfilFuncionalidade> getAll() {
		return service.getAll();
	}
	
	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('/perfilfuncionalidade/list')")
	public ResponseEntity<PerfilFuncionalidade> findById(@PathVariable() Long id) {
		return service.findById(id);
	}
	
	@PostMapping
	@PreAuthorize("hasAuthority('/perfilfuncionalidade/new')")
	public ResponseEntity<PerfilFuncionalidade> create(@RequestBody PerfilFuncionalidade perfil) {
		return service.create(perfil);
	}
	
	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('/perfilfuncionalidade/edit')")
	public ResponseEntity<PerfilFuncionalidade> update(@RequestBody PerfilFuncionalidade perfil, @PathVariable() Long id) {
		return service.update(perfil, id);
	}
	
	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('/perfilfuncionalidade/delete')")
	public ResponseEntity<PerfilFuncionalidade> delete(@PathVariable() Long id) {
		return service.delete(id);
	}
}

