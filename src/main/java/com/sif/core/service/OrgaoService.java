package com.sif.core.service;

import java.io.IOException;
import java.net.URISyntaxException;
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
import com.sif.model.Orgao;
import com.sif.model.custom.OrgaoCustomDTO;
import com.sif.model.list.OrgaoDTO;
import com.sif.model.utils.DescricaoLogAcaoHelper;
import com.sif.repository.EnderecoRepository;
import com.sif.repository.LogRepository;
import com.sif.repository.OrgaoRepository;
import com.sif.repository.specification.OrgaoSpecification;

@Service
public class OrgaoService {

	@Autowired
	OrgaoRepository orgaoRepository;
	
	@Autowired
	EnderecoRepository enderecoRepository;
	
	@Autowired
	TicketService ticketService;
	
	@Autowired
	Funcoes funcoes;
	
	@Autowired
	LogRepository logRepository;	
	
	@Autowired
	SifLogUtil logUtil;
	
	public Page<Orgao> getAll(Pageable pageable, Orgao orgao) {
		OrgaoSpecification spec = new OrgaoSpecification();
		
		List<OrgaoDTO> listaRetorno = new ArrayList<OrgaoDTO>();
		Page<Orgao> lista = orgaoRepository.findAll(Specification.where(
				spec.idEquals(orgao.getId() != null ? orgao.getId().toString() : null))
			.and(spec.nomeLike(orgao.getNome()))
			.and(spec.siglaLike(orgao.getSigla()))
			.and(spec.emailLike(orgao.getEmail()))
			.and(spec.cnpjLike(orgao.getCnpj()))
			.and(spec.findByStatus())
			.and(spec.orderByNome()),pageable);
		
//		for(Orgao objeto : lista) {
//			OrgaoDTO entity = new OrgaoDTO();
//			entity.setId(objeto.getId());
//			entity.setNome(objeto.getNome());
//			entity.setSigla(objeto.getSigla());
//			entity.setEmail(objeto.getEmail());
//			entity.setCnpj(objeto.getCnpj());
//			
//			listaRetorno.add(entity);
//		}
//		
//		int start = Integer.parseInt(String.valueOf(pageable.getOffset()));
//		int end = (start + pageable.getPageSize()) > listaRetorno.size() ? listaRetorno.size() : (start + pageable.getPageSize());
		return lista;
	}
	
