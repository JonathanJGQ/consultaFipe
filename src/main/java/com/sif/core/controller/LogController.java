package com.sif.core.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sif.core.service.LogService;
import com.sif.model.Log;

@RestController
@RequestMapping("/log")
public class LogController {
	
	@Autowired
	LogService service;
	
	@GetMapping
	@PreAuthorize("hasAuthority('/log/list')")
	public Page<Log> getAll(Pageable pageable, Log log) {
		return service.getAll(pageable, log);
	}
	
}
