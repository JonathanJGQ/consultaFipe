package com.sif.core.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import com.sif.core.service.FuncionarioService;
import com.sif.core.service.UfService;
import com.sif.core.utils.Funcoes;
import com.sif.model.Cidade;
import com.sif.model.Endereco;
import com.sif.model.Funcionario;
import com.sif.model.Uf;
import com.sif.model.UnidadeAdministrativa;
import com.sif.model.custom.FuncionarioCustomDTO;
import com.sif.model.custom.FuncionarioMargemCustomDTO;
import com.sif.repository.FuncionarioRepository;
import com.sif.repository.UnidadeAdministrativaRepository;

@RestController
@RequestMapping("/funcionario")
public class FuncionarioController{
	
	@Autowired
	FuncionarioService service;
	
	@Autowired
	UfService ufService;
	
	@Autowired
	CidadeService cidadeService;
	
	@Autowired
	EnderecoService enderecoService;
	
	@Autowired
	FuncionarioRepository funcionarioRepository;
	
	@Autowired
	UnidadeAdministrativaRepository unidadeAdministrativaRepository;
	
	@GetMapping
	@PreAuthorize("hasAuthority('/funcionario/list')")
	public Page<Funcionario> getAll(Pageable pageable, Funcionario cliente) {
		return service.getAll(pageable, cliente);
	}
	
	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('/funcionario/list')")
	public ResponseEntity<FuncionarioCustomDTO> findById(@PathVariable() Long id) {
		return service.findById(id);
	}
	
	@GetMapping("/view/{id}")
	@PreAuthorize("hasAuthority('/funcionario/view')")
	public ResponseEntity<FuncionarioMargemCustomDTO> viewFuncionario(@PathVariable() Long id) {
		return service.viewFuncionario(id);
	}
	
	@PostMapping
	@PreAuthorize("hasAuthority('/funcionario/new')")
	public ResponseEntity<Funcionario> create(@RequestBody FuncionarioCustomDTO cliente) {
		return service.create(cliente);
	}
	
	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('/funcionario/edit')")
	public ResponseEntity<Funcionario> update(@RequestBody FuncionarioCustomDTO cliente, @PathVariable() Long id) {
		return service.update(cliente, id);
	}
	
	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('/funcionario/delete')")
	public ResponseEntity<Funcionario> delete(@PathVariable() Long id) {
		return service.delete(id);
	}
	
