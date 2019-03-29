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

import com.sif.core.service.UsuarioPermissaoService;
import com.sif.model.UsuarioPermissao;

@RestController
@RequestMapping("/usuariopermissao")
public class UsuarioPermissaoController{
	
	@Autowired
	UsuarioPermissaoService service;
	
	@GetMapping
	@PreAuthorize("hasAuthority('/usuariopermissao/list')")
	public ResponseEntity<List<UsuarioPermissao>> getAll() {
		return service.getAll();
	}
	
	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('/usuariopermissao/list')")
	public ResponseEntity<UsuarioPermissao> findById(@PathVariable() Long id) {
		return service.findById(id);
	}
	
	@PostMapping
	@PreAuthorize("hasAuthority('/usuariopermissao/new')")
	public ResponseEntity<UsuarioPermissao> create(@RequestBody UsuarioPermissao usuarioPermissao) {
		return service.create(usuarioPermissao);
	}
	
	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('/usuariopermissao/edit')")
	public ResponseEntity<UsuarioPermissao> update(@RequestBody UsuarioPermissao usuarioPermissao, @PathVariable() Long id) {
		return service.update(usuarioPermissao, id);
	}
	
	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('/usuariopermissao/delete')")
	public ResponseEntity<UsuarioPermissao> delete(@PathVariable() Long id) {
		return service.delete(id);
	}
}

