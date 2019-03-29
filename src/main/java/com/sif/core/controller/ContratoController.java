package com.sif.core.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

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

import com.sif.core.service.ContratoService;
import com.sif.core.service.FuncionarioService;
import com.sif.core.service.LogAcaoService;
import com.sif.core.utils.Funcoes;
import com.sif.model.Contrato;
import com.sif.model.Funcionario;
import com.sif.model.LogAcao;
import com.sif.model.Verba;
import com.sif.model.custom.ContratoCustomDTO;
import com.sif.model.list.ContratoDTO;
import com.sif.model.utils.DescricaoLogAcaoHelper;
import com.sif.repository.ContratoRepository;
import com.sif.repository.FuncionarioRepository;
import com.sif.repository.SituacaoContratoRepository;
import com.sif.repository.VerbaRepository;

@RestController
@RequestMapping("/contrato")
public class ContratoController{
	
	@Autowired
	ContratoService service;
	
	@Autowired
	ContratoRepository contratoRepository;
	
	@Autowired
	FuncionarioService funcionarioService;
	
	@Autowired
	FuncionarioRepository funcionarioRepository;
	
	@Autowired
	SituacaoContratoRepository situacaoContratoRepository;
	
	@Autowired
	VerbaRepository verbaRepository;
	
	@Autowired
	LogAcaoService logAcaoService;
	
	@Autowired 
	Funcoes funcoes;
	
	@GetMapping
	@PreAuthorize("hasAuthority('/contrato/list')")
	public Page<Contrato> getAll(Pageable pageable, ContratoDTO contrato) {
		return service.getAll(pageable, contrato);
	}
	
	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('/contrato/list')")
	public ResponseEntity<Contrato> findById(@PathVariable() Long id) {
		return service.findById(id); 
	}
	
	@PostMapping
	@PreAuthorize("hasAuthority('/contrato/new')")
	public ResponseEntity<Contrato> create(@RequestBody ContratoCustomDTO contrato) {
		return service.create(contrato);
	}
	
	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('/contrato/edit')")
	public ResponseEntity<Contrato> update(@RequestBody ContratoCustomDTO contrato, @PathVariable() Long id) {
		return service.update(contrato, id);
	}
	
	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('/contrato/delete')")
	public ResponseEntity<Contrato> delete(@PathVariable() Long id) {
		return service.delete(id);
	}
	