	@PostMapping(value="/importfile")
	@PreAuthorize("hasAuthority('/funcionario/new')")
	public ResponseEntity<String> importFuncionarios(@RequestParam("file") MultipartFile multiPartFile, HttpServletResponse response){
		
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonSucesso = new JSONObject();
		
		boolean calculaMargem = false;
		
		List<Funcionario> listaFuncionario = new ArrayList<Funcionario>();
		
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
		    	
		    	if(campos.length < 21) {
					jsonObject.put("linha", "erro na linha: " + (firstLine +1));
					jsonObject.put("mensagem", "nenhum valor pode estar vazio");
					
					jsonArray.put(jsonObject);
					continue;
				}
		    	
		    	String numFuncional = campos[0];
		    	String numVinculo = campos[1];
		    	String numPensionista = campos[2];
		    	String cpf = campos[3];
		    	String nome = campos[4];
		    	String setor = campos[5];
		    	String numRg = campos[6];
		    	String orgaoRg = campos[7];
		    	String estadoCivil = campos[8];
		    	String sexo = campos[9];
		    	String dataNascimento = campos[10];
		    	String logradouro = campos[11];
		    	String cep = campos[12];
		    	String uf = campos[13];
		    	String cidade = campos[14];
		    	String bairro = campos[15];
		    	String numero = campos[16];
		    	String telefone = campos[17];
		    	String dataAdmissao = campos[18];
		    	String mae = campos[19];
		    	String pai = campos[20];
		    	String sbc = "";
		    	try {
		    		sbc = campos[21];
		    	}
		    	catch(Exception e) {}
		    	
		    	if(numFuncional.isEmpty()
						|| numVinculo.isEmpty()
						|| numPensionista.isEmpty()
						|| cpf.isEmpty()
						|| nome.isEmpty()
						|| setor.isEmpty()
						|| numRg.isEmpty()
						|| orgaoRg.isEmpty()
						|| estadoCivil.isEmpty()
						|| sexo.isEmpty()
						|| dataNascimento.isEmpty()
						|| logradouro.isEmpty()
						|| cep.isEmpty()
						|| uf.isEmpty()
						|| cidade.isEmpty()
						|| bairro.isEmpty()
						|| numero.isEmpty()
						|| telefone.isEmpty()
						|| dataAdmissao.isEmpty()
						|| mae.isEmpty()
						|| pai.isEmpty()) {
					
						jsonObject.put("linha", "erro na linha: " + (firstLine + 1));
						jsonObject.put("mensagem", "Nenhum valor pode estar vazio");
						
						jsonArray.put(jsonObject);
						continue;
						
					}
		    	if(!sbc.isEmpty()) {
		    		calculaMargem = true;
		    	}
		    	
		    	Funcionario funcionario = funcionarioRepository.findForMargemInsert(numFuncional, numVinculo, numPensionista, cpf);
		    	
		    	UnidadeAdministrativa unidAdmin = unidadeAdministrativaRepository.findByCodigo(setor);
		    	
		    	if(unidAdmin == null) {
		    		jsonObject.put("linha", "erro na linha: " + (firstLine + 1));
					jsonObject.put("mensagem", "Unidade Administrativa não encontrada");
					
					jsonArray.put(jsonObject);
					continue;
		    	}
		    	
		    	Endereco endereco = new Endereco();
		    	if(funcionario == null) {
		    		funcionario = new Funcionario();
		    	}
		    	else {
		    		if(funcionario.getEndereco() != null) {
		    			endereco = funcionario.getEndereco();
		    		}
		    	}
		    	
		    	SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
		    	
		    	Uf ufExists = ufService.findUfBySigla(uf);
				
				if(ufExists == null) {
					jsonObject.put("linha", "erro na linha: " + (firstLine + 1));
					jsonObject.put("mensagem", "UF inexistente");
					
					jsonArray.put(jsonObject);
					continue;
				}
				
				cidade = Normalizer.normalize(cidade, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
				cidade = cidade.toUpperCase();
				List<Cidade> cidades = cidadeService.findByNome(cidade);
				
				if(cidades == null
					|| cidades.isEmpty()) {
					
					jsonObject.put("linha", "erro na linha: " + (firstLine + 1));
					jsonObject.put("mensagem", "Cidade inexistente");
					
					jsonArray.put(jsonObject);
					continue;
				}
				
				Cidade cidadeExists = null;
				for(Cidade cidadeEndereco : cidades) {
					if(cidadeEndereco.getUf().getId() == ufExists.getId()) {
						cidadeExists = cidadeEndereco;
					}
				}
				
				if(cidadeExists == null) {
					jsonObject.put("linha", "erro na linha: " + (firstLine + 1));
					jsonObject.put("mensagem", "Esta cidade não pertence a este UF");
					
					jsonArray.put(jsonObject);
					continue;
				}
				
		    	endereco.setBairro(bairro);
//		    	endereco.setCelular(celular);
		    	endereco.setCep(cep);
		    	endereco.setCidade(cidadeExists);
//		    	endereco.setComplemento(complemento);
//		    	endereco.setDdd(ddd);
		    	endereco.setLogradouro(logradouro);
		    	endereco.setNumero(numero);
		    	endereco.setStatus(true);
		    	endereco.setTelefone(telefone);
		    	
		    	funcionario.setCpf(cpf);
		    	
		    	try {
		    		funcionario.setDataAdmissao(sdf.parse(dataAdmissao));
//			    	funcionario.setDataAposentadoria(dataAposentadoria);
			    	funcionario.setDataNascimento(sdf.parse(dataNascimento));
//			    	funcionario.setDataVacancia(dataVacancia);
		    	}
		    	catch(Exception e) {
		    		jsonObject.put("linha", "erro na linha: " + (firstLine + 1));
					jsonObject.put("mensagem", "Erro na conversão das datas");
					
					jsonArray.put(jsonObject);
					continue;
		    	}
		    	
		    	funcionario.setEmissorRg(orgaoRg);
		    	funcionario.setEndereco(endereco);
		    	funcionario.setEstadoCivil(estadoCivil);
//		    	funcionario.setFormaContratual(formaContratual);
		    	funcionario.setMatricula(numFuncional + numVinculo + "-" + numPensionista );
		    	funcionario.setNome(nome);
		    	funcionario.setNomeMae(mae);
		    	funcionario.setNomePai(pai);
		    	funcionario.setNumeroFuncional(numFuncional);
		    	funcionario.setNumeroMaximoParcela(120L);
		    	funcionario.setNumeroPensionista(numPensionista);
		    	funcionario.setNumeroVinculo(numVinculo);
		    	funcionario.setRg(numRg);
		    	funcionario.setSexo(sexo);
		    	funcionario.setStatus(true);
		    	funcionario.setSetor(setor);
		    	funcionario.setSecretaria(unidAdmin.getSecretaria());
		    	
		    	if(calculaMargem) {
		    		try {
		    			funcionario.setSalarioBase(new BigDecimal(sbc.replace(",", ".")));
		    		}
		    		catch(Exception e) {
		    			e.printStackTrace();
		    		}
		    	}
		    	
		    	listaFuncionario.add(funcionario);
		    	
		    	if(firstLine % 10000 == 0) {
		    		service.save(listaFuncionario);
		    		listaFuncionario = new ArrayList<Funcionario>();
		    	}

		    }
		    
		    if(!listaFuncionario.isEmpty()) {
		    	service.save(listaFuncionario);
	    		listaFuncionario = new ArrayList<Funcionario>();
		    }
		    
		    jsonSucesso.put("title", "Sucesso");
			jsonSucesso.put("messages", jsonArray);
		}
		catch(Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().body(Funcoes.jsonMessage("Erro", "Erro ao ler arquivo!", "400"));
		}
	
