package com.sif.core.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sif.core.service.ItensFolhaService;
import com.sif.core.utils.Funcoes;
import com.sif.model.Conciliacao;
import com.sif.model.ConciliacaoErro;
import com.sif.model.Contrato;
import com.sif.model.Funcionario;
import com.sif.model.ItensFolha;
import com.sif.repository.ConciliacaoErroRepository;
import com.sif.repository.ConciliacaoRepository;
import com.sif.repository.ContratoRepository;
import com.sif.repository.FuncionarioRepository;
import com.sif.repository.ItensFolhaRepository;

@RestController
@RequestMapping("/itensfolha")
public class ItensFolhaController{
	
	@Autowired
	ItensFolhaService service;
	
	@Autowired
	ItensFolhaRepository itensFolhaRepository;
	
	@Autowired
	FuncionarioRepository funcionarioRepository;
	
	@Autowired
	ContratoRepository contratoRepository;
	
	@Autowired
	ConciliacaoRepository conciliacaoRepository;
	
	@Autowired
	ConciliacaoErroRepository conciliacaoErroRepository;
	
	@Autowired
	Funcoes funcoes;
	
	@GetMapping
	@PreAuthorize("hasAuthority('/itensfolha/list')")
	public ResponseEntity<List<ItensFolha>> getAll() {
		return service.getAll();
	}
	
	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('/itensfolha/list')")
	public ResponseEntity<ItensFolha> findById(@PathVariable() Long id) {
		return service.findById(id);
	}
	
	@PostMapping
	@PreAuthorize("hasAuthority('/itensfolha/new')")
	public ResponseEntity<ItensFolha> create(@RequestBody ItensFolha itensFolha) {
		return service.create(itensFolha);
	}
	
	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('/itensfolha/edit')")
	public ResponseEntity<ItensFolha> update(@RequestBody ItensFolha itensFolha, @PathVariable() Long id) {
		return service.update(itensFolha, id);
	}
	
	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('/itensfolha/delete')")
	public ResponseEntity<ItensFolha> delete(@PathVariable() Long id) {
		return service.delete(id);
	}
	
