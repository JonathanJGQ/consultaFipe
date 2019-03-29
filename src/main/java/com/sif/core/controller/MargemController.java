package com.sif.core.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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

import com.sif.core.service.FuncionarioMargemService;
import com.sif.core.service.FuncionarioService;
import com.sif.core.service.MargemService;
import com.sif.core.service.OrgaoService;
import com.sif.core.service.SecretariaService;
import com.sif.model.Funcionario;
import com.sif.model.FuncionarioMargem;
import com.sif.model.Margem;
import com.sif.model.custom.MargemCustomDTO;
import com.sif.repository.FuncionarioMargemRepository;
import com.sif.repository.FuncionarioRepository;
import com.sif.repository.MargemRepository;

@RestController
@RequestMapping("/margem")
public class MargemController{
	
	@Autowired
	MargemService service;
	
	@Autowired
	FuncionarioService funcionarioService;
	
	@Autowired
	FuncionarioMargemService funcionarioMargemService;
	
	@Autowired
	FuncionarioMargemRepository funcionarioMargemRepository;
	
	@Autowired
	FuncionarioRepository funcionarioRepository;
	
	@Autowired
	MargemRepository margemRepository;
	
	@Autowired
	SecretariaService secretariaService;
	
	@Autowired
	OrgaoService orgaoService;
	
	@GetMapping
	@PreAuthorize("hasAuthority('/margem/list')")
	public Page<Margem> getAll(Pageable pageable, Margem margem) {
		return service.getAll(pageable, margem);
	}
	
	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('/margem/list')")
	public ResponseEntity<MargemCustomDTO> findById(@PathVariable() Long id) {
		return service.findById(id);
	}
	
	@PostMapping
	@PreAuthorize("hasAuthority('/margem/new')")
	public ResponseEntity<Margem> create(@RequestBody MargemCustomDTO margem) {
		return service.create(margem);
	}
	
	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('/margem/edit')")
	public ResponseEntity<Margem> update(@RequestBody MargemCustomDTO margem, @PathVariable() Long id) {
		return service.update(margem, id);
	}
	
	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('/margem/delete')")
	public ResponseEntity<Margem> delete(@PathVariable() Long id) {
		return service.delete(id);
	}
	
