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

import com.sif.core.service.TipoUsuarioService;
import com.sif.model.TipoUsuario;

@RestController
@RequestMapping("/tipousuario")
public class TipoUsuarioController {

	@Autowired
	TipoUsuarioService tipoUsuarioService;
	
	@GetMapping
	@PreAuthorize("hasAuthority('/tipousuario/list')")
	public Page<TipoUsuario> getAll(Pageable pageable, TipoUsuario tipoUsuario) {
		return tipoUsuarioService.getAll(pageable, tipoUsuario);
	}
	
	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('/tipousuario/list')")
	public ResponseEntity<TipoUsuario> findById(@PathVariable() Long id) {
		return tipoUsuarioService.findById(id);
	}
	
	@PostMapping
	@PreAuthorize("hasAuthority('/tipousuario/new')")
	public ResponseEntity<TipoUsuario> create(@RequestBody TipoUsuario tipoUsuario) {
		return tipoUsuarioService.create(tipoUsuario);
	}
	
	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('/tipousuario/edit')")
	public ResponseEntity<TipoUsuario> update(@RequestBody TipoUsuario tipoUsuario, @PathVariable() Long id) {
		return tipoUsuarioService.update(tipoUsuario, id);
	}
	
	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('/tipousuario/delete')")
	public ResponseEntity<TipoUsuario> delete(@PathVariable() Long id) {
		return tipoUsuarioService.delete(id);
	}
}
