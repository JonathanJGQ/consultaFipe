package com.sif.core.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.json.JSONObject;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.sif.core.exception.GenericException;
import com.sif.core.utils.Funcoes;
import com.sif.core.utils.SifLogUtil;
import com.sif.model.Configuracao;
import com.sif.model.Log;
import com.sif.model.LogAcao;
import com.sif.model.utils.DescricaoLogAcaoHelper;
import com.sif.repository.ConfiguracaoRepository;
import com.sif.repository.LogRepository;
import com.sif.repository.specification.ConfiguracaoSpecification;

@Service
public class ConfiguracaoService {

	@Autowired
	ConfiguracaoRepository configuracaoRepository;
	
	@Autowired
	SifLogUtil logUtil;
	
	@Autowired
	LogRepository logRepository;
	
	@Autowired
	Funcoes funcoes;
	
	public ResponseEntity<List<Configuracao>> getAll() {
		return Optional
				.ofNullable(configuracaoRepository.findAll(new ConfiguracaoSpecification().findByStatus()))
				.map(configuracao -> ResponseEntity.ok().body(configuracao))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<Configuracao> findById(Long id) {
		return Optional
				.ofNullable(configuracaoRepository.findById(id).get())
				.map(configuracao -> ResponseEntity.ok().body(configuracao))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<Configuracao> create(Configuracao configuracao) {
		
		Calendar calendar = Calendar.getInstance();
		Date today = calendar.getTime();
		configuracao.setData(today);
		
		validarConfiguracao(configuracao);
		
//		logConfiguracao(null,configuracao);
		
		return Optional
				.ofNullable(configuracaoRepository.save(configuracao))
				.map(configuracaoAux -> ResponseEntity.ok().body(configuracaoAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<String> update(List<Configuracao> listaConfiguracao) {
		
		LogAcao logAcao = null;
		try {
			logAcao = funcoes.logAcao(null, getDescricaoAlterar(), funcoes.getLoggedUser());
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		for(Configuracao configuracao : listaConfiguracao) {
		
			validarConfiguracao(configuracao);
			
			Configuracao configuracaoSave = configuracaoRepository.findById(configuracao.getId()).get();
			if(configuracaoSave == null) {
				return ResponseEntity.notFound().build();
			}
			
			Long configuracaoId = configuracaoSave.getId();
			
			try {
				if(logAcao != null) {
					logConfiguracao(configuracaoSave,configuracao, logAcao);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			BeanUtils.copyProperties(configuracao, configuracaoSave);
			configuracaoSave.setId(configuracaoId);
			
			configuracaoRepository.save(configuracaoSave);
		}
		
		JSONObject jsonRetorno = new JSONObject();
		jsonRetorno.put("title", "Sucesso");
		jsonRetorno.put("message", "Configurações atualizadas com sucesso!");
		
		
		return Optional
				.ofNullable(jsonRetorno.toString())
				.map(verbaAux -> ResponseEntity.ok().body(verbaAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<Configuracao> delete(Long id) {
		
		Configuracao configuracaoSave = configuracaoRepository.findById(id).get();
		if(configuracaoSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		configuracaoSave.setStatus(false);
		
		Configuracao configuracao = new Configuracao();
		BeanUtils.copyProperties(configuracaoSave, configuracao);
		
		configuracao.setFlag(!configuracaoSave.isStatus());
		
//		logConfiguracao(configuracao,configuracao);
		
		return Optional
				.ofNullable(configuracaoRepository.save(configuracaoSave))
				.map(configuracaoAux -> ResponseEntity.ok().body(configuracaoAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	private void validarConfiguracao(Configuracao configuracao) throws GenericException {
		
		if(configuracao.getNome() == null || configuracao.getNome().isEmpty()) {
			throw new GenericException("Erro","Nome não pode ser nulo");
		}
	}	
	
	public Long getDescricaoAlterar() {
		return DescricaoLogAcaoHelper.ALTERAR_CONFIGURACAO;
	}
	
	public void logConfiguracao(Configuracao previousConfiguracao, Configuracao currentConfiguracao, LogAcao logAcao){
		
		if(previousConfiguracao == null || previousConfiguracao.getId() == null) {
			previousConfiguracao = new Configuracao();
		}
		
		List<Log> logs = new ArrayList<Log>();
		
		Date datAlteracao = new Date();
		
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:ss:mm");
		
		logUtil.withEntidade("Configuração");
		
		logs.add(logUtil.fromValues("descricao_configuracao", 
				previousConfiguracao.getNome() == null ? "-" : previousConfiguracao.getNome(), 
				currentConfiguracao.getNome() == null ? "-" : currentConfiguracao.getNome(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("valor_configuracao", 
				previousConfiguracao.getValor() == null ? "-" : previousConfiguracao.getValor(), 
				currentConfiguracao.getValor() == null ? "-" : currentConfiguracao.getValor(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("data_configuracao", 
				previousConfiguracao.getData() == null ? "-" : sdf.format(previousConfiguracao.getData()), 
				currentConfiguracao.getData() == null ? "-" : sdf.format(currentConfiguracao.getData()),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("nome_grupo", 
				previousConfiguracao.getNomeGrupo() == null ? "-" : previousConfiguracao.getNomeGrupo(), 
				currentConfiguracao.getNomeGrupo() == null ? "-" : currentConfiguracao.getNomeGrupo(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("is_flag", 
				Boolean.toString(previousConfiguracao.isFlag()), 
				Boolean.toString(currentConfiguracao.isFlag()),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("status_configuracao",
				Boolean.toString(previousConfiguracao.isStatus()),
				Boolean.toString(currentConfiguracao.isStatus()),
				datAlteracao
		));
		
		if(logs.isEmpty()) {
			return;
		}
		
		for(Log log : logs) {
			if(log != null) {
				log.setIdRow(currentConfiguracao.getId());
				log.setLogAcao(logAcao);
				logRepository.save(log);
			}
		}
			
		
	}
	
}
