package com.sif.core.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.sif.model.UnidadeAdministrativa;
import com.sif.model.custom.UnidadeAdministrativaCustomDTO;
import com.sif.model.list.UnidadeAdministrativaDTO;
import com.sif.model.utils.DescricaoLogAcaoHelper;
import com.sif.repository.EnderecoRepository;
import com.sif.repository.LogRepository;
import com.sif.repository.SecretariaRepository;
import com.sif.repository.UnidadeAdministrativaRepository;
import com.sif.repository.specification.UnidadeAdministrativaSpecification;

@Service
public class UnidadeAdministrativaService {

	@Autowired
	UnidadeAdministrativaRepository unidadeAdministrativaRepository;
	
	@Autowired
	SecretariaRepository secretariaRepository;
	
	@Autowired
	EnderecoRepository enderecoRepository;
	
	@Autowired
	Funcoes funcoes;
	
	@Autowired
	SifLogUtil logUtil;
	
	@Autowired
	LogRepository logRepository;
	
	public Page<UnidadeAdministrativa> getAll(Pageable pageable, UnidadeAdministrativa unidadeAdministrativa) {
		UnidadeAdministrativaSpecification spec = new UnidadeAdministrativaSpecification();
		
		List<UnidadeAdministrativaDTO> listaRetorno = new ArrayList<UnidadeAdministrativaDTO>();
		Page<UnidadeAdministrativa> lista = unidadeAdministrativaRepository.findAll(Specification.where(
				spec.idEquals(unidadeAdministrativa.getId() != null ? unidadeAdministrativa.getId().toString() : null))
			.and(spec.secretariaEqual(unidadeAdministrativa.getSecretaria() != null ? unidadeAdministrativa.getSecretaria().getId() : null))
			.and(spec.descricaoLike(unidadeAdministrativa.getDescricao()))
			.and(spec.codigoLike(unidadeAdministrativa.getCodigo()))
			.and(spec.findByStatus()), pageable);
		
//		for(UnidadeAdministrativa objeto : lista) {
//			UnidadeAdministrativaDTO entity = new UnidadeAdministrativaDTO();
//			entity.setId(objeto.getId());
//			entity.setDescricao(objeto.getDescricao());
//			entity.setCodigo(objeto.getCodigo());
//			entity.setSecretaria(objeto.getSecretaria().getDescricao());
//			
//			listaRetorno.add(entity);
//		}
//		
//		int start = Integer.parseInt(String.valueOf(pageable.getOffset()));
//		int end = (start + pageable.getPageSize()) > listaRetorno.size() ? listaRetorno.size() : (start + pageable.getPageSize());
		return lista;
	}
	
