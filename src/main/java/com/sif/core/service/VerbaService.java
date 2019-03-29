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
import com.sif.model.Verba;
import com.sif.model.custom.VerbaCustomDTO;
import com.sif.model.list.VerbaDTO;
import com.sif.model.utils.DescricaoLogAcaoHelper;
import com.sif.repository.ConsignatariaRepository;
import com.sif.repository.LogRepository;
import com.sif.repository.MargemRepository;
import com.sif.repository.VerbaRepository;
import com.sif.repository.specification.VerbaSpecification;

@Service
public class VerbaService {

	@Autowired
	VerbaRepository verbaRepository;
	
	@Autowired
	MargemRepository margemRepository;
	
	@Autowired
	ConsignatariaRepository consignatariaRepository;
	
	@Autowired
	Funcoes funcoes;
	
	@Autowired
	SifLogUtil logUtil;
	
	@Autowired
	LogRepository logRepository;
	
	public Page<Verba> getAll(Pageable pageable, Verba verba) {
		VerbaSpecification spec = new VerbaSpecification();
		
		List<VerbaDTO> listaRetorno = new ArrayList<VerbaDTO>();
		Page<Verba> lista = verbaRepository.findAll(Specification.where(
				spec.idEquals(verba.getId() != null ? verba.getId().toString() : null))
			.and(spec.descricaoLike(verba.getDescricao()))
			.and(spec.consignatariaEquals(verba.getConsignataria() != null ? verba.getConsignataria().getId().toString() : null))
			.and(spec.findByStatus()),pageable);
		
//		for(Verba objeto : lista) {
//			VerbaDTO entity = new VerbaDTO();
//			entity.setId(objeto.getId());
//			entity.setDescricao(objeto.getDescricao());
//			entity.setConsignataria(objeto.getConsignataria().getNome());
//			
//			listaRetorno.add(entity);
//		}
//		
//		int start = Integer.parseInt(String.valueOf(pageable.getOffset()));
//		int end = (start + pageable.getPageSize()) > listaRetorno.size() ? listaRetorno.size() : (start + pageable.getPageSize());
		return lista;
	}
	
