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

import com.sif.core.service.AverbadoraService;
import com.sif.core.service.CidadeService;
import com.sif.core.service.ConsignatariaService;
import com.sif.core.service.EnderecoService;
import com.sif.core.service.UfService;
import com.sif.model.Averbadora;
import com.sif.model.Cidade;
import com.sif.model.Consignataria;
import com.sif.model.Endereco;
import com.sif.model.Uf;
import com.sif.model.custom.ConsignatariaCustomDTO;

@RestController
@RequestMapping("/consignataria")
public class ConsignatariaController{
	
	@Autowired
	ConsignatariaService service;
	
	@Autowired
	AverbadoraService averbadoraService;
	
	@Autowired
	UfService ufService;
	
	@Autowired
	CidadeService cidadeService;
	
	@Autowired
	EnderecoService enderecoService;
	
	@GetMapping
	@PreAuthorize("hasAuthority('/consignataria/list')")
	public Page<Consignataria> getAll(Pageable pageable, Consignataria consignataria) {
		return service.getAll(pageable, consignataria);
	}
	
	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('/consignataria/list')")
	public ResponseEntity<ConsignatariaCustomDTO> findById(@PathVariable() Long id) {
		return service.findById(id);
	}
	
	@GetMapping("/view/{id}")
	@PreAuthorize("hasAuthority('/consignataria/view')")
	public ResponseEntity<ConsignatariaCustomDTO> viewById(@PathVariable() Long id) {
		return service.findById(id);
	}
	
	@PostMapping
	@PreAuthorize("hasAuthority('/consignataria/new')")
	public ResponseEntity<Consignataria> create(@RequestBody ConsignatariaCustomDTO consignataria) {
		return service.create(consignataria);
	}
	
	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('/consignataria/edit')")
	public ResponseEntity<Consignataria> update(@RequestBody ConsignatariaCustomDTO consignataria, @PathVariable() Long id) {
		return service.update(consignataria, id);
	}
	
	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('/consignataria/delete')")
	public ResponseEntity<Consignataria> delete(@PathVariable() Long id) {
		return service.delete(id);
	}
	
	@PostMapping("/blockDisblock/{id}")
	@PreAuthorize("hasAuthority('/consignataria/block')")
	public ResponseEntity<String> blockDisblock(@PathVariable("id") Long id) {
		return service.blockDisblock(id);
	}
	