	public ResponseEntity<UnidadeAdministrativaCustomDTO> findById(Long id) {
		
		UnidadeAdministrativaCustomDTO unidadeAdministrativaDto = new UnidadeAdministrativaCustomDTO();
		UnidadeAdministrativa unidadeAdministrativa = unidadeAdministrativaRepository.findById(id).get();
		BeanUtils.copyProperties(unidadeAdministrativa, unidadeAdministrativaDto);
		unidadeAdministrativaDto.setEndereco(Arrays.asList(funcoes.enderecoToDTO(unidadeAdministrativa.getEndereco())));
		unidadeAdministrativaDto.setSecretaria(unidadeAdministrativa.getSecretaria().getId());
		
		return Optional
				.ofNullable(unidadeAdministrativaDto)
				.map(unidadeAdministrativaAux -> ResponseEntity.ok().body(unidadeAdministrativaAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<UnidadeAdministrativa> create(UnidadeAdministrativaCustomDTO unidadeAdministrativaDto) {
		
		UnidadeAdministrativa unidadeAdministrativa = dtoToUnidadeAdministrativa(unidadeAdministrativaDto);
		
		validarUnidadeAdministrativa(unidadeAdministrativa);
		
		enderecoRepository.save(unidadeAdministrativa.getEndereco());
		
		UnidadeAdministrativa unidadeAdministrativaSave = unidadeAdministrativaRepository.save(unidadeAdministrativa);
		
		try {
			LogAcao logAcao = funcoes.logAcao(unidadeAdministrativaSave.getId(), getDescricaoAdicionar(), funcoes.getLoggedUser());
			logUnidadeAdministrativa(null, unidadeAdministrativa, logAcao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return Optional
				.ofNullable(unidadeAdministrativaSave)
				.map(unidadeAdministrativaAux -> ResponseEntity.ok().body(unidadeAdministrativaAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<UnidadeAdministrativa> update(UnidadeAdministrativaCustomDTO unidadeAdministrativaDto,Long id) {
		
		UnidadeAdministrativa unidadeAdministrativa = dtoToUnidadeAdministrativa(unidadeAdministrativaDto);
		
		validarUnidadeAdministrativa(unidadeAdministrativa);
		
		UnidadeAdministrativa unidadeAdministrativaSave = unidadeAdministrativaRepository.findById(id).get();
		if(unidadeAdministrativaSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		Long unidadeAdministrativaId = unidadeAdministrativaSave.getId();
		
		try {
			LogAcao logAcao = funcoes.logAcao(unidadeAdministrativaSave.getId(), getDescricaoEditar(), funcoes.getLoggedUser());
			logUnidadeAdministrativa(unidadeAdministrativaSave, unidadeAdministrativa, logAcao );
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		BeanUtils.copyProperties(unidadeAdministrativa, unidadeAdministrativaSave);
		unidadeAdministrativaSave.setId(unidadeAdministrativaId);
		
		return Optional
				.ofNullable(unidadeAdministrativaRepository.save(unidadeAdministrativaSave))
				.map(unidadeAdministrativaAux -> ResponseEntity.ok().body(unidadeAdministrativaAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<UnidadeAdministrativa> delete(Long id) {
		
		UnidadeAdministrativa unidadeAdministrativaSave = unidadeAdministrativaRepository.findById(id).get();
		if(unidadeAdministrativaSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		unidadeAdministrativaSave.setStatus(false);
		
		UnidadeAdministrativa unidadeAdministrativa = new UnidadeAdministrativa();
		BeanUtils.copyProperties(unidadeAdministrativaSave, unidadeAdministrativa);
		unidadeAdministrativa.setStatus(!unidadeAdministrativaSave.isStatus());
		
		try {
			LogAcao logAcao = funcoes.logAcao(unidadeAdministrativaSave.getId(), getDescricaoExcluir(), funcoes.getLoggedUser());
			logUnidadeAdministrativa(unidadeAdministrativa, unidadeAdministrativaSave, logAcao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return Optional
				.ofNullable(unidadeAdministrativaRepository.save(unidadeAdministrativaSave))
				.map(perfilAux -> ResponseEntity.ok().body(perfilAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	private void validarUnidadeAdministrativa(UnidadeAdministrativa unidadeAdministrativa) throws GenericException {
		
		if(unidadeAdministrativa.getDescricao() == null || unidadeAdministrativa.getDescricao().isEmpty()) {
			throw new GenericException("Erro","Nome não pode ser nulo");
		}
		if(unidadeAdministrativa.getSecretaria() == null) {
			throw new GenericException("Erro","Secretaria não pode ser nulo");
		}
	}
	
	private UnidadeAdministrativa dtoToUnidadeAdministrativa(UnidadeAdministrativaCustomDTO unidadeAdministrativaDto) {
		
		UnidadeAdministrativa unidadeAdministrativa = new UnidadeAdministrativa();
		BeanUtils.copyProperties(unidadeAdministrativaDto, unidadeAdministrativa);
		unidadeAdministrativa.setEndereco(funcoes.dtoToEndereco(unidadeAdministrativaDto.getEndereco().get(0)));
		unidadeAdministrativa.setSecretaria(secretariaRepository.findById(unidadeAdministrativaDto.getSecretaria()).get());
		
		return unidadeAdministrativa;
		
	}
	
	public Long getDescricaoAdicionar() {
		return DescricaoLogAcaoHelper.ADICIONAR_UNIDADE_ADMINISTRATIVA;
	}

	public Long getDescricaoEditar() {
		return DescricaoLogAcaoHelper.EDITAR_UNIDADE_ADMINISTRATIVA;
	}

	public Long getDescricaoExcluir() {
		return DescricaoLogAcaoHelper.EXCLUIR_UNIDADE_ADMINISTRATIVA;
	}
	
	public void logUnidadeAdministrativa(UnidadeAdministrativa previous, UnidadeAdministrativa current, LogAcao logAcao) {
		
		if(previous == null || previous.getId() == null) {
			previous = new UnidadeAdministrativa();
		}
		
		List<Log> logs = new ArrayList<Log>();
		
		Date datAlteracao = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:ss:mm");
		
		logUtil.withEntidade("UnidadeAdministrativa");
		
		logs.add(logUtil.fromValues("id_endereco", 
				(previous.getEndereco() == null || previous.getEndereco().getId() == null) ? "-" : previous.getEndereco().getId().toString(),
				(current.getEndereco() == null || current.getEndereco().getId() == null) ? "-" : current.getEndereco().getId().toString(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("id_secretaria", 
				(previous.getSecretaria() == null || previous.getSecretaria().getId() == null) ? "-" : previous.getSecretaria().getId().toString(),
				(current.getSecretaria() == null || current.getSecretaria().getId() == null) ? "-" : current.getSecretaria().getId().toString(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("nome_unid_admin", 
				previous.getDescricao() == null ? "-" : previous.getDescricao(),
				current.getDescricao() == null ? "-" : current.getDescricao(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("status_unid_admin", 
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

	public UnidadeAdministrativa save(UnidadeAdministrativa unidadeAdministrativa) {
		return unidadeAdministrativaRepository.save(unidadeAdministrativa);
	}
	
	public UnidadeAdministrativa findByCodigo(String codigo) {
		return unidadeAdministrativaRepository.findByCodigo(codigo);
	}
	
}
