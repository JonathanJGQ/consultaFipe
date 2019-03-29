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
import com.sif.model.Perfil;
import com.sif.model.list.PerfilDTO;
import com.sif.model.utils.DescricaoLogAcaoHelper;
import com.sif.repository.LogRepository;
import com.sif.repository.PerfilRepository;
import com.sif.repository.specification.PerfilSpecification;

@Service
public class PerfilService {

	@Autowired
	PerfilRepository perfilRepository;
	
	@Autowired
	Funcoes funcoes;
	
	@Autowired
	SifLogUtil logUtil;
	
	@Autowired
	LogRepository logRepository;
	
	public Page<Perfil> getAll(Pageable pageable, Perfil perfil) {
		PerfilSpecification spec = new PerfilSpecification();
		
		List<PerfilDTO> listaRetorno = new ArrayList<PerfilDTO>();
		Page<Perfil> lista = perfilRepository.findAll(Specification.where(
				spec.idEquals(perfil.getId() != null ? perfil.getId().toString() : null))
			.and(spec.nomeLike(perfil.getNome()))
			.and(spec.siglaEquals(perfil.getSigla()))
			.and(spec.findByStatus()),pageable);
		
//		for(Perfil objeto : lista) {
//			PerfilDTO entity = new PerfilDTO();
//			entity.setId(objeto.getId());
//			entity.setNome(objeto.getNome());
//			entity.setSigla(objeto.getSigla());
//			
//			listaRetorno.add(entity);
//		}
//		
//		int start = Integer.parseInt(String.valueOf(pageable.getOffset()));
//		int end = (start + pageable.getPageSize()) > listaRetorno.size() ? listaRetorno.size() : (start + pageable.getPageSize());
		return lista;
	}
	
	public ResponseEntity<Perfil> findById(Long id) {
		return Optional
				.ofNullable(perfilRepository.findById(id).get())
				.map(perfil -> ResponseEntity.ok().body(perfil))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<Perfil> create(Perfil perfil) {
		
		validarPerfil(perfil);
		
		Perfil perfilSave = perfilRepository.save(perfil);
		
		
		try {
			LogAcao logAcao = funcoes.logAcao(perfilSave.getId(), getDescricaoAdicionar(), funcoes.getLoggedUser());
			logPerfil(null, perfil, logAcao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return Optional
				.ofNullable(perfilSave)
				.map(perfilAux -> ResponseEntity.ok().body(perfilAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<Perfil> update(Perfil perfil,Long id) {
		
		validarPerfil(perfil);
		
		Perfil perfilSave = perfilRepository.findById(id).get();
		if(perfilSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		Long perfilId = perfilSave.getId();
		
		try {
			LogAcao logAcao = funcoes.logAcao(perfilSave.getId(), getDescricaoEditar(), funcoes.getLoggedUser());
			logPerfil(perfilSave, perfil, logAcao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		BeanUtils.copyProperties(perfil, perfilSave);
		perfilSave.setId(perfilId);
		
		return Optional
				.ofNullable(perfilRepository.save(perfilSave))
				.map(perfilAux -> ResponseEntity.ok().body(perfilAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<Perfil> delete(Long id) {
		
		Perfil perfilSave = perfilRepository.findById(id).get();
		if(perfilSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		perfilSave.setStatus(false);
		
		Perfil perfil = new Perfil();
		BeanUtils.copyProperties(perfilSave, perfil);
		perfil.setStatus(!perfilSave.isStatus());
		
		try {
			LogAcao logAcao = funcoes.logAcao(perfilSave.getId(), getDescricaoExcluir(), funcoes.getLoggedUser());
			logPerfil(perfil, perfilSave, logAcao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return Optional
				.ofNullable(perfilRepository.save(perfilSave))
				.map(perfilAux -> ResponseEntity.ok().body(perfilAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	private void validarPerfil(Perfil perfil) throws GenericException {
		
		if(perfil.getNome() == null || perfil.getNome().isEmpty()) {
			throw new GenericException("Erro","Nome não pode ser nulo");
		}
	}
	
	public Long getDescricaoAdicionar() {
		return DescricaoLogAcaoHelper.ADICIONAR_PERFIL;
	}

	public Long getDescricaoEditar() {
		return DescricaoLogAcaoHelper.EDITAR_PERFIL;
	}

	public Long getDescricaoExcluir() {
		return DescricaoLogAcaoHelper.EXCLUIR_PERFIL;
	}
	
	
	public void logPerfil(Perfil previous, Perfil current, LogAcao logAcao) {
		
		if(previous == null || previous.getId() == null) {
			previous = new Perfil();
		}
		
		List<Log> logs = new ArrayList<Log>();
		
		Date datAlteracao = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:ss:mm");
		
		logUtil.withEntidade("Perfil");
		
		logs.add(logUtil.fromValues("nome_perfil", 
				previous.getNome() == null ? "-" : previous.getNome(),
				current.getNome() == null ? "-" : current.getNome(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("descricao_perfil", 
				previous.getDescricao() == null ? "-" : previous.getDescricao() ,
				current.getDescricao()  == null ? "-" : current.getDescricao() ,
				datAlteracao));
		
		logs.add(logUtil.fromValues("sigla_perfil", 
				previous.getSigla() == null ? "-" : previous.getSigla(),
				current.getSigla() == null ? "-" : current.getSigla(),
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
