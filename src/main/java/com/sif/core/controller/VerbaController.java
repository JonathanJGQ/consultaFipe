package com.sif.core.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sif.core.service.ConsignatariaService;
import com.sif.core.service.MargemService;
import com.sif.core.service.VerbaService;
import com.sif.core.utils.Funcoes;
import com.sif.model.Consignataria;
import com.sif.model.Margem;
import com.sif.model.Verba;
import com.sif.model.custom.VerbaCustomDTO;

@RestController
@RequestMapping("/verba")
public class VerbaController {

	@Autowired
	VerbaService verbaService;
	
	@Autowired
	ConsignatariaService consignatariaService;
	
	@Autowired
	MargemService margemService;
	
	@GetMapping
	@PreAuthorize("hasAuthority('/verba/list')")
	public Page<Verba> getAll(Pageable pageable, Verba verba) {
		return verbaService.getAll(pageable, verba);
	}
	
	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('/verba/list')")
	public ResponseEntity<VerbaCustomDTO> findById(@PathVariable() Long id) {
		return verbaService.findById(id);
	}
	
	@PostMapping
	@PreAuthorize("hasAuthority('/verba/new')")
	public ResponseEntity<Verba> create(@RequestBody VerbaCustomDTO verba) {
		return verbaService.create(verba);
	}
	
	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('/verba/edit')")
	public ResponseEntity<Verba> update(@RequestBody VerbaCustomDTO verba, @PathVariable() Long id) {
		return verbaService.update(verba, id);
	}
	
	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('/verba/delete')")
	public ResponseEntity<Verba> delete(@PathVariable() Long id) {
		return verbaService.delete(id);
	}
	
	@PostMapping(value="/importfile")
	@PreAuthorize("hasAuthority('/verba/new')")
	public ResponseEntity<String> importVerba(@RequestParam("file") MultipartFile multiPartFile, HttpServletResponse response){
		
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonSucesso = new JSONObject();
		
		try {
			File file = new File(multiPartFile.getName());
			FileUtils.writeByteArrayToFile(file, multiPartFile.getBytes());
			
			FileInputStream fstream = new FileInputStream(file);
			BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
			
			String strLine;
		    
		    int firstLine = -1;
		    while ((strLine = br.readLine()) != null) {
		    	
		    	firstLine = firstLine + 1;
		    	if(firstLine == 0) {
					continue;
				}
		    	
		    	JSONObject jsonObject  = new JSONObject();
		    	
		    	String[] campos = strLine.split(";");
		    	
		    	if(campos.length != 7) {
					jsonObject.put("linha", "erro na linha: " + firstLine);
					jsonObject.put("mensagem", "nenhum valor pode estar vazio");
					
					jsonArray.put(jsonObject);
					continue;
				}
		    	
		    	String margemIncidente = campos[0];
		    	String codigo = campos[1];
		    	String codigoConsignataria = campos[2];
		    	String especie = campos[3];
		    	String descricao = campos[4];
		    	String ciclo = campos[5];
		    	String maxContrato = campos[6];
		    	
		    	if(maxContrato.isEmpty()
						|| margemIncidente.isEmpty()
						|| codigo.isEmpty()
						|| codigoConsignataria.isEmpty()
						|| especie.isEmpty()
						|| descricao.isEmpty()
						|| ciclo.isEmpty()) {
					
						jsonObject.put("linha", "erro na linha: " + firstLine);
						jsonObject.put("mensagem", "Nenhum valor pode estar vazio");
						
						jsonArray.put(jsonObject);
						continue;
						
					}
		    	
		    	Verba verba = verbaService.findByCodigo(codigo);
		    	if(verba == null) {
		    		verba = new Verba();
		    	}
		    	
		    	
		    	verba.setCiclo(ciclo);
		    	verba.setCodigo(codigo);
		    	
		    	Consignataria consignataria = consignatariaService.findByCodigo(codigoConsignataria);
		    	
		    	if(consignataria == null) {
		    		jsonObject.put("linha", "erro na linha: " + firstLine);
					jsonObject.put("mensagem", "Consignatária não encontrada");
					
					jsonArray.put(jsonObject);
					continue;
		    	}
		    	
		    	verba.setConsignataria(consignataria);
		    	verba.setDescricao(descricao);
		    	verba.setEspecie(especie);
		    	
		    	Margem margem = margemService.findByNome(margemIncidente);
		    	
		    	if(margem == null) {
		    		jsonObject.put("linha", "erro na linha: " + firstLine);
					jsonObject.put("mensagem", "Margem não encontrada");
					
					jsonArray.put(jsonObject);
					continue;
		    	}
		    	
		    	verba.setMargem(margem);
		    	verba.setMaximoContratoVerba(new BigDecimal(maxContrato));
		    	verba.setStatus(true);
				
				verbaService.save(verba);
				
		    }
		    jsonSucesso.put("title", "Sucesso");
			jsonSucesso.put("messages", jsonArray);
		}
		catch(Exception e) {
			return ResponseEntity.badRequest().body(Funcoes.jsonMessage("Erro", "Erro ao ler arquivo!", "400"));
		}
	
		return ResponseEntity.ok().body(jsonSucesso.toString());
		
	}
}
