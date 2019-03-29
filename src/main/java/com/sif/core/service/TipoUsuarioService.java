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
import com.sif.model.TipoUsuario;
import com.sif.model.list.TipoUsuarioDTO;
import com.sif.model.utils.DescricaoLogAcaoHelper;
import com.sif.repository.LogRepository;
import com.sif.repository.TipoUsuarioRepository;
import com.sif.repository.specification.TipoUsuarioSpecification;

@Service
public class TipoUsuarioService {

	@Autowired
	TipoUsuarioRepository tipoUsuarioRepository;
	
	@Autowired
	SifLogUtil logUtil;
	
	@Autowired
	Funcoes funcoes;
	
	@Autowired
	LogRepository logRepository;
	
	public Page<TipoUsuario> getAll(Pageable pageable, TipoUsuario tipoUsuario) {
		TipoUsuarioSpecification spec = new TipoUsuarioSpecification();
		
		List<TipoUsuarioDTO> listaRetorno = new ArrayList<TipoUsuarioDTO>();
		Page<TipoUsuario> lista = tipoUsuarioRepository.findAll(Specification.where(
				spec.idEquals(tipoUsuario.getId() != null ? tipoUsuario.getId().toString() : null))
			.and(spec.codigoLike(tipoUsuario.getCodigo()))
			.and(spec.nomeLike(tipoUsuario.getNome()))
			.and(spec.findByStatus()),pageable);
		
//		for(TipoUsuario objeto : lista) {
//			TipoUsuarioDTO entity = new TipoUsuarioDTO();
//			entity.setId(objeto.getId());
//			entity.setCodigo(objeto.getCodigo());
//			entity.setNome(objeto.getNome());
//			
//			listaRetorno.add(entity);
//		}
//		
//		int start = Integer.parseInt(String.valueOf(pageable.getOffset()));
//		int end = (start + pageable.getPageSize()) > listaRetorno.size() ? listaRetorno.size() : (start + pageable.getPageSize());
		return lista;
	}
	
	public ResponseEntity<TipoUsuario> findById(Long id) {
		return Optional
				.ofNullable(tipoUsuarioRepository.findById(id).get())
				.map(tipoUsuario -> ResponseEntity.ok().body(tipoUsuario))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<TipoUsuario> create(TipoUsuario tipoUsuario) {
		
		validarTipoUsuario(tipoUsuario);
	
		TipoUsuario tipoUsuarioSave = tipoUsuarioRepository.save(tipoUsuario);
		
		try {
			LogAcao logAcao = funcoes.logAcao(tipoUsuario.getId(), getDescricaoAdicionar(), funcoes.getLoggedUser());
			logTipoUsuario(null, tipoUsuario, logAcao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return Optional
				.ofNullable(tipoUsuarioSave)
				.map(tipoUsuarioAux -> ResponseEntity.ok().body(tipoUsuarioAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<TipoUsuario> update(TipoUsuario tipoUsuario,Long id) {
		
		validarTipoUsuario(tipoUsuario);
		
		TipoUsuario tipoUsuarioSave = tipoUsuarioRepository.findById(id).get();
		if(tipoUsuarioSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		Long tipoUsuarioId = tipoUsuarioSave.getId();
		
		try {
			LogAcao logAcao = funcoes.logAcao(tipoUsuarioSave.getId(), getDescricaoEditar(), funcoes.getLoggedUser());
			logTipoUsuario(tipoUsuarioSave, tipoUsuario, logAcao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		BeanUtils.copyProperties(tipoUsuario, tipoUsuarioSave);
		tipoUsuarioSave.setId(tipoUsuarioId);
		
		return Optional
				.ofNullable(tipoUsuarioRepository.save(tipoUsuarioSave))
				.map(tipoUsuarioAux -> ResponseEntity.ok().body(tipoUsuarioAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<TipoUsuario> delete(Long id) {
		
		TipoUsuario tipoUsuarioSave = tipoUsuarioRepository.findById(id).get();
		if(tipoUsuarioSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		tipoUsuarioSave.setStatus(false);
		
		TipoUsuario tipoUsuario = new TipoUsuario();
		BeanUtils.copyProperties(tipoUsuarioSave, tipoUsuario);
		tipoUsuario.setStatus(!tipoUsuarioSave.isStatus());
		
		try {
			LogAcao logAcao = funcoes.logAcao(tipoUsuarioSave.getId(), getDescricaoExcluir(), funcoes.getLoggedUser());
			logTipoUsuario(tipoUsuario, tipoUsuarioSave, logAcao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return Optional
				.ofNullable(tipoUsuarioRepository.save(tipoUsuarioSave))
				.map(tipoUsuarioAux -> ResponseEntity.ok().body(tipoUsuarioAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	private void validarTipoUsuario(TipoUsuario tipoUsuario) throws GenericException {
		
		if(tipoUsuario.getNome() == null || tipoUsuario.getNome().isEmpty()) {
			throw new GenericException("Erro","Nome não pode ser nulo");
		}
		
		List<TipoUsuario> listaTipo = tipoUsuarioRepository.findAll(Specification.where(new TipoUsuarioSpecification().findByStatus()));
		
		for(TipoUsuario tipo : listaTipo) {
			if(tipoUsuario.getCodigo().equals(tipo.getCodigo()) && tipoUsuario.getId() != tipo.getId()) {
				throw new GenericException("Erro","Código já existente");
			}
		}
	}
	
	public Long getDescricaoAdicionar() {
		return DescricaoLogAcaoHelper.ADICIONAR_TIPO_USUARIO;
	}

	public Long getDescricaoEditar() {
		return DescricaoLogAcaoHelper.EDITAR_TIPO_USUARIO;
	}

	public Long getDescricaoExcluir() {
		return DescricaoLogAcaoHelper.EXCLUIR_TIPO_USUARIO;
	}
	
	public void logTipoUsuario(TipoUsuario existingTipoUsuario, TipoUsuario currentTipoUsuario, LogAcao logAcao) {
		if(existingTipoUsuario == null || existingTipoUsuario.getId() == null) {
			existingTipoUsuario = new TipoUsuario();
		}
		
		List<Log> logs = new ArrayList<Log>();
		
		Date datAlteracao = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:ss:mm");
		
		logUtil.withEntidade("TipoUsuario");
		
		logs.add(logUtil.fromValues("nome_tipo_usuario", 
				existingTipoUsuario.getNome() == null ? "-" : existingTipoUsuario.getNome(),
				currentTipoUsuario.getNome() == null ? "-" : currentTipoUsuario.getNome(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("codigo_tipo_usuario", 
				existingTipoUsuario.getCodigo() == null ? "-" : existingTipoUsuario.getCodigo(),
				currentTipoUsuario.getCodigo() == null ? "-" : currentTipoUsuario.getCodigo(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("status_tipo_usuario", 
				existingTipoUsuario.isStatus() ? "ativo" : "inativo",
				currentTipoUsuario.isStatus() ? "ativo" : "inativo",
				datAlteracao));
		
		if(logs.isEmpty()) {
			return;
		}
		
		for(Log log : logs) {
			if(log != null) {
				log.setIdRow(currentTipoUsuario.getId());
				log.setLogAcao(logAcao);
				logRepository.save(log);
			}
		}
		
	}
	
}