	@PostMapping(value="/importfile")
//	@PreAuthorize("hasAuthority('/itensfolha/import')")
	public ResponseEntity<String> importItensFolha(@RequestParam("file") MultipartFile multiPartFile, HttpServletResponse response){
		
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonSucesso = new JSONObject();
		
		Conciliacao conciliacao = new Conciliacao();
		conciliacao.setNomeArquivo(multiPartFile.getOriginalFilename());
		conciliacao.setUsuario(funcoes.getLoggedUser());
		conciliacao.setDataRegistro(new Date());
		
		conciliacao = conciliacaoRepository.save(conciliacao);
		
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
		    	
		    	if(campos.length != 13) {
					jsonObject.put("linha", "erro na linha " + (firstLine + 1));
					jsonObject.put("mensagem", "Arquivo com formato inválido");
					
					jsonArray.put(jsonObject);
					
					saveErrorConciliacao("erro na linha " + (firstLine + 1)
							, "Arquivo com formato inválido", conciliacao);
					
					continue;
				}
		    	
		    	String idItensFolha = campos[0];
		    	String numFuncional = campos[1];
		    	String numVinculo = campos[2];
		    	String numPens = campos[3];
		    	String matricula = campos[4];
		    	String cpf = campos[5];
		    	String nome = campos[6];
		    	String verba = campos[7];
		    	String especie = campos[8];
		    	String dataInicial = campos[9];
		    	String dataFim = campos[10];
		    	String valor = campos[11].replaceAll(",", ".");
		    	String observacao = campos[12];
		    	
		    	
		    	if(idItensFolha.isEmpty()
						|| numFuncional.isEmpty()
						|| numVinculo.isEmpty()
						|| numPens.isEmpty()
						|| matricula.isEmpty()
						|| cpf.isEmpty()
						|| nome.isEmpty()
						|| verba.isEmpty()
						|| especie.isEmpty()
						|| dataInicial.isEmpty()
						|| dataFim.isEmpty()
						|| valor.isEmpty()
						|| observacao.isEmpty()) {
					
						jsonObject.put("linha", "erro na linha" + firstLine);
						jsonObject.put("mensagem", "Nenhum valor pode estar vazio");
						
						jsonArray.put(jsonObject);
						
						saveErrorConciliacao("erro na linha " + (firstLine + 1)
								, "Nenhum valor pode estar vazio", conciliacao);
						
						continue;
						
					}
		    	
		    	
		    	Funcionario funcionario = funcionarioRepository.findForMargemInsert(numFuncional, numVinculo, numPens, cpf);
		    	
		    	if(funcionario == null) {
		    		jsonObject.put("linha", "erro na linha " + (firstLine + 1));
					jsonObject.put("mensagem", "Funcionário não foi encontrado");
					
					jsonArray.put(jsonObject);
				
					saveErrorConciliacao("erro na linha " + (firstLine + 1)
							, "Funcionário não foi encontrado", conciliacao);
					
					continue;
		    	}
		    	
		    	List<Contrato> contratosFuncionario = contratoRepository.findByFuncionario(funcionario.getId());
		    	
		    	if(contratosFuncionario == null || contratosFuncionario.isEmpty()) {
		    		jsonObject.put("linha", "erro na linha " + (firstLine + 1));
					jsonObject.put("mensagem", "Não foram encontrado contratos para este funcionário");
					
					jsonArray.put(jsonObject);
					
					saveErrorConciliacao("erro na linha " + (firstLine + 1)
							, "Não foram encontrado contratos para este funcionário", conciliacao);
					
					continue;
		    	}
		    	
		    	List<ItensFolha> itensFolhaFuncionario = new ArrayList<ItensFolha>();
		    	for(Contrato contrato : contratosFuncionario) {
		    		itensFolhaFuncionario.addAll(itensFolhaRepository.findByContrato(contrato.getId()));
		    	}
		    	
		    	if(itensFolhaFuncionario.isEmpty()) {
		    		jsonObject.put("linha", "erro na linha " + (firstLine + 1));
					jsonObject.put("mensagem", "Não foram encontrado Itens Folha para este funcionário");
					
					jsonArray.put(jsonObject);
					
					saveErrorConciliacao("erro na linha " + (firstLine + 1)
							, "Não foram encontrado Itens Folha para este funcionário", conciliacao);
					continue;
		    	}
		    	
		    	ItensFolha itemFolhaAtual = null;
		    	for(ItensFolha item : itensFolhaFuncionario) {
		    		if(item.getId().equals(Long.valueOf(idItensFolha))) {
		    			itemFolhaAtual = item;
		    			break;
		    		}
		    	}
		    	
		    	if(itemFolhaAtual == null) {
		    		jsonObject.put("linha", "erro na linha " + (firstLine + 1));
					jsonObject.put("mensagem", "Item Folha do arquivo não encontrado");
					
					jsonArray.put(jsonObject);
					
					saveErrorConciliacao("erro na linha " + (firstLine + 1)
							, "Item Folha do arquivo não encontrado", conciliacao);
					
					continue;
		    	}
		    	
		    	try {
			    	itemFolhaAtual.setValorParcela(new BigDecimal(valor));
			    	itemFolhaAtual.setObservacao(observacao);
		    	}
		    	catch(Exception e) {
		    		jsonObject.put("linha", "erro na linha " + (firstLine + 1));
					jsonObject.put("mensagem", "Erro ao converter valor da parcela");
					
					jsonArray.put(jsonObject);
					
					saveErrorConciliacao("erro na linha " + (firstLine + 1)
							,  "Erro ao converter valor da parcela", conciliacao);
					
					continue;
		    	}
		    	
				itensFolhaRepository.save(itemFolhaAtual);
				
		    }
		    jsonSucesso.put("title", "Sucesso");
			jsonSucesso.put("messages", jsonArray);
		}
		catch(Exception e) {
			
			if(conciliacao.getId() != null) {
				saveErrorConciliacao("Erro","Erro ao ler arquivo",conciliacao);
			}
			return ResponseEntity.badRequest().body(Funcoes.jsonMessage("Erro", "Erro ao ler arquivo!", "400"));
		}
	
		return ResponseEntity.ok().body(jsonSucesso.toString());
		
	}
	
	private void saveErrorConciliacao(String titulo, String mensagem, Conciliacao conciliacao) {

		ConciliacaoErro erro = new ConciliacaoErro();
		erro.setConciliacao(conciliacao);
		erro.setMensagem(mensagem);
		erro.setTitulo(titulo);
		
		conciliacaoErroRepository.save(erro);
	}
}

