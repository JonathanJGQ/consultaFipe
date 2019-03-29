package com.sif.core.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.Normalizer;
import java.util.List;

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

import com.sif.core.service.CidadeService;
import com.sif.core.service.EnderecoService;
import com.sif.core.service.UfService;
import com.sif.core.service.UnidadeAdministrativaService;
import com.sif.core.utils.Funcoes;
import com.sif.model.Cidade;
import com.sif.model.Endereco;
import com.sif.model.Secretaria;
import com.sif.model.Uf;
import com.sif.model.UnidadeAdministrativa;
import com.sif.model.custom.UnidadeAdministrativaCustomDTO;
import com.sif.repository.SecretariaRepository;

@RestController
@RequestMapping("/unidadmin")
public class UnidadeAdministrativaController{
	
	@Autowired
	UnidadeAdministrativaService service;

	@Autowired
	SecretariaRepository secretariaRepository;
	
	@Autowired
	CidadeService cidadeService;
	
	@Autowired
	UfService ufService;
	
	@Autowired
	EnderecoService enderecoService;
	
	@GetMapping
	@PreAuthorize("hasAuthority('/unidadmin/list')")
	public Page<UnidadeAdministrativa> getAll(Pageable pageable, UnidadeAdministrativa unidadmin) {
		return service.getAll(pageable, unidadmin);
	}
	
	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('/unidadmin/list')")
	public ResponseEntity<UnidadeAdministrativaCustomDTO> findById(@PathVariable() Long id) {
		return service.findById(id);
	}
	
	@PostMapping
	@PreAuthorize("hasAuthority('/unidadmin/new')")
	public ResponseEntity<UnidadeAdministrativa> create(@RequestBody UnidadeAdministrativaCustomDTO unidadeAdministrativa) {
		return service.create(unidadeAdministrativa);
	}
	
	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('/unidadmin/edit')")
	public ResponseEntity<UnidadeAdministrativa> update(@RequestBody UnidadeAdministrativaCustomDTO unidadeAdministrativa, @PathVariable() Long id) {
		return service.update(unidadeAdministrativa, id);
	}
	
	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('/unidadmin/delete')")
	public ResponseEntity<UnidadeAdministrativa> delete(@PathVariable() Long id) {
		return service.delete(id);
	}
	
	@PostMapping(value="/importfile")
	@PreAuthorize("hasAuthority('/unidadmin/new')")
	public ResponseEntity<String> importUnidadeAdministrativa(@RequestParam("file") MultipartFile multiPartFile, HttpServletResponse response){
		
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonSucesso = new JSONObject();
		
		try {
			File file = new File(multiPartFile.getName());
			FileUtils.writeByteArrayToFile(file, multiPartFile.getBytes());
			
			FileInputStream fstream = new FileInputStream(file);
			BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
			
			String strLine;
			
			OutputStream downloadStream = response.getOutputStream();
		    
		    int totalBytes = 0;
		    
		    int firstLine = -1;
		    while ((strLine = br.readLine()) != null) {
		    	
		    	firstLine = firstLine + 1;
		    	if(firstLine == 0) {
					continue;
				}
		    	
		    	JSONObject jsonObject  = new JSONObject();
		    	
		    	String[] campos = strLine.split(";");
		    	
		    	if(campos.length != 9) {
					jsonObject.put("linha", "erro na linha" + firstLine);
					jsonObject.put("mensagem", "nenhum valor pode estar vazio");
					
					jsonArray.put(jsonObject);
					continue;
				}
		    	
		    	if(campos[0].isEmpty()
						|| campos[1].isEmpty()
						|| campos[2].isEmpty()
						|| campos[3].isEmpty()
						|| campos[4].isEmpty()
						|| campos[5].isEmpty()
						|| campos[6].isEmpty()
						|| campos[7].isEmpty()
						|| campos[8].isEmpty()) {
					
						jsonObject.put("linha", "erro na linha" + firstLine);
						jsonObject.put("mensagem", "Nenhum valor pode estar vazio");
						
						jsonArray.put(jsonObject);
						continue;
						
					}
		    	
		    	UnidadeAdministrativa unidade = new UnidadeAdministrativa();
		    	Endereco endereco = new Endereco();

		    	UnidadeAdministrativa unidSalva = service.findByCodigo(campos[0]);
		    	
		    	if(unidSalva != null) {
		    		unidade.setId(unidSalva.getId());
		    		endereco.setId(unidSalva.getEndereco().getId());
		    	}
		    	
		    	unidade.setCodigo(campos[0]);
		    	unidade.setDescricao(campos[1]);
		    	unidade.setStatus(true);
		    	
		    	Secretaria secretaria = secretariaRepository.findByCodigo(campos[2]);
		    	
		    	if(secretaria == null) {
		    		jsonObject.put("linha", "erro na linha" + firstLine);
					jsonObject.put("mensagem", "Secretaria não foi encontrada");
					
					jsonArray.put(jsonObject);
					continue;
		    	}
		    	
		    	Uf ufExists = ufService.findUfBySigla(campos[3]);
		    	
		    	if(ufExists == null) {
					jsonObject.put("linha", "erro na linha" + firstLine);
					jsonObject.put("mensagem", "UF inexistente");
					
					jsonArray.put(jsonObject);
					continue;
				}
		    	
		    	campos[4] = Normalizer.normalize(campos[4], Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
		    	campos[4] = campos[4].toUpperCase();
				List<Cidade> cidades = cidadeService.findByNome(campos[4]);
				
				if(cidades == null
					|| cidades.isEmpty()) {
					
					jsonObject.put("linha", "erro na linha" + firstLine);
					jsonObject.put("mensagem", "Cidade inexistente");
					
					jsonArray.put(jsonObject);
					continue;
				}
				
				Cidade cidadeExists = null;
				for(Cidade cidade : cidades) {
					if(cidade.getUf().getId() == ufExists.getId()) {
						cidadeExists = cidade;
					}
				}
				
				if(cidadeExists == null) {
					jsonObject.put("linha", "erro na linha" + firstLine);
					jsonObject.put("mensagem", "Esta cidade não pertence a este UF");
					
					jsonArray.put(jsonObject);
					continue;
				}
				
				endereco.setBairro(campos[5]);
				endereco.setCidade(cidadeExists);
				endereco.setLogradouro(campos[6]);
				endereco.setNumero(campos[7]);
				endereco.setCelular(campos[8]);
				
				Endereco enderecoSave = enderecoService.create(endereco).getBody();
				
				unidade.setEndereco(enderecoSave);
				unidade.setSecretaria(secretaria);
				
				service.save(unidade);
				
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