	@PostMapping(value="/importfile")
	@PreAuthorize("hasAuthority('/contrato/import')")
	public ResponseEntity<String> importContrato(@RequestParam("file") MultipartFile multiPartFile, HttpServletResponse response){
		
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonSucesso = new JSONObject();
		
		boolean anyImport = false;
		
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
		    	
		    	if(campos.length != 15) {
					jsonObject.put("linha", "erro na linha " + (firstLine + 1));
					jsonObject.put("mensagem", "nenhum valor pode estar vazio");
					
					jsonArray.put(jsonObject);
					continue;
				}
		    	
		    	String idVerba = campos[0];
		    	String nome = campos[1];
		    	String cpf = campos[2];
		    	String numFuncional = campos[3];
		    	String numVinculo = campos[4];
		    	String numPens = campos[5];
		    	String matricula = campos[6];
		    	String numContrato = campos[7];
		    	String dataLancamento = campos[8];
		    	String qtdParcelas = campos[9];
		    	String valorParcela = campos[10];
		    	String valorSolicitado = campos[11];
		    	String qtdParcelasPagas = campos[12];
		    	String saldoContratacao = campos[13];
		    	String flagLegado = campos[14];
		    	
		    	if(idVerba.isEmpty()
						|| nome.isEmpty()
						|| cpf.isEmpty()
						|| numFuncional.isEmpty()
						|| numVinculo.isEmpty()
						|| numPens.isEmpty()
						|| matricula.isEmpty()
						|| numContrato.isEmpty()
						|| dataLancamento.isEmpty()
						|| qtdParcelas.isEmpty()
						|| valorParcela.isEmpty()
						|| valorSolicitado.isEmpty()
						|| qtdParcelasPagas.isEmpty()
						|| saldoContratacao.isEmpty()
						|| flagLegado.isEmpty()) {
					
						jsonObject.put("linha", "erro na linha " + (firstLine + 1));
						jsonObject.put("mensagem", "Nenhum valor pode estar vazio");
						
						jsonArray.put(jsonObject);
						continue;
						
					}
		    	
		    	qtdParcelas = qtdParcelas.replace(",", ".").replace(" ", "");
		    	qtdParcelasPagas = qtdParcelasPagas.replace(",", ".").replace(" ", "");
		    	saldoContratacao = saldoContratacao.replace(",", ".").replace(" ", "");
		    	valorParcela = valorParcela.replace(",", ".").replace(" ", "");
		    	valorSolicitado = valorSolicitado.replace(",", ".").replace(" ", "");
		    	
		    	BigDecimal qtdParcelasBd = null;
		    	BigDecimal qtdParcelasPagasBd = null;
		    	BigDecimal saldoContratacaoBd = null;
		    	BigDecimal valorParcelaBd = null;
		    	BigDecimal valorSolicitadoBd = null;
		    	
		    	try {
		    		qtdParcelasBd = new BigDecimal(qtdParcelas);
		    		qtdParcelasPagasBd = new BigDecimal(qtdParcelasPagas);
		    		saldoContratacaoBd = new BigDecimal(saldoContratacao);
		    		valorParcelaBd = new BigDecimal(valorParcela);
		    		valorSolicitadoBd = new BigDecimal(valorSolicitado);
		    		
		    	}
		    	catch(Exception e) {
		    		jsonObject.put("linha", "erro na linha " + (firstLine + 1));
					jsonObject.put("mensagem", "Erro nos ");
					
					jsonArray.put(jsonObject);
					continue;
		    	}
		    	
		    	Contrato contrato = contratoRepository.findByCodigoContrato(numContrato);
		    	if(contrato == null) {
		    		contrato = new Contrato();
		    	}
//		    	else {
//		    		jsonObject.put("linha", "erro na linha " + (firstLine + 1));
//					jsonObject.put("mensagem", "Número de contrato já existe");
//					
//					jsonArray.put(jsonObject);
//					continue;
//		    	}
		    	
		    	Funcionario funcionario = funcionarioRepository.findForMargemInsert(numFuncional, numVinculo, numPens, cpf);
		    	if(funcionario == null) {
		    		jsonObject.put("linha", "erro na linha " + (firstLine + 1));
					jsonObject.put("mensagem", "Funcionário não encontrado");
					
					jsonArray.put(jsonObject);
					continue;
		    	}
		    	
		    	Verba verba = verbaRepository.findById(Long.valueOf(idVerba)).get();
		    	if(verba == null) {
		    		jsonObject.put("linha", "erro na linha " + (firstLine + 1));
					jsonObject.put("mensagem", "Verba não encontrada");
					
					jsonArray.put(jsonObject);
					continue;
		    	}
		    	
		    	SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
		    	contrato.setDataLancamento(sdf.parse(dataLancamento));
		    	contrato.setFlagLegado(flagLegado.equals("0") ? false : true);
		    	contrato.setFuncionario(funcionario);
				contrato.setNumeroContrato(numContrato);
				contrato.setQuantidadeParcelas(new BigDecimal(qtdParcelas));
				contrato.setQuantidadeParcelasPagas(new BigDecimal(qtdParcelasPagas));
				contrato.setSaldoContratacao(new BigDecimal(saldoContratacao));
				contrato.setSituacao(situacaoContratoRepository.findById(1L).get());
				contrato.setStatus(true);
				contrato.setTipoContrato("Carga de Contratos");
				contrato.setTipoPagamento('N');
				contrato.setUsuario(funcoes.getLoggedUser());
				contrato.setValorParcela(new BigDecimal(valorParcela));
				contrato.setValorSolicitacao(new BigDecimal(valorSolicitado));
				
				
				contrato.setVerba(verba);
		    	
				service.save(contrato);
				anyImport = true;
		    }
		    jsonSucesso.put("title", "Sucesso");
			jsonSucesso.put("messages", jsonArray);
		}
		catch(Exception e) {
			e.printStackTrace();
			if(anyImport) {
				try {
					funcoes.logAcao(0L, DescricaoLogAcaoHelper.ENVIO_DE_REMESSA_DE_CONTRATO, funcoes.getLoggedUser());
				} catch (Exception e1) {
					e1.printStackTrace();
				}				
			}
			return ResponseEntity.badRequest().body(Funcoes.jsonMessage("Erro", "Erro ao ler arquivo!", "400"));
		}
		
		if(anyImport) {
			try {
				funcoes.logAcao(0L, DescricaoLogAcaoHelper.ENVIO_DE_REMESSA_DE_CONTRATO, funcoes.getLoggedUser());
			} catch (Exception e1) {
				e1.printStackTrace();
			}				
		}
	
		return ResponseEntity.ok().body(jsonSucesso.toString());
		
	}
}

