package com.sif.core.service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.sif.model.Log;
import com.sif.model.LogAcao;
import com.sif.model.custom.AdministracaoCustomDTO;
import com.sif.model.custom.EnderecoCustomDTO;
import com.sif.model.utils.DescricaoLogAcaoHelper;
import com.sif.repository.AdministracaoRepository;
import com.sif.repository.EnderecoRepository;
import com.sif.repository.LogRepository;
import com.sif.repository.specification.AdministracaoSpecification;

@Service
@JsonSerialize
public class AdministracaoService {

	@Autowired
	AdministracaoRepository administracaoRepository;
	
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
	
	public Page<Administracao> getAll(Pageable pageable, Administracao administracao) {
		AdministracaoSpecification spec = new AdministracaoSpecification();
		
//		List<AdministracaoDTO> listaRetorno = new ArrayList<AdministracaoDTO>();
		Page<Administracao> lista = administracaoRepository.findAll(Specification.where(
				spec.idEquals(administracao.getId() != null ? administracao.getId().toString() : null))
			.and(spec.nomeLike(administracao.getNome()))
			.and(spec.emailLike(administracao.getEmail()))
			.and(spec.documentoLike(administracao.getDocumento()))
			.and(spec.findByStatus())
			.and(spec.orderByNome()),pageable);
		
//		for(Administracao objeto : lista) {
//			AdministracaoDTO entity = new AdministracaoDTO();
//			entity.setId(objeto.getId());
//			entity.setNome(objeto.getNome());
//			entity.setEmail(objeto.getEmail());
//			entity.setDocumento(objeto.getDocumento());
//			
//			listaRetorno.add(entity);
//		}
//		
//		int start = Integer.parseInt(String.valueOf(pageable.getOffset()));
//		int end = (start + pageable.getPageSize()) > listaRetorno.size() ? listaRetorno.size() : (start + pageable.getPageSize());
		return lista;
	}
	