	@PostMapping("/importfile")
	@PreAuthorize("hasAuthority('/margem/new')")
	public ResponseEntity<String> importMargem(@RequestParam("file") MultipartFile multiPartFile){
		
		JSONObject jsonRetorno = new JSONObject();
		JSONObject jsonSucesso = new JSONObject();
		
		List<FuncionarioMargem> inserts = new ArrayList<FuncionarioMargem>();
		
		int status = 200;
		JSONArray jsonArray = new JSONArray();
		
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
		
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
				
				if(campos.length < 13) {
					jsonObject.put("linha", "erro na linha" + firstLine);
					jsonObject.put("mensagem", "nenhum valor pode estar vazio");
					
					jsonArray.put(jsonObject);
					continue;
				}
				
				String numeroFuncionalidade = campos[0];
				String numeroVinculo = campos[1];
				String numeroPensionista = campos[2];
				String cpf = campos[3];
				String nome = campos[4];
				String dataNascimento = campos[5];
				String setor = campos[6];
				String regime = campos[7];
				String dataAdmissao = campos[8];
				String salarioBase = campos[9];
				String categoria = campos[10];
				String percentualClienteMargem = campos[11];
				String numeroMaximoParcelas = campos[12];
				
				//Informações necessárias para buscar o Funcionario
				
				if(numeroFuncionalidade.isEmpty()
						|| numeroVinculo.isEmpty()
						|| numeroPensionista.isEmpty()
						|| cpf.isEmpty()
						|| nome.isEmpty()
						|| dataNascimento.isEmpty()
						|| setor.isEmpty()
						|| regime.isEmpty()
						|| dataAdmissao.isEmpty()
						|| salarioBase.isEmpty()
						|| categoria.isEmpty()
						|| percentualClienteMargem.isEmpty()
						|| numeroMaximoParcelas.isEmpty()) {
					
						jsonObject.put("linha", "erro na linha" + firstLine);
						jsonObject.put("mensagem", "Nenhum valor pode estar vazio");
						
						jsonArray.put(jsonObject);
						continue;
						
				}
				
				Funcionario funcionario = new Funcionario();
				funcionario.setNumeroFuncional(numeroFuncionalidade);
				funcionario.setNumeroVinculo(numeroVinculo);
				funcionario.setNumeroPensionista(numeroPensionista);
				funcionario.setCpf(cpf);
				funcionario.setNome(nome);
				
				Date dateNascimento = sdf.parse(dataNascimento);
				
				funcionario.setDataNascimento(dateNascimento);
				
				BigDecimal salarioBaseBigDecimal = new BigDecimal(salarioBase.replace(",", "."));
				
				funcionario.setSalarioBase(salarioBaseBigDecimal);
				
				Funcionario funcionarioAchado = funcionarioRepository.findForMargemInsert(funcionario.getNumeroFuncional(), funcionario.getNumeroVinculo(), funcionario.getNumeroPensionista(), funcionario.getCpf());
			
				if(funcionarioAchado == null) {
					jsonObject.put("linha", "erro na linha: " + firstLine);
					jsonObject.put("mensagem", "Funcionario não foi encontrado");
					
					jsonArray.put(jsonObject);
					
					continue;
				}
				
				List<Margem> margens = getMargensForFuncionario(funcionarioAchado);
				
				if(margens == null || margens.isEmpty()) {
					
					jsonObject.put("linha", "erro na linha" + firstLine);
					jsonObject.put("mensagem", "Não foram encontradas margens");
					
					continue;
				}
				
				for(Margem margem : margens) {
					
					//Para editar
					FuncionarioMargem funcionarioMargem = funcionarioMargemService.findByMargemFuncionario(margem.getId(), funcionarioAchado.getId());
					
					if(funcionarioMargem == null) {
						funcionarioMargem = new FuncionarioMargem();
					}
					
					if(funcionarioAchado.getSalarioBase() == null) {
						funcionarioAchado.setSalarioBase(salarioBaseBigDecimal);
						funcionarioAchado = funcionarioRepository.save(funcionarioAchado);
					}
					
					funcionarioMargem.setFuncionario(funcionarioAchado);
					funcionarioMargem.setMargem(margem);
					
					//Falta trazer o certo
					
					Long percentRestricao = Long.parseLong(percentualClienteMargem);
					funcionarioMargem.setPercentualClienteMargem(percentRestricao);
					
					Long maxParcelas = Long.parseLong(numeroMaximoParcelas);
					funcionarioMargem.setMaximaParcelaCliente(maxParcelas);
					
					//Calculando Limite de Funcionario na Margem
					funcionarioMargem.setLimite(calculateLimit(margem, funcionarioAchado, funcionarioMargem.getPercentualClienteMargem()));
					
					funcionarioMargem.setStatus(true);
//					funcionarioMargemRepository.save(funcionarioMargem);
					
					inserts.add(funcionarioMargem);
					
					if(firstLine == 1) {
						funcionarioMargemService.saveList(inserts);
						inserts = new ArrayList<FuncionarioMargem>();
					}
					
				}
				
				if(inserts.size() == 10000) {
					funcionarioMargemService.saveList(inserts);
					inserts = new ArrayList<FuncionarioMargem>();
				}
				
			}
			
			if(!inserts.isEmpty()) {
				funcionarioMargemService.saveList(inserts);
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
	
	private BigDecimal calculateLimit(Margem margem, Funcionario funcionario, Long percentualMargem) {
		
		BigDecimal percentagemMargem = new BigDecimal(margem.getPercentual());
		BigDecimal percentagemDecimal = percentagemMargem.divide(new BigDecimal(100));
		
		BigDecimal limit = percentagemDecimal.multiply(funcionario.getSalarioBase());
//		
//		return limit.multiply(new BigDecimal(percentualMargem / 100));
		
		return limit;
		
	}
	
	private List<Margem> getMargensForFuncionario(Funcionario funcionario){
		if(funcionario != null) {
			if(funcionario.getId() != null) {
				return margemRepository.findByFuncionario(funcionario.getId());
			}
		}
		
		return null;
	}
}

