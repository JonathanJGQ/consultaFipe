package com.sif.core.controller;

import org.json.JSONArray;
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

import com.sif.core.service.ModuloService;

import com.sif.model.Modulo;

@RestController
@RequestMapping("/modulo")
public class ModuloController {

	@Autowired
	ModuloService moduloService;

	@GetMapping
	@PreAuthorize("hasAuthority('/modulo/list')")
	public Page<Modulo> getAll(Pageable pageable, Modulo modulo) {
		return moduloService.getAll(pageable, modulo);
	}
	
	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('/modulo/list')")
	public ResponseEntity<Modulo> findById(@PathVariable() Long id) {
		return moduloService.findById(id);
	}
	
	@PostMapping
	@PreAuthorize("hasAuthority('/modulo/new')")
	public ResponseEntity<Modulo> create(@RequestBody Modulo modulo) {
		return moduloService.create(modulo);
	}
	
	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('/modulo/edit')")
	public ResponseEntity<Modulo> update(@RequestBody Modulo modulo, @PathVariable() Long id) {
		return moduloService.update(modulo, id);
	}
	
	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('/modulo/delete')")
	public ResponseEntity<Modulo> delete(@PathVariable() Long id) {
		return moduloService.delete(id);
	}
	
	@GetMapping("/getOrderedMenu")
	public ResponseEntity<String> getMenu() {
		return moduloService.getMenu();
	}
	
	@PostMapping("/saveOrderedMenu")
	public ResponseEntity<String> salvarOrdenacaoModulos(@RequestBody() String jsonObject) {
		
		JSONArray json = new JSONArray(jsonObject);
		
		return moduloService.saveMenu(json);
	}
	
}