	public ResponseEntity<VerbaCustomDTO> findById(Long id) {
		
		VerbaCustomDTO verbaDto = new VerbaCustomDTO();
		Verba verba = verbaRepository.findById(id).get();
		BeanUtils.copyProperties(verba, verbaDto);
		verbaDto.setMargem(verba.getMargem().getId());
		verbaDto.setConsignataria(verba.getConsignataria().getId());
		
		return Optional
				.ofNullable(verbaDto)
				.map(verbaAux -> ResponseEntity.ok().body(verbaAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public Verba findByCodigo(String codigo) {
		
		VerbaSpecification verbaSpec = new VerbaSpecification();
		
		List<Verba> verbas = verbaRepository.findAll(
			Specification.where(
				verbaSpec.codigoEqual(codigo)
			)
		);
		
		if(verbas != null && !verbas.isEmpty()) {
			return verbas.get(0);
		}
		
		return null;
	}
	
	public ResponseEntity<Verba> create(VerbaCustomDTO verbaDto) {
		
		Verba verba = dtoToVerba(verbaDto);
		
		validarVerba(verba);
		validarVerbaCreate(verba);
		
		Verba verbaSave = verbaRepository.save(verba);
		
		try {
			LogAcao logAcao = funcoes.logAcao(verbaSave.getId(), getDescricaoAdicionar(), funcoes.getLoggedUser());
			logVerba(null, verba, logAcao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return Optional
				.ofNullable(verbaRepository.save(verba))
				.map(verbaAux -> ResponseEntity.ok().body(verbaAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<Verba> update(VerbaCustomDTO verbaDto,Long id) {
		
		Verba verba = dtoToVerba(verbaDto);
		
		validarVerba(verba);
		
		
		Verba verbaSave = verbaRepository.findById(id).get();
		if(verbaSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		Long verbaId = verbaSave.getId();
		verba.setId(verbaId);
		validarVerbaUpdate(verba);
		
		try {
			LogAcao logAcao = funcoes.logAcao(verbaSave.getId(), getDescricaoEditar(), funcoes.getLoggedUser());
			logVerba(verbaSave, verba, logAcao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		BeanUtils.copyProperties(verba, verbaSave);
		verbaSave.setId(verbaId);
		verbaSave = verbaRepository.save(verbaSave);
		
		return Optional
				.ofNullable(verbaSave)
				.map(verbaAux -> ResponseEntity.ok().body(verbaAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<Verba> delete(Long id) {
		
		Verba verbaSave = verbaRepository.findById(id).get();
		if(verbaSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		verbaSave.setStatus(false);
		
		Verba verba = new Verba();
		BeanUtils.copyProperties(verbaSave, verba);
		verba.setStatus(!verbaSave.isStatus());
		
		try {
			LogAcao logAcao = funcoes.logAcao(verbaSave.getId(), getDescricaoExcluir(), funcoes.getLoggedUser());
			logVerba(verba, verbaSave, logAcao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return Optional
				.ofNullable(verbaRepository.save(verbaSave))
				.map(perfilAux -> ResponseEntity.ok().body(perfilAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	private void validarVerba(Verba verba) throws GenericException {
		
		if(verba.getMargem() == null) {
			throw new GenericException("Erro","Margem não pode ser nulo");
		}
		if(verba.getCodigo() == null || verba.getCodigo().isEmpty()) {
			throw new GenericException("Erro","Código não pode ser nulo");
		}
		if(verba.getEspecie() == null || verba.getEspecie().isEmpty()) {
			throw new GenericException("Erro","Especie não pode ser nulo");
		}
		if(verba.getDescricao() == null || verba.getDescricao().isEmpty()) {
			throw new GenericException("Erro","Descrição não pode ser nulo");
		}
		if(verba.getCiclo() == null || verba.getCiclo().isEmpty()) {
			throw new GenericException("Erro","Ciclo não pode ser nulo");
		}
		if(verba.getMaximoContratoVerba() == null) {
			throw new GenericException("Erro","Máximo contrato não pode ser nulo");
		}
	}
	
	private void validarVerbaCreate(Verba verba) throws GenericException {
		Verba verbaExisting = findByCodigo(verba.getCodigo());
		
		if(verbaExisting != null) {
			throw new GenericException("Erro","Já existe uma verba com este codigo");
		}
	}
	
	private void validarVerbaUpdate(Verba verba) throws GenericException {
		Verba verbaExisting = findByCodigo(verba.getCodigo());
		
		if(verbaExisting != null) {
			if(!verbaExisting.getId().equals(verba.getId())) {
				throw new GenericException("Erro","Já existe uma verba com este codigo");
			}
		}
	}
	
	private Verba dtoToVerba(VerbaCustomDTO verbaDto) {
		
		Verba verba = new Verba();
		BeanUtils.copyProperties(verbaDto, verba);
		verba.setMargem(margemRepository.findById(verbaDto.getMargem()).get());
		verba.setConsignataria(consignatariaRepository.findById(verbaDto.getConsignataria()).get());
		
		return verba;
		
	}
	
	public Long getDescricaoAdicionar() {
		return DescricaoLogAcaoHelper.ADICIONAR_VERBA;
	}

	public Long getDescricaoEditar() {
		return DescricaoLogAcaoHelper.EDITAR_VERBA;
	}

	public Long getDescricaoExcluir() {
		return DescricaoLogAcaoHelper.EXCLUIR_VERBA;
	}
	
	public void logVerba(Verba previous, Verba current, LogAcao logAcao) {
		
		if(previous == null || previous.getId() == null) {
			previous = new Verba();
		}
		
		List<Log> logs = new ArrayList<Log>();
		
		Date datAlteracao = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:ss:mm");
		
		logs.add(logUtil.fromValues("id_margem", 
				(previous.getMargem() == null || previous.getMargem().getId() == null) ? "-" : previous.getMargem().getId().toString(),
				(current.getMargem() == null || current.getMargem().getId() == null) ? "-" : current.getMargem().getId().toString(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("id_consignataria", 
				(previous.getConsignataria() == null || previous.getConsignataria().getId() == null) ? "-" : previous.getConsignataria().getId().toString(),
				(current.getConsignataria() == null || current.getConsignataria().getId() == null) ? "-" : current.getConsignataria().getId().toString(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("codigo_verba", 
				previous.getCodigo() == null ? "-" : previous.getCodigo(),
				current.getCodigo() == null ? "-" : current.getCodigo(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("especie_verba", 
				previous.getEspecie() == null ? "-" : previous.getEspecie(),
				current.getEspecie() == null ? "-" : current.getEspecie(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("descricao_verba", 
				previous.getDescricao() == null ? "-" : previous.getDescricao(),
				current.getDescricao() == null ? "-" : current.getDescricao(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("ciclo_verba", 
				previous.getCiclo() == null ? "-" : previous.getCiclo(),
				current.getCiclo() == null ? "-" : current.getCiclo(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("max_contrato_verba", 
				previous.getMaximoContratoVerba() == null ? "-" : previous.getMaximoContratoVerba().toString(),
				current.getMaximoContratoVerba() == null ? "-" : current.getMaximoContratoVerba().toString(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("status_verba", 
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

	public Verba save(Verba verba) {
		return verbaRepository.save(verba);
		
	}
	
}