	@PostMapping("/importfile")
	@PreAuthorize("hasAuthority('/consignataria/new')")
	public ResponseEntity<String> importConsignataria(@RequestParam("file") MultipartFile multiPartFile){
		
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
				
				if(campos.length != 16) {
					jsonObject.put("linha", "erro na linha: " + (firstLine + 1));
					jsonObject.put("mensagem", "nenhum valor pode estar vazio");
					
					jsonArray.put(jsonObject);
					continue;
				}
				
				String averbadora = campos[0];
				String codigo = campos[1];
				String nome = campos[2];
				String cnpj = campos[3];
				String email = campos[4];
				String max_parcelas = campos[5];
				String max_parcelas_compra = campos[6];
				String min_parcelas_pagas = campos[7];
				String carencia = campos[8];
				String dia_fechamento = campos[9];
				String ufSigla = campos[10];
				String cidadeNome = campos[11];
				String bairro = campos[12];
				String logradouro = campos[13];
				String numero = campos[14];
				String telefone = campos[15];
				
				if(averbadora.isEmpty()
					|| codigo.isEmpty()
					|| nome.isEmpty()
					|| cnpj.isEmpty()
					|| email.isEmpty()
					|| max_parcelas.isEmpty()
					|| max_parcelas_compra.isEmpty()
					|| min_parcelas_pagas.isEmpty()
					|| carencia.isEmpty()
					|| dia_fechamento.isEmpty()
					|| ufSigla.isEmpty()
					|| cidadeNome.isEmpty()
					|| bairro.isEmpty()
					|| logradouro.isEmpty()
					|| numero.isEmpty()
					|| telefone.isEmpty()) {
				
					jsonObject.put("linha", "erro na linha: " + (firstLine + 1));
					jsonObject.put("mensagem", "Nenhum valor pode estar vazio");
					
					jsonArray.put(jsonObject);
					continue;
					
				}
				
				Consignataria consignataria = service.findAtivaByCNPJ(cnpj);
				Endereco endereco = new Endereco();
				
				if(consignataria == null) {
					consignataria = new Consignataria();
					consignataria.setCnpj(cnpj);
					
					Consignataria consigByEmail = service.findAtivaByEmail(email);
					
					if(consigByEmail != null ) {
						jsonObject.put("linha", "erro na linha: " + (firstLine + 1));
						jsonObject.put("mensagem", "Já existe uma consignataria com este email");
						
						jsonArray.put(jsonObject);
						continue;
					}
				
				} else {
					
					endereco = consignataria.getEndereco();
					
					Consignataria consigByEmail = service.findAtivaByEmail(email);
					
					if(consigByEmail != null) {
						if(!consigByEmail.getId().equals(consignataria.getId())) {
							jsonObject.put("linha", "erro na linha: " + (firstLine + 1));
							jsonObject.put("mensagem", "Já existe uma consignataria diferente desta com este email");
							
							jsonArray.put(jsonObject);
							continue;
						}
						
					}
					
				}
				
				Long idAverbadora = Long.parseLong(averbadora);
				Averbadora averbadoraExists = averbadoraService.findAverbadoraById(idAverbadora);
				
				if(averbadoraExists == null) {
					jsonObject.put("linha", "erro na linha: " + (firstLine + 1));
					jsonObject.put("mensagem", "Averbadora inexistente");
					
					jsonArray.put(jsonObject);
					continue;
				}
				
				consignataria.setAverbadora(averbadoraExists);
				consignataria.setCodigo(codigo);
				consignataria.setNome(nome);
				consignataria.setEmail(email);
				
				Long maxParcela = Long.parseLong(max_parcelas);
				consignataria.setMaximoParcela(maxParcela);
				
				Long maxParcelaCompra = Long.parseLong(max_parcelas_compra);
				consignataria.setMaximoParcelaCompra(maxParcelaCompra);
				
				Long minParcelaPaga = Long.parseLong(min_parcelas_pagas);
				consignataria.setMinimoParcelaPagas(minParcelaPaga);
				
				Integer opera = Integer.parseInt(carencia);
				consignataria.setOperaComCarencia(opera);
				
				Integer diaFechamento = Integer.parseInt(dia_fechamento);
				consignataria.setDiaFechamento(diaFechamento);
				
				consignataria.setStatus(1);
				
				Uf ufExists = ufService.findUfBySigla(ufSigla);
				
				if(ufExists == null) {
					jsonObject.put("linha", "erro na linha: " + (firstLine + 1));
					jsonObject.put("mensagem", "UF inexistente");
					
					jsonArray.put(jsonObject);
					continue;
				}
				
				cidadeNome = Normalizer.normalize(cidadeNome, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
				cidadeNome = cidadeNome.toUpperCase();
				List<Cidade> cidades = cidadeService.findByNome(cidadeNome);
				
				if(cidades == null
					|| cidades.isEmpty()) {
					
					jsonObject.put("linha", "erro na linha: " + (firstLine + 1));
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
					jsonObject.put("linha", "erro na linha: " + (firstLine + 1));
					jsonObject.put("mensagem", "Esta cidade não pertence a este UF");
					
					jsonArray.put(jsonObject);
					continue;
				}
				
				if(consignataria.getEndereco() == null) {
					endereco.setBairro(bairro);
					endereco.setCidade(cidadeExists);
					endereco.setLogradouro(logradouro);
					endereco.setCelular(telefone);
					endereco.setNumero(numero);
					
					Endereco enderecoSave = enderecoService.create(endereco).getBody();
					consignataria.setEndereco(enderecoSave);
				} else {
					consignataria.getEndereco().setBairro(bairro);
					consignataria.getEndereco().setCidade(cidadeExists);
					consignataria.getEndereco().setLogradouro(logradouro);
					consignataria.getEndereco().setCelular(telefone);
					consignataria.getEndereco().setNumero(numero);
					
					Endereco enderecoSave = enderecoService.create(consignataria.getEndereco()).getBody();
					consignataria.setEndereco(enderecoSave);
				}
				
				service.save(consignataria);
				
			}
			
			jsonSucesso.put("title", "Sucesso");
			jsonSucesso.put("messages", jsonArray);
			br.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			jsonRetorno.put("title", "Erro");
			jsonRetorno.put("message", "Não foi possivel processar este arquivo");

			status = 500;
			
			return ResponseEntity.status(status).body(jsonRetorno.toString());
		}
		
		return ResponseEntity.status(status).body(jsonSucesso.toString());
		
	}
}

