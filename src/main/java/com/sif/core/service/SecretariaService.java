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
import com.sif.model.Funcionario;
import com.sif.model.FuncionarioSecretaria;
import com.sif.model.Log;
import com.sif.model.LogAcao;
import com.sif.model.Secretaria;
import com.sif.model.custom.SecretariaCustomDTO;
import com.sif.model.list.SecretariaDTO;
import com.sif.model.utils.DescricaoLogAcaoHelper;
import com.sif.repository.EnderecoRepository;
import com.sif.repository.LogRepository;
import com.sif.repository.OrgaoRepository;
import com.sif.repository.SecretariaRepository;
import com.sif.repository.specification.SecretariaSpecification;

@Service
public class SecretariaService {

	@Autowired
	SecretariaRepository secretariaRepository;
	
	@Autowired
	FuncionarioSecretariaService funcionarioSecretariaService;
	
	@Autowired
	OrgaoRepository orgaoRepository;
	
	@Autowired
	EnderecoRepository enderecoRepository;
	
	@Autowired
	Funcoes funcoes;
	
	@Autowired
	SifLogUtil logUtil;
	
	@Autowired
	LogRepository logRepository;
	
	public Page<Secretaria> getAll(Pageable pageable, Secretaria secretaria) {
		SecretariaSpecification spec = new SecretariaSpecification();
		
		List<SecretariaDTO> listaRetorno = new ArrayList<SecretariaDTO>();
		Page<Secretaria> lista = secretariaRepository.findAll(Specification.where(
				spec.idEquals(secretaria.getId() != null ? secretaria.getId().toString() : null))
			.and(spec.codigoLike(secretaria.getCodigo()))
			.and(spec.descricaoLike(secretaria.getDescricao()))
			.and(spec.siglaEquals(secretaria.getSigla()))
			.and(spec.findByStatus()),pageable);
		
//		for(Secretaria objeto : lista) {
//			SecretariaDTO entity = new SecretariaDTO();
//			entity.setId(objeto.getId());
//			entity.setCodigo(objeto.getCodigo());
//			entity.setDescricao(objeto.getDescricao());
//			entity.setSigla(objeto.getSigla());
//			entity.setOrgao(objeto.getOrgao().getNome());
//			
//			listaRetorno.add(entity);
//		}
//		
//		int start = Integer.parseInt(String.valueOf(pageable.getOffset()));
//		int end = (start + pageable.getPageSize()) > listaRetorno.size() ? listaRetorno.size() : (start + pageable.getPageSize());
		return lista;
	}
	
