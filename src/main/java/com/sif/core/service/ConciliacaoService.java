package com.sif.core.service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sif.core.exception.GenericException;
import com.sif.core.utils.Funcoes;
import com.sif.core.utils.SifLogUtil;
import com.sif.model.Administracao;
import com.sif.model.Conciliacao;
import com.sif.model.ConciliacaoErro;
import com.sif.model.Log;
import com.sif.model.LogAcao;
import com.sif.model.custom.AdministracaoCustomDTO;
import com.sif.model.custom.EnderecoCustomDTO;
import com.sif.model.utils.DescricaoLogAcaoHelper;
import com.sif.repository.AdministracaoRepository;
import com.sif.repository.ConciliacaoErroRepository;
import com.sif.repository.ConciliacaoRepository;
import com.sif.repository.EnderecoRepository;
import com.sif.repository.LogRepository;
import com.sif.repository.specification.AdministracaoSpecification;
import com.sif.repository.specification.ConciliacaoErroSpecification;
import com.sif.repository.specification.ConciliacaoSpecification;

@Service
@JsonSerialize
public class ConciliacaoService {

	@Autowired
	ConciliacaoRepository conciliacaoRepository;
	
	@Autowired
	ConciliacaoErroRepository conciliacaoErroRepository;
	
	@Autowired
	EnderecoRepository enderecoRepository;
	
	@Autowired
	TicketService ticketService;
	
	@Autowired
	SifLogUtil logUtil;
	
	@Autowired
	Funcoes funcoes;
	
	@Autowired
	LogRepository logRepository;
	
	public Page<Conciliacao> getAll(Pageable pageable, Conciliacao conciliacao) {
		ConciliacaoSpecification spec = new ConciliacaoSpecification();
		
		if(conciliacao.getDataRegistro() != null) {
			conciliacao.setDataInicio(conciliacao.getDataRegistro());
			
			Calendar calLe = Calendar.getInstance();
			calLe.setTime(conciliacao.getDataRegistro());
			calLe.add(Calendar.DAY_OF_MONTH, 1);
			conciliacao.setDataFim(calLe.getTime());
			
		}
		
		Page<Conciliacao> lista = conciliacaoRepository.findAll(Specification.where(
				spec.nomeUsuarioLike(conciliacao.getNomeUsuario()))
			.and(spec.nomeLike(conciliacao.getNomeArquivo()))
			.and(spec.dataLogGe(conciliacao.getDataInicio()))
			.and(spec.dataLogLe(conciliacao.getDataFim())),pageable);
		
		return lista;
	}

	public Page<ConciliacaoErro> getErrorByConciliacao(Long idConciliacao, Pageable pageable) {
		
		ConciliacaoErroSpecification spec = new ConciliacaoErroSpecification();
		
		Page<ConciliacaoErro> lista = conciliacaoErroRepository.findAll(Specification.where(
				spec.conciliacaoEqual(idConciliacao)),pageable);
		
		return lista;
	}
}
