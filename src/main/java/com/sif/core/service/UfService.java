package com.sif.core.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.sif.core.exception.GenericException;
import com.sif.core.utils.Funcoes;
import com.sif.core.utils.SifLogUtil;
import com.sif.model.Log;
import com.sif.model.LogAcao;
import com.sif.model.Uf;
import com.sif.model.list.UfDTO;
import com.sif.model.utils.DescricaoLogAcaoHelper;
import com.sif.repository.LogRepository;
import com.sif.repository.UfRepository;
import com.sif.repository.specification.UfSpecification;

@Service
public class UfService {

	@Autowired
	UfRepository ufRepository;
	
	@Autowired
	Funcoes funcoes;
	
	@Autowired
	SifLogUtil logUtil;
	
	@Autowired
	LogRepository logRepository;
	
	public Page<Uf> getAll(Pageable pageable, Uf uf) {
		UfSpecification spec = new UfSpecification();
		
		List<UfDTO> listaRetorno = new ArrayList<UfDTO>();
		Page<Uf> lista = ufRepository.findAll(Specification.where(
				spec.idEquals(uf.getId() != null ? uf.getId().toString() : null))
			.and(spec.siglaEquals(uf.getSigla()))
			.and(spec.nomeLike(uf.getNome()))
			.and(spec.findByStatus()),pageable);
		
//		for(Uf objeto : lista) {
//			UfDTO entity = new UfDTO();
//			entity.setId(objeto.getId());
//			entity.setSigla(objeto.getSigla());
//			entity.setNome(objeto.getNome());
//			
//			listaRetorno.add(entity);
//		}
//		
//		int start = Integer.parseInt(String.valueOf(pageable.getOffset()));
//		int end = (start + pageable.getPageSize()) > listaRetorno.size() ? listaRetorno.size() : (start + pageable.getPageSize());
		return lista;
	}
	
	public ResponseEntity<Uf> findById(Long id) {
		return Optional
				.ofNullable(ufRepository.findById(id).get())
				.map(uf -> ResponseEntity.ok().body(uf))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<Uf> create(Uf uf) {
		
		validarUf(uf);
		
		Uf ufSave = ufRepository.save(uf);
		
		try {
			LogAcao logAcao = funcoes.logAcao(ufSave.getId(), getDescricaoAdicionar(), funcoes.getLoggedUser());
			logUf(null, uf, logAcao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return Optional
				.ofNullable(ufRepository.save(uf))
				.map(ufAux -> ResponseEntity.ok().body(ufAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<Uf> update(Uf uf,Long id) {
		
		validarUf(uf);
		
		Uf ufSave = ufRepository.findById(id).get();
		if(ufSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		Long idUf = ufSave.getId();
		
		try {
			LogAcao logAcao = funcoes.logAcao(ufSave.getId(), getDescricaoEditar(), funcoes.getLoggedUser());
			logUf(ufSave, uf, logAcao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		BeanUtils.copyProperties(uf, ufSave);
		ufSave.setId(idUf);
		
		return Optional
				.ofNullable(ufRepository.save(ufSave))
				.map(ufAux -> ResponseEntity.ok().body(ufAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public Uf findUfBySigla(String sigla) {
		UfSpecification ufSpec = new UfSpecification();
		Optional<Uf> uf = ufRepository.findOne(Specification.where(
				ufSpec.siglaEquals(sigla)
			));
		
		if(uf.isPresent()) {
			return uf.get();
		}
		
		
		return null;
	}
	
	public ResponseEntity<Uf> delete(Long id) {
		
		Uf ufSave = ufRepository.findById(id).get();
		if(ufSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		ufSave.setStatus(false);
		
		Uf uf = new Uf();
		BeanUtils.copyProperties(ufSave, uf);
		uf.setStatus(!ufSave.isStatus());
		
		try {
			LogAcao logAcao = funcoes.logAcao(ufSave.getId(), getDescricaoExcluir(), funcoes.getLoggedUser());
			logUf(uf, ufSave, logAcao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return Optional
				.ofNullable(ufRepository.save(ufSave))
				.map(ufAux -> ResponseEntity.ok().body(ufAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	private void validarUf(Uf uf) throws GenericException {
		
		if(uf.getNome() == null || uf.getNome().isEmpty()) {
			throw new GenericException("Erro","Nome não pode ser nulo");
		}
		if(uf.getSigla() == null || uf.getSigla().isEmpty()) {
			throw new GenericException("Erro","Sigla não pode ser nulo");
		}
	}
	
	public Long getDescricaoAdicionar() {
		return DescricaoLogAcaoHelper.ADICIONAR_UF;
	}

	public Long getDescricaoEditar() {
		return DescricaoLogAcaoHelper.EDITAR_UF;
	}

	public Long getDescricaoExcluir() {
		return DescricaoLogAcaoHelper.EXCLUIR_UF;
	}
	
	public void logUf(Uf previous, Uf current, LogAcao logAcao) {
		
		if(previous == null || previous.getId() == null) {
			previous = new Uf();
		}
		
		List<Log> logs = new ArrayList<Log>();
		
		Date datAlteracao = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:ss:mm");
		
		logUtil.withEntidade("UF");
		
		logs.add(logUtil.fromValues("nome_uf", 
				previous.getNome() == null ? "-" : previous.getNome(),
				current.getNome() == null ? "-" : current.getNome(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("sigla_uf", 
				previous.getSigla() == null ? "-" : previous.getSigla(),
				current.getSigla() == null ? "-" : current.getSigla(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("status_uf", 
				previous.isStatus() ? "ativo" : "inativo",
				current.isStatus() ? "ativo" : "inativo",
				datAlteracao));
		
		if(logs.isEmpty()) {
			return;
		}
		
		for(Log log : logs) {
			if(log != null) {
				log.setIdRow(current.getId());
				log.setLogAcao(logAcao);
				logRepository.save(log);
			}
		}
		
	}
	
}