	public ResponseEntity<SecretariaCustomDTO> findById(Long id) {
		
		SecretariaCustomDTO secretariaDto = new SecretariaCustomDTO();
		Secretaria secretaria = secretariaRepository.findById(id).get();
		BeanUtils.copyProperties(secretaria, secretariaDto);
		secretariaDto.setEndereco(Arrays.asList(funcoes.enderecoToDTO(secretaria.getEndereco())));
		secretariaDto.setOrgao(secretaria.getOrgao().getId());
		
		return Optional
				.ofNullable(secretariaDto)
				.map(secretariaAux -> ResponseEntity.ok().body(secretariaAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<Secretaria> create(SecretariaCustomDTO secretariaDto) {
		
		Secretaria secretaria = dtoToSecretaria(secretariaDto);
		
		validarSecretariaCreate(secretaria);
		
		enderecoRepository.save(secretaria.getEndereco());
		
		Secretaria secretariaSave = secretariaRepository.save(secretaria);
		
		try {
			LogAcao logAcao = funcoes.logAcao(secretariaSave.getId(), getDescricaoAdicionar(), funcoes.getLoggedUser());
			logSecretaria(null, secretaria, logAcao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return Optional
				.ofNullable(secretariaSave)
				.map(secretariaAux -> ResponseEntity.ok().body(secretariaAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public Secretaria save(Secretaria secretaria) {
		return secretariaRepository.save(secretaria);
	}
	
	public ResponseEntity<Secretaria> update(SecretariaCustomDTO secretariaDto,Long id) {
		
		Secretaria secretaria = dtoToSecretaria(secretariaDto);
		
		validarSecretariaUpdate(secretaria);
		
		Secretaria secretariaSave = secretariaRepository.findById(id).get();
		if(secretariaSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		Long idUf = secretariaSave.getId();
		
		try {
			LogAcao logAcao = funcoes.logAcao(secretariaSave.getId(), getDescricaoEditar(), funcoes.getLoggedUser());
			logSecretaria(secretariaSave, secretaria, logAcao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		BeanUtils.copyProperties(secretaria, secretariaSave);
		secretariaSave.setId(idUf);
		
		return Optional
				.ofNullable(secretariaRepository.save(secretariaSave))
				.map(administracaoAux -> ResponseEntity.ok().body(administracaoAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<Secretaria> delete(Long id) {
		
		Secretaria secretariaSave = secretariaRepository.findById(id).get();
		if(secretariaSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		secretariaSave.setStatus(false);
		
		Secretaria secretaria = new Secretaria();
		BeanUtils.copyProperties(secretariaSave, secretaria);
		secretaria.setStatus(!secretariaSave.isStatus());
		
		try {
			LogAcao logAcao = funcoes.logAcao(secretariaSave.getId(), getDescricaoExcluir(), funcoes.getLoggedUser());
			logSecretaria(secretaria, secretariaSave, logAcao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return Optional
				.ofNullable(secretariaRepository.save(secretariaSave))
				.map(secretariaAux -> ResponseEntity.ok().body(secretariaAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	private void validarSecretariaCreate(Secretaria secretaria) throws GenericException {
		
		if(secretaria.getEndereco() == null) {
			throw new GenericException("Erro","Endereço não pode ser nulo");
		}
		if(secretaria.getOrgao() == null) {
			throw new GenericException("Erro","Orgão não pode ser nulo");
		}
		if(secretaria.getCodigo() == null || secretaria.getCodigo().isEmpty()) {
			throw new GenericException("Erro","Codigo não pode ser nulo");
		}
		if(secretaria.getDescricao() == null || secretaria.getDescricao().isEmpty()) {
			throw new GenericException("Erro","Descrição não pode ser nulo");
		}
		
		Secretaria secretariaSave = secretariaRepository.findByCodigo(secretaria.getCodigo());
		
		if(secretariaSave != null ) {
			if(secretariaSave.getCodigo().equals(secretaria.getCodigo()) && !secretariaSave.getId().equals(secretaria.getId())) {
				throw new GenericException("Erro","Já existe Secretaria cadastrada com este código");
			}
			
		}
	}
	
	private void validarSecretariaUpdate(Secretaria secretaria) throws GenericException {
		
		validarSecretariaCreate(secretaria);
		
		
	}
	
	private Secretaria dtoToSecretaria(SecretariaCustomDTO secretariaDto) {
		
		Secretaria secretaria = new Secretaria();
		BeanUtils.copyProperties(secretariaDto, secretaria);

		if(!secretariaDto.getEndereco().isEmpty()) {
			secretaria.setEndereco(funcoes.dtoToEndereco(secretariaDto.getEndereco().get(0)));
		}
		
		if(secretariaDto.getOrgao() != null) {
			secretaria.setOrgao(orgaoRepository.findById(secretariaDto.getOrgao()).get());
		}
		
		return secretaria;
		
	}
	
	public List<Secretaria> getSecretariaByFuncionario(Funcionario funcionario) {
		
		List<FuncionarioSecretaria> funcScretarias = funcionarioSecretariaService.findByFuncionario(funcionario);
	
		List<Secretaria> secretarias = new ArrayList<Secretaria>();
		
		if(funcScretarias != null) {
			for(FuncionarioSecretaria funcScretaria : funcScretarias) {
				secretarias.add(funcScretaria.getSecretaria());
			}
			
			return secretarias;
		}
		
		return null;
	}
	
	public Long getDescricaoAdicionar() {
		return DescricaoLogAcaoHelper.ADICIONAR_SECRETARIA;
	}

	public Long getDescricaoEditar() {
		return DescricaoLogAcaoHelper.EDITAR_SECRETARIA;
	}

	public Long getDescricaoExcluir() {
		return DescricaoLogAcaoHelper.EXCLUIR_SECRETARIA;
	}
	
	public void logSecretaria(Secretaria previous, Secretaria current, LogAcao logAcao) {
		
		if(previous == null || previous.getId() == null) {
			previous = new Secretaria();
		}
		
		List<Log> logs = new ArrayList<Log>();
		
		Date datAlteracao = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:ss:mm");
		
		logUtil.withEntidade("Secretaria");
		
		logs.add(logUtil.fromValues("id_endereco", 
				(previous.getEndereco() == null || previous.getEndereco().getId() == null) ? "-" : previous.getEndereco().getId().toString(),
				(current.getEndereco() == null || current.getEndereco().getId() == null) ? "-" : current.getEndereco().getId().toString(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("id_orgao", 
				(previous.getOrgao() == null || previous.getOrgao().getId() == null) ? "-" : previous.getOrgao().getId().toString(),
				(current.getOrgao() == null || current.getOrgao().getId() == null) ? "-" : current.getOrgao().getId().toString(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("codigo_secretaria", 
				previous.getCodigo() == null ? "-" : previous.getCodigo(),
				current.getCodigo() == null ? "-" : current.getCodigo(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("descricao_secretaria",
				previous.getDescricao() == null ? "-" : previous.getDescricao(),
				current.getDescricao() == null ? "-" : current.getDescricao(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("sigla_secretaria", 
				previous.getSigla() == null ? "-" : previous.getSigla(),
				current.getSigla() == null ? "-" : current.getSigla(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("status_secretaria", 
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

	public Secretaria findByCodigo(String codigo) {
		
		return secretariaRepository.findByCodigo(codigo);
	}
	
}
