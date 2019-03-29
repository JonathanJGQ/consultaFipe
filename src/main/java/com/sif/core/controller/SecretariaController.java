package com.sif.core.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.Normalizer;
import java.util.List;

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
import com.sif.core.service.OrgaoService;
import com.sif.core.service.SecretariaService;
import com.sif.core.service.UfService;
import com.sif.model.Cidade;
import com.sif.model.Endereco;
import com.sif.model.Orgao;
import com.sif.model.Secretaria;
import com.sif.model.Uf;
import com.sif.model.custom.SecretariaCustomDTO;

@RestController
@RequestMapping("/secretaria")
public class SecretariaController{
	
	@Autowired
	SecretariaService secretariaService;
	
	@Autowired
	OrgaoService orgaoService;
	
	@Autowired
	UfService ufService;
	
	@Autowired
	CidadeService cidadeService;
	
	@Autowired
	EnderecoService enderecoService;
	
	@GetMapping
	@PreAuthorize("hasAuthority('/secretaria/list')")
	public Page<Secretaria> getAll(Pageable pageable, Secretaria secretaria) {
		return secretariaService.getAll(pageable, secretaria);
	}
	
	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('/secretaria/list')")
	public ResponseEntity<SecretariaCustomDTO> findById(@PathVariable() Long id) {
		return secretariaService.findById(id);
	}
	
	@PostMapping
	@PreAuthorize("hasAuthority('/secretaria/new')")
	public ResponseEntity<Secretaria> create(@RequestBody SecretariaCustomDTO secretaria) {
		return secretariaService.create(secretaria);
	}
	
	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('/secretaria/edit')")
	public ResponseEntity<Secretaria> update(@RequestBody SecretariaCustomDTO secretaria, @PathVariable() Long id) {
		return secretariaService.update(secretaria, id);
	}
	
	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('/secretaria/delete')")
	public ResponseEntity<Secretaria> delete(@PathVariable() Long id) {
		return secretariaService.delete(id);
	}
	
	@PostMapping("/importfile")
	@PreAuthorize("hasAuthority('/secretaria/new')")
	public ResponseEntity<String> importSecretaria(@RequestParam("file") MultipartFile multiPartFile){

		JSONObject jsonRetorno = new JSONObject();
		JSONObject jsonSucesso = new JSONObject();
		
		int status = 200;
		JSONArray jsonArray = new JSONArray();
		
		try {
			File file = new File(multiPartFile.getName());
			FileUtils.writeByteArrayToFile(file, multiPartFile.getBytes());
			
			FileInputStream fstream = new FileInputStream(file);
			BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
			
			String strLine;
			
			int firstLine = -1;
			while ((strLine = br.readLine()) != null)   {
				JSONObject jsonObject = new JSONObject();
				
				firstLine = firstLine + 1;
				if(firstLine == 0) {
					continue;
				}
				
				String[] campos = strLine.split(";");
				
				if(campos.length != 10) {
					jsonObject.put("linha", "erro na linha" + firstLine);
					jsonObject.put("mensagem", "nenhum valor pode estar vazio");
					
					jsonArray.put(jsonObject);
					continue;
				}
				
				String gestor = campos[0];
				String codigo = campos[1];
				String descricao = campos[2];
				String sigla = campos[3];
				String ufSigla = campos[4];
				String cidadeNome = campos[5];
				String bairro = campos[6];
				String logradouro = campos[7];
				String numero = campos[8];
				String telefone = campos[9];
				
				if(gestor.isEmpty()
					|| codigo.isEmpty()
					|| descricao.isEmpty()
					|| sigla.isEmpty()
					|| ufSigla.isEmpty()
					|| cidadeNome.isEmpty()
					|| bairro.isEmpty()
					|| logradouro.isEmpty()
					|| numero.isEmpty()
					|| telefone.isEmpty()) {
				
					jsonObject.put("linha", "erro na linha" + firstLine);
					jsonObject.put("mensagem", "Nenhum valor pode estar vazio");
					
					jsonArray.put(jsonObject);
					continue;
					
				}
				
				Secretaria secretaria = new Secretaria();
				Endereco endereco = new Endereco();
				
				Secretaria secretariaSalva = secretariaService.findByCodigo(campos[1]);
				
		    	if(secretariaSalva != null) {
		    		secretaria.setId(secretariaSalva.getId());
		    		endereco.setId(secretariaSalva.getEndereco().getId());
		    	}
		    	
				
				Long idOrgao = Long.parseLong(gestor);
				Orgao orgao = orgaoService.findOrgaoById(idOrgao);
				
				if(orgao == null) {
					jsonObject.put("linha", "erro na linha" + firstLine);
					jsonObject.put("mensagem", "Orgao inexistente");
					
					jsonArray.put(jsonObject);
					continue;
				}
				
				secretaria.setOrgao(orgao);
				secretaria.setCodigo(codigo);
				secretaria.setDescricao(descricao);
				secretaria.setSigla(sigla);
				secretaria.setStatus(true);
				
				Uf ufExists = ufService.findUfBySigla(ufSigla);
				
				if(ufExists == null) {
					jsonObject.put("linha", "erro na linha" + firstLine);
					jsonObject.put("mensagem", "UF inexistente");
					
					jsonArray.put(jsonObject);
					continue;
				}
				
				cidadeNome = Normalizer.normalize(cidadeNome, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
				cidadeNome = cidadeNome.toUpperCase();
				List<Cidade> cidades = cidadeService.findByNome(cidadeNome);
				
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
				
				endereco.setBairro(bairro);
				endereco.setCidade(cidadeExists);
				endereco.setLogradouro(logradouro);
				endereco.setCelular(telefone);
				endereco.setNumero(numero);
				
				Endereco enderecoSave = enderecoService.create(endereco).getBody();
				
				secretaria.setEndereco(enderecoSave);
				secretaria.setStatus(true);
				
				secretariaService.save(secretaria);
				
				
			}
			
			jsonSucesso.put("title", "Sucesso");
			jsonSucesso.put("messages", jsonArray);
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
			jsonRetorno.put("title", "Erro");
			jsonRetorno.put("message", "Não foi possivel processar este arquivo");

			status = 500;
			
			return ResponseEntity.status(status).body(jsonRetorno.toString());
		}
		
		return ResponseEntity.status(status).body(jsonSucesso.toString());
	}
}