		return ResponseEntity.ok().body(jsonSucesso.toString());
		
	}
	
	@PostMapping(value="/token/importfile")
	@PreAuthorize("hasAuthority('/funcionario/new')")
	public ResponseEntity<String> importFuncionariosToken(@RequestParam("file") MultipartFile multiPartFile, HttpServletResponse response){
		
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonSucesso = new JSONObject();
		
		List<Funcionario> listaFuncionario = new ArrayList<Funcionario>();
		
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
		    	
		    	if(campos.length < 3) {
					jsonObject.put("linha", "erro na linha: " + (firstLine +1));
					jsonObject.put("mensagem", "nenhum valor pode estar vazio");
					
					jsonArray.put(jsonObject);
					continue;
				}
		    	
		    	String cpf = campos[0];
		    	String matricula = campos[1];
		    	String token = campos[2];
		    	
		    	if(cpf.isEmpty()
					|| matricula.isEmpty()
					|| token.isEmpty()) {
					
					jsonObject.put("linha", "erro na linha: " + (firstLine + 1));
					jsonObject.put("mensagem", "Nenhum valor pode estar vazio");
					
					jsonArray.put(jsonObject);
					continue;
						
				}
		    	
		    	Funcionario funcionario = funcionarioRepository.findByCpfAndMatricula(cpf, matricula);
		    	
		    	if(funcionario == null) {
		    		jsonObject.put("linha", "erro na linha: " + (firstLine + 1));
					jsonObject.put("mensagem", "Funcionário não encontrado");
					
					jsonArray.put(jsonObject);
					continue;
		    	}
		    	
		    	funcionario.setToken(token);

		    	listaFuncionario.add(funcionario);
		    	
		    	if(firstLine % 10000 == 0) {
		    		service.saveToken(listaFuncionario);
		    		listaFuncionario = new ArrayList<Funcionario>();
		    	}
		    }
		    
		    
		    if(!listaFuncionario.isEmpty()) {
		    	service.saveToken(listaFuncionario);
	    		listaFuncionario = new ArrayList<Funcionario>();
		    }
		    
		    jsonSucesso.put("title", "Sucesso");
			jsonSucesso.put("messages", jsonArray);
		}
		catch(Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().body(Funcoes.jsonMessage("Erro", "Erro ao ler arquivo!", "400"));
		}
	
		return ResponseEntity.ok().body(jsonSucesso.toString());
	}
}

