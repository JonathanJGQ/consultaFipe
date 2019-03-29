package com.sif.core.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
import com.sif.model.Margem;
import com.sif.model.custom.MargemCustomDTO;
import com.sif.model.list.MargemDTO;
import com.sif.model.utils.DescricaoLogAcaoHelper;
import com.sif.repository.LogRepository;
import com.sif.repository.MargemRepository;
import com.sif.repository.OrgaoRepository;
import com.sif.repository.specification.MargemSpecification;

@Service
public class MargemService {

	@Autowired
	MargemRepository margemRepository;
	
	@Autowired
	OrgaoRepository orgaoRepository;
	
	@Autowired
	Funcoes funcoes;
	
	@Autowired
	SifLogUtil logUtil;
	
	@Autowired
	LogRepository logRepository;
	
	public Page<Margem> getAll(Pageable pageable, Margem margem) {
		MargemSpecification spec = new MargemSpecification();
		
		List<MargemDTO> listaRetorno = new ArrayList<MargemDTO>();
		Page<Margem> lista = margemRepository.findAll(Specification.where(
				spec.idEquals(margem.getId() != null ? margem.getId().toString() : null))
			.and(spec.descricaoLike(margem.getDescricao()))
			.and(spec.orgaoEquals(margem.getOrgao() != null ? margem.getOrgao().getId() : null))
			.and(spec.findByStatus()),pageable);
		
//		for(Margem objeto : lista) {
//			MargemDTO entity = new MargemDTO();
//			entity.setId(objeto.getId());
//			entity.setDescricao(objeto.getDescricao());
//			entity.setOrgao(objeto.getOrgao().getNome());;
//			
//			listaRetorno.add(entity);
//		}
//		
//		int start = Integer.parseInt(String.valueOf(pageable.getOffset()));
//		int end = (start + pageable.getPageSize()) > listaRetorno.size() ? listaRetorno.size() : (start + pageable.getPageSize());
		return lista;
	}
	