	public ResponseEntity<AdministracaoCustomDTO> findById(Long id) {
		
		Administracao administracao = administracaoRepository.findById(id).get();
		
		AdministracaoCustomDTO administracaoDto = new AdministracaoCustomDTO();
		administracaoDto.setDocumento(administracao.getDocumento());
		administracaoDto.setEmail(administracao.getEmail());
		
		EnderecoCustomDTO enderecoDto = new EnderecoCustomDTO();
		enderecoDto = funcoes.enderecoToDTO(administracao.getEndereco());
		
		administracaoDto.setEndereco(Arrays.asList(enderecoDto));
		
		administracaoDto.setId(administracao.getId());
		administracaoDto.setNome(administracao.getNome());
		administracaoDto.setStatus(administracao.isStatus());
		
		
		return Optional
				.ofNullable(administracaoDto)
				.map(administracaoAux -> ResponseEntity.ok().body(administracaoAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public Administracao findAdministracaoById(Long id) {
		
		if(id == null) {
			return null;
		}
		
		AdministracaoSpecification administracaoSpecification = new AdministracaoSpecification();
		Optional<Administracao> optional = administracaoRepository.findOne(Specification.where(
			administracaoSpecification.idLongEquals(id)
		));
		
		if(optional.isPresent()) {
			return optional.get();
		}
		
		return null;
	}
	
	@Transactional
	public ResponseEntity<Administracao> create(AdministracaoCustomDTO administracaoDto) {
		
		Administracao administracao = dtoToAdministracao(administracaoDto);
		
		validarAdministracao(administracao);
		
		enderecoRepository.save(administracao.getEndereco());
		
		Administracao administracaoSave = administracaoRepository.save(administracao);
		
		try {
			LogAcao logAcao = funcoes.logAcao(administracaoSave.getId(), getDescricaoAdicionar(), funcoes.getLoggedUser());
			logAdministracao(null, administracaoSave, logAcao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		ResponseEntity<Administracao> responseEntity = Optional
				.ofNullable(administracaoSave)
				.map(administracaoAux -> ResponseEntity.ok().body(administracaoAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
		
		try {
			ticketService.createEntity(administracaoSave);
		} catch (IOException | URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	

		return responseEntity;
	}
	
	public ResponseEntity<Administracao> update(AdministracaoCustomDTO administracaoDto,Long id) {
		
		Administracao administracao = dtoToAdministracao(administracaoDto);
		
		validarAdministracao(administracao);
		
		Administracao administracaoSave = administracaoRepository.findById(id).get();
		if(administracaoSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		Long idUf = administracaoSave.getId();
		administracao.setId(administracaoSave.getId());
		administracao.setItsmID(administracao.getItsmID());
		
		try {
			LogAcao logAcao = funcoes.logAcao(administracaoSave.getId(), getDescricaoEditar(), funcoes.getLoggedUser());
			logAdministracao(administracaoSave, administracao, logAcao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		BeanUtils.copyProperties(administracao, administracaoSave);
		administracaoSave.setId(idUf);
		
		
		
		return Optional
				.ofNullable(administracaoRepository.save(administracaoSave))
				.map(administracaoAux -> ResponseEntity.ok().body(administracaoAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<Administracao> delete(Long id) {
		
		Administracao administracao = administracaoRepository.findById(id).get();
		if(administracao == null) {
			return ResponseEntity.notFound().build();
		}		
		
		administracao.setStatus(false);
		
		Administracao administracaoSave = new Administracao();
		BeanUtils.copyProperties(administracao, administracaoSave);
		administracaoSave.setStatus(!administracao.isStatus());
		
		administracaoRepository.save(administracao);
		
		try {
			LogAcao logAcao = funcoes.logAcao(administracao.getId(), getDescricaoExcluir(), funcoes.getLoggedUser());
			logAdministracao(administracao, administracaoSave, logAcao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		ResponseEntity<Administracao> responseEntity = Optional
				.ofNullable(administracao)
				.map(administracaoAux -> ResponseEntity.ok().body(administracaoAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
		
		try {
			ticketService.deleteEntity(administracaoSave);
		} catch (IOException | URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return responseEntity;
	}
	
	private void validarAdministracao(Administracao administracao) throws GenericException {
		
		if(administracao.getEndereco() == null) {
			throw new GenericException("Erro","Endereço não pode ser nulo");
		}
		if(administracao.getNome() == null || administracao.getNome().isEmpty()) {
			throw new GenericException("Erro","Nome não pode ser nulo");
		}
		if(administracao.getDocumento() == null || administracao.getDocumento().isEmpty()) {
			throw new GenericException("Erro","Documento não pode ser nulo");
		}
		Administracao administracaoByCnpj = administracaoRepository.findByDocumento(administracao.getDocumento());
		if(administracaoByCnpj != null) {
			if(administracao.getId() != null) {
				if(!administracao.getId().equals(administracaoByCnpj.getId())) {
					throw new GenericException("Erro","Já existe uma Administração com este documento!");
				}
			} else {
				throw new GenericException("Erro","Já existe uma Administração com este documento!");
			}
			
		}
	}
	
	private Administracao dtoToAdministracao(AdministracaoCustomDTO administracaoDto) {
		
		Administracao administracao = new Administracao();
		BeanUtils.copyProperties(administracaoDto, administracao);
		administracao.setEndereco(funcoes.dtoToEndereco(administracaoDto.getEndereco().get(0)));
		return administracao;
		
	}
	
	public Long getDescricaoAdicionar() {
		return DescricaoLogAcaoHelper.ADICIONAR_ADMINISTRACAO;
	}

	public Long getDescricaoEditar() {
		return DescricaoLogAcaoHelper.EDITAR_ADMINISTRACAO;
	}

	public Long getDescricaoExcluir() {
		return DescricaoLogAcaoHelper.EXCLUIR_ADMINISTRACAO;
	}
	
	public void logAdministracao(Administracao previous, Administracao current, LogAcao logAcao){
		
		if(previous == null || previous.getId() == null) {
			previous = new Administracao();
		}
		
		List<Log> logs = new ArrayList<Log>();
		Date datAlteracao = new Date();
		
		logUtil.withEntidade("Administração");
		
		logs.add(logUtil.fromValues("nome_administracao", 
				previous.getNome() == null ? "-" : previous.getNome(), 
				current.getNome() == null ? "-" : current.getNome(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("documento_administracao", 
				previous.getDocumento() == null ? "-" : previous.getDocumento(), 
				current.getDocumento() == null ? "-" : current.getDocumento(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("email_administracao", 
				previous.getEmail() == null ? "-" : previous.getEmail(), 
				current.getEmail() == null ? "-" : current.getEmail(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("status_administracao", 
				Boolean.toString(previous.isStatus()), 
				Boolean.toString(current.isStatus()),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("itsm_id", 
				previous.getItsmID() == null ? "-" : previous.getItsmID(), 
				current.getItsmID() == null ? "-" : current.getItsmID(),
				datAlteracao
		));
		
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
