package com.sif.core.controller;

import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sif.core.exception.GenericException;
import com.sif.core.service.FaturaService;
import com.sif.model.Fatura;
import com.sif.model.custom.FaturaCustomDTO;
import com.sif.model.custom.FaturaListDTO;

@RestController
@RequestMapping("/financeiro")
public class FaturaController {

	@Autowired
	FaturaService faturaService;
	
	@GetMapping
	@PreAuthorize("hasAuthority('/financeiro/list')")
	public Page<FaturaListDTO> getAll(Pageable pageable, Fatura fatura) {
		return faturaService.getAll(pageable, fatura);
	}
	
	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('/financeiro/list')")
	public ResponseEntity<FaturaCustomDTO> findById(@PathVariable() Long id) {
		return faturaService.findById(id);
	}
	
	@GetMapping("/pdf/{id}")
	@PreAuthorize("hasAuthority('/financeiro/list')")
	public void getPDF(@PathVariable("id") Long id, HttpServletResponse response) {
		
		Fatura fatura = faturaService.get(id);
		
		if(fatura == null) {
			throw new GenericException("Erro", "Esta fatura não existe");
		}
		
		try {
			response.setContentType("application/pdf");
			response.setHeader("Content-Disposition", "attachment; filename=fatura.pdf");
			faturaService.gerarPDF(id, response.getOutputStream());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@GetMapping("/xls/{id}")
	@PreAuthorize("hasAuthority('/financeiro/list')")
	public void getXLS(@PathVariable("id") Long id, HttpServletResponse response) {
		
		Fatura fatura = faturaService.get(id);
		
		if(fatura == null) {
			throw new GenericException("Erro", "Esta fatura não existe");
		}
		
		try {
			response.setContentType("application/vnd.ms-excel");
			response.setHeader("Content-Disposition", "attachment; filename=fatura.xls");
			
			ServletOutputStream fileOut = response.getOutputStream();
			faturaService.gerarExcel(id, fileOut);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@GetMapping("/csv/{id}")
	@PreAuthorize("hasAuthority('/financeiro/list')")
	public void getCSV(@PathVariable("id") Long id, HttpServletResponse response) {
		
		Fatura fatura = faturaService.get(id);
		
		if(fatura == null) {
			throw new GenericException("Erro", "Esta fatura não existe");
		}
		
		try {
			response.setContentType("text/csv");
			response.setHeader("Content-Disposition", "attachment; filename=fatura.csv");
			
			byte[] bytes = faturaService.gerarCSV(id);
			
			ServletOutputStream out = response.getOutputStream();
			out.write(bytes);
			out.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@PutMapping("/baixar/{id}")
	@PreAuthorize("hasAuthority('/financeiro/baixar')")
	public ResponseEntity<String> baixarFatura(@PathVariable("id") Long id, HttpServletResponse response, @RequestBody String motivo) {
		JSONObject json = new JSONObject(motivo);
		
		return faturaService.baixarFatura(id, json.get("motivo").toString());
	}
}