	public ResponseEntity<MargemCustomDTO> findById(Long id) {
		
		MargemCustomDTO margemDto = new MargemCustomDTO();
		Margem margem = margemRepository.findById(id).get();
		BeanUtils.copyProperties(margem, margemDto);
		margemDto.setDataCadastro(margem.getDataCadastro());
		margemDto.setDescricao(margem.getDescricao());
		margemDto.setId(margem.getId());
		margemDto.setMaxParcelas(margem.getMaxParcelas());
		margemDto.setOrgao(margem.getOrgao().getId());
		margemDto.setPercentual(margem.getPercentual());
		margemDto.setStatus(margem.isStatus());
		
		
		return Optional
				.ofNullable(margemDto)
				.map(margemAux -> ResponseEntity.ok().body(margemAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<Margem> create(MargemCustomDTO margemDto) {
		
		Margem margem = dtoToMargem(margemDto);
		
		Calendar calendar = Calendar.getInstance();
		Date today = calendar.getTime();
		margem.setDataCadastro(today);
		
		validarMargemCreate(margem);
		
		Margem margemSave = margemRepository.save(margem);
		
		try {
			LogAcao logAcao = funcoes.logAcao(margemSave.getId(), getDescricaoAdicionar(), funcoes.getLoggedUser());
			logMargem(null, margem, logAcao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return Optional
				.ofNullable(margemSave)
				.map(margemAux -> ResponseEntity.ok().body(margemAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<Margem> update(MargemCustomDTO margemDto,Long id) {
		
		Margem margem = dtoToMargem(margemDto);
		
		validarMargem(margem);
		
		Margem margemSave = margemRepository.findById(id).get();
		if(margemSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		Long margemId = margemSave.getId();
		margem.setId(margemId);
		
		try {
			LogAcao logAcao = funcoes.logAcao(margemSave.getId(), getDescricaoEditar(), funcoes.getLoggedUser());
			logMargem(margemSave, margem, logAcao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		BeanUtils.copyProperties(margem, margemSave);
		margemSave.setId(margemId);
		
		return Optional
				.ofNullable(margemRepository.save(margemSave))
				.map(margemAux -> ResponseEntity.ok().body(margemAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<Margem> delete(Long id) {
		
		Margem margemSave = margemRepository.findById(id).get();
		if(margemSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		margemSave.setStatus(false);
		
		Margem margem = new Margem();
		BeanUtils.copyProperties(margemSave, margem);
		margem.setStatus(!margemSave.isStatus());
		
		try {
			LogAcao logAcao = funcoes.logAcao(margemSave.getId(), getDescricaoExcluir(), funcoes.getLoggedUser());
			logMargem(margem, margemSave, logAcao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return Optional
				.ofNullable(margemRepository.save(margemSave))
				.map(margemAux -> ResponseEntity.ok().body(margemAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	private void validarMargem(Margem margem) throws GenericException {
		
		if(margem.getOrgao() == null) {
			throw new GenericException("Erro","Orgão não pode ser nulo");
		}
	}
	
	private void validarMargemCreate(Margem margem) throws GenericException {
		
		
		validarMargem(margem);
		
		if(margem.getId() != null) {
			throw new GenericException("Erro","O id não deve ser preenchido na criação de um cadastro.");
		}
	}
	
	private Margem dtoToMargem(MargemCustomDTO margemDto) {
		
		Margem margem = new Margem();
		BeanUtils.copyProperties(margemDto, margem);
		margem.setDataCadastro(margemDto.getDataCadastro());
		margem.setDescricao(margemDto.getDescricao());
		margem.setId(margemDto.getId());
		margem.setMaxParcelas(margemDto.getMaxParcelas());
		margem.setOrgao(orgaoRepository.findById(margemDto.getOrgao()).get());
		margem.setPercentual(margemDto.getPercentual());
		margem.setStatus(margemDto.isStatus());
			
		return margem;
		
	}
	
	public Long getDescricaoAdicionar() {
		return DescricaoLogAcaoHelper.ADICIONAR_MARGEM;
	}

	public Long getDescricaoEditar() {
		return DescricaoLogAcaoHelper.EDITAR_MARGEM;
	}

	public Long getDescricaoExcluir() {
		return DescricaoLogAcaoHelper.EXCLUIR_MARGEM;
	}
	
	public void logMargem(Margem previous, Margem current, LogAcao logAcao) {
		
		if(previous == null || previous.getId() == null) {
			previous = new Margem();
		}
		
		List<Log> logs = new ArrayList<Log>();
		
		Date datAlteracao = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:ss:mm");
		
		logUtil.withEntidade("Margem");
		
		logs.add(logUtil.fromValues("id_orgao", 
				(previous.getOrgao() == null || previous.getOrgao().getId() == null) ? "-" : previous.getOrgao().getId().toString(),
				(current.getOrgao() == null || current.getOrgao().getId() == null) ? "-" : current.getOrgao().getId().toString(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("descricao_margem", 
				previous.getDescricao() == null ? "-" : previous.getDescricao(),
				current.getDescricao() == null ? "-" : current.getDescricao(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("percentual_margem", 
				previous.getPercentual() == null ? "-" : previous.getPercentual(),
				current.getPercentual() == null ? "-" : current.getPercentual(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("max_parcelas_margem", 
				previous.getMaxParcelas() == null ? "-" : previous.getMaxParcelas(),
				current.getMaxParcelas() == null ? "-" : current.getMaxParcelas(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("data_cadastro_margem", 
				previous.getDataCadastro() == null ? "-" : sdf.format(previous.getDataCadastro()),
				current.getDataCadastro() == null ? "-" : sdf.format(current.getDataCadastro()),
				datAlteracao));
		
		logs.add(logUtil.fromValues("status_margem", 
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

	public Margem findByNome(String margem) {
		return margemRepository.findByDescricao(margem);
	}
	
	public List<Margem> findByOrgao(Long orgaoID) {
		
		MargemSpecification margemSpec = new MargemSpecification();
		
		return margemRepository.findAll(
				Specification.where(margemSpec.orgaoEquals(orgaoID))
			);
	}
}