	public ResponseEntity<OrgaoCustomDTO> findById(Long id) {
		
		OrgaoCustomDTO orgaoDto = new OrgaoCustomDTO();
		Orgao orgao = orgaoRepository.findById(id).get();
		BeanUtils.copyProperties(orgao, orgaoDto);
		orgaoDto.setEndereco(Arrays.asList(funcoes.enderecoToDTO(orgao.getEndereco())));
		
		return Optional
				.ofNullable(orgaoDto)
				.map(orgaoAux -> ResponseEntity.ok().body(orgaoAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public Orgao findOrgaoById(Long id) {
		
		OrgaoSpecification specification = new OrgaoSpecification();
		
		if(id == null) {
			return null;
		}
		
		Optional<Orgao> optional = orgaoRepository.findOne(Specification.where(
			specification.idLongEquals(id)
		));
		
		if(optional.isPresent()) {
			return optional.get();
		}
		
		return null;
	}
	
	public ResponseEntity<Orgao> create(OrgaoCustomDTO orgaoDto) {
		
		Orgao orgao = dtoToOrgao(orgaoDto);
		
		validarOrgao(orgao);
		
		enderecoRepository.save(orgao.getEndereco());
		
		ResponseEntity<Orgao> responseEntity = Optional
				.ofNullable(orgaoRepository.save(orgao))
				.map(orgaoAux -> ResponseEntity.ok().body(orgaoAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
		
		try {
			LogAcao logAcao = funcoes.logAcao(responseEntity.getBody().getId(), getDescricaoAdicionar(), funcoes.getLoggedUser());
			logOrgao(null, orgao, logAcao);
		} catch (Exception e) {
			e.printStackTrace();
		}	
		
		try {
			ticketService.createEntity(orgao);
		} catch (IOException | URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return responseEntity;
	}
	
	public ResponseEntity<Orgao> update(OrgaoCustomDTO orgaoDto,Long id) {
		
		Orgao orgao = dtoToOrgao(orgaoDto);
		
		validarOrgao(orgao);
		
		Orgao orgaoSave = orgaoRepository.findById(id).get();
		if(orgaoSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		Long idOrgao = orgaoSave.getId();
		orgao.setId(orgaoSave.getId());
		orgao.setItsmID(orgaoSave.getItsmID());
		
		try {
			LogAcao logAcao = funcoes.logAcao(orgaoSave.getId(), getDescricaoEditar(), funcoes.getLoggedUser());
			logOrgao(orgaoSave, orgao, logAcao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		BeanUtils.copyProperties(orgao, orgaoSave);
		orgaoSave.setId(idOrgao);
		
		return Optional
				.ofNullable(orgaoRepository.save(orgaoSave))
				.map(orgaoAux -> ResponseEntity.ok().body(orgaoAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<Orgao> delete(Long id) {
		
		Orgao orgao = orgaoRepository.findById(id).get();
		if(orgao == null) {
			return ResponseEntity.notFound().build();
		}
		
		orgao.setStatus(false);
		
		orgaoRepository.save(orgao);
		
		Orgao orgaoSave = new Orgao();
		BeanUtils.copyProperties(orgao, orgaoSave);
		orgaoSave.setStatus(!orgao.isStatus());
		
		ResponseEntity<Orgao> responseEntity = Optional
				.ofNullable(orgao)
				.map(orgaoAux -> ResponseEntity.ok().body(orgaoAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
		
		try {
			LogAcao logAcao = funcoes.logAcao(responseEntity.getBody().getId(), getDescricaoExcluir(), funcoes.getLoggedUser());
			logOrgao(orgaoSave, orgao, logAcao);
		} catch (Exception e) {
			e.printStackTrace();
		}
				
		try {
			ticketService.deleteEntity(orgaoSave);
		} catch (IOException | URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return responseEntity;
	}
	
	private void validarOrgao(Orgao orgao) throws GenericException {
		
		if(orgao.getNome() == null || orgao.getNome().isEmpty()) {
			throw new GenericException("Erro","Nome não pode ser nulo");
		}
		if(orgao.getEmail() == null || orgao.getEmail().isEmpty()) {
			throw new GenericException("Erro","Email não pode ser nulo");
		}
		if(orgao.getFolhaAtual() == null) {
			throw new GenericException("Erro","Folha atual não pode ser nulo");
		}
		if(orgao.getFechamento() == null) {
			throw new GenericException("Erro","Fechamento não pode ser nulo");
		}
		if(orgao.getFechamento() < 1 || orgao.getFechamento() > 31) {
			throw new GenericException("Erro","Fechamento tem que ser entre 1 e 31");
		}
		
		String[] dataFolhaAtual = orgao.getFolhaAtual().split("/");
		
		if(dataFolhaAtual.length != 2) {
			throw new GenericException("Erro","Folha atual inválida");
		}
		
		if(dataFolhaAtual[0].isEmpty() || dataFolhaAtual[1].isEmpty()) {
			throw new GenericException("Erro","Folha atual inválida");
		}
		
		if(Integer.parseInt(dataFolhaAtual[0]) < 2000 || Integer.parseInt(dataFolhaAtual[0]) > 2100) {
			throw new GenericException("Erro","Ano inválido da Folha Atual");
		}
		if(Integer.parseInt(dataFolhaAtual[1]) < 1 || Integer.parseInt(dataFolhaAtual[1]) > 12) {
			throw new GenericException("Erro","Mês inválido da Folha Atual");
		}
		if(!funcoes.isValidCNPJ(orgao.getCnpj())) {
			throw new GenericException("Erro","CNPJ inválido");
		}
		Orgao orgaoByCnpj = orgaoRepository.findByCnpj(orgao.getCnpj());
		if(orgaoByCnpj != null) {
			if(orgao.getId() != null) {
				if(!orgao.getId().equals(orgaoByCnpj.getId())) {
					throw new GenericException("Erro","Já existe um Orgão com este CNPJ!");
				}
			}
			else {
				throw new GenericException("Erro","Já existe um Orgão com este CNPJ!");
			}
		}
	}
	
	private Orgao dtoToOrgao(OrgaoCustomDTO orgaoDto) {
		
		Orgao orgao = new Orgao();
		BeanUtils.copyProperties(orgaoDto, orgao);
		orgao.setEndereco(funcoes.dtoToEndereco(orgaoDto.getEndereco().get(0)));
		
		return orgao;
		
	}
	
	public Long getDescricaoAdicionar() {
		return DescricaoLogAcaoHelper.ADICIONAR_ORGAO;
	}

	public Long getDescricaoEditar() {
		return DescricaoLogAcaoHelper.EDITAR_ORGAO;
	}

	public Long getDescricaoExcluir() {
		return DescricaoLogAcaoHelper.EXCLUIR_ORGAO;
	}
	
	public void logOrgao(Orgao previous, Orgao current, LogAcao logAcao){
		
		if(previous == null || previous.getId() == null) {
			previous = new Orgao();
		}
		
		List<Log> logs = new ArrayList<Log>();
		
		Date datAlteracao = new Date();
		
		logUtil.withEntidade("Orgão");
		
		logs.add(logUtil.fromValues("endereco_Orgao", 
				previous.getEndereco() == null ? "-" : previous.getEndereco().getId().toString(), 
				current.getEndereco() == null ? "-" : current.getEndereco().getId().toString(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("nome_orgao", 
				previous.getNome() == null ? "-" : previous.getNome(), 
				current.getNome() == null ? "-" : current.getNome(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("sigla_orgao", 
				previous.getSigla() == null ? "-" : previous.getSigla(), 
				current.getSigla() == null ? "-" : current.getSigla(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("email_orgao", 
				previous.getEmail() == null ? "-" : previous.getEmail(), 
				current.getEmail() == null ? "-" : current.getEmail(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("folha_atual_orgao", 
				previous.getFolhaAtual() == null ? "-" : previous.getFolhaAtual().toString(), 
				current.getFolhaAtual() == null ? "-" : current.getFolhaAtual().toString(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("fechamento_orgao", 
				previous.getFechamento() == null ? "-" : previous.getFechamento().toString(), 
				current.getFechamento() == null ? "-" : current.getFechamento().toString(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("token_margem_orgao", 
				previous.getTokenMargem() == null ? "-" : previous.getTokenMargem(), 
				current.getTokenMargem() == null ? "-" : current.getTokenMargem().toString(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("token_contratacao_orgao", 
				previous.getTokenContratacao() == null ? "-" : previous.getTokenContratacao(), 
				current.getTokenContratacao() == null ? "-" : current.getTokenContratacao().toString(),
				datAlteracao
		));
		
		logs.add(logUtil.fromValues("status_orgao", 
				Boolean.toString(previous.isStatus()), 
				Boolean.toString(current.isStatus()),
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
