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
import com.sif.model.Cidade;
import com.sif.model.Log;
import com.sif.model.LogAcao;
import com.sif.model.custom.CidadeCustomDTO;
import com.sif.model.utils.DescricaoLogAcaoHelper;
import com.sif.repository.CidadeRepository;
import com.sif.repository.LogRepository;
import com.sif.repository.UfRepository;
import com.sif.repository.specification.CidadeSpecification;

@Service
public class CidadeService {

	@Autowired
	CidadeRepository cidadeRepository;
	
	@Autowired
	UfRepository ufRepository;
	
	@Autowired
	Funcoes funcoes;
	
	@Autowired
	SifLogUtil logUtil;
	
	@Autowired
	LogRepository logRepository;
	
	public Page<Cidade> getAll(Pageable pageable, Cidade cidade) {
		CidadeSpecification spec = new CidadeSpecification();
		
//		List<CidadeDTO> listaRetorno = new ArrayList<CidadeDTO>();
		Page<Cidade> lista = cidadeRepository.findAll(Specification.where(
				spec.idEquals(cidade.getId() != null ? cidade.getId().toString() : null))
			.and(spec.nomeLike(cidade.getNome()))
			.and(spec.ufEquals(cidade.getUf() != null ? cidade.getUf().getId().toString() : null))
			.and(spec.findByStatusOrderByNome()),pageable);
		
//		for(Cidade objeto : lista) {
//			CidadeDTO entity = new CidadeDTO();
//			entity.setId(objeto.getId());
//			entity.setNome(objeto.getNome());
//			entity.setUf(objeto.getUf().getSigla());
//			
//			listaRetorno.add(entity);
//		}
//		
//		int start = Integer.parseInt(String.valueOf(pageable.getOffset()));
//		int end = (start + pageable.getPageSize()) > listaRetorno.size() ? listaRetorno.size() : (start + pageable.getPageSize());
		return lista;
	}
	
	public ResponseEntity<CidadeCustomDTO> findById(Long id) {
		
		Cidade cidade = cidadeRepository.findById(id).get();
		CidadeCustomDTO cidadeDto = new CidadeCustomDTO();
		
		cidadeDto.setId(cidade.getId());
		cidadeDto.setNome(cidade.getNome());
		cidadeDto.setStatus(cidade.isStatus());
		cidadeDto.setUf(cidade.getUf().getId());
		
		return Optional
				.ofNullable(cidadeDto)
				.map(cidadeAux -> ResponseEntity.ok().body(cidadeAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public List<Cidade> findByNome(String nome) {
		CidadeSpecification cidadeSpec = new CidadeSpecification();
		
		List<Cidade> cidades = cidadeRepository.findAll(Specification.where(
				cidadeSpec.nomeEqual(nome)
			));
		
		return cidades;
	}
	
	public ResponseEntity<Cidade> findCidadeById(Long id) {
		
		Cidade cidade = cidadeRepository.findById(id).get();
		
		return Optional
				.ofNullable(cidade)
				.map(cidadeAux -> ResponseEntity.ok().body(cidadeAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<Cidade> create(CidadeCustomDTO cidadeDto) {
		
		Cidade cidade = dtoToCidade(cidadeDto);
		
		validarCidade(cidade);
		
		Cidade cidadeSave = cidadeRepository.save(cidade);
		
		try {
			LogAcao logAcao = funcoes.logAcao(cidadeSave.getId(), getDescricaoAdicionar(), funcoes.getLoggedUser());
			logCidade(null, cidade, logAcao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return Optional
				.ofNullable(cidadeRepository.save(cidade))
				.map(cidadeAux -> ResponseEntity.ok().body(cidadeAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<Cidade> update(CidadeCustomDTO cidadeDto,Long id) {
		
		Cidade cidade = dtoToCidade(cidadeDto);
		
		validarCidade(cidade);
		
		Cidade cidadeSave = cidadeRepository.findById(id).get();
		if(cidadeSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		Long cidadeId = cidadeSave.getId();
		
		cidade.setId(cidadeId);
		
		try {
			LogAcao logAcao = funcoes.logAcao(cidadeSave.getId(), getDescricaoEditar(), funcoes.getLoggedUser());
			logCidade(cidadeSave, cidade, logAcao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		BeanUtils.copyProperties(cidade, cidadeSave);
		cidadeSave.setId(cidadeId);
		
		return Optional
				.ofNullable(cidadeRepository.save(cidadeSave))
				.map(cidadeAux -> ResponseEntity.ok().body(cidadeAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<Cidade> delete(Long id) {
		
		Cidade cidadeSave = cidadeRepository.findById(id).get();
		if(cidadeSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		cidadeSave.setStatus(false);
		
		Cidade cidade = new Cidade();
		BeanUtils.copyProperties(cidadeSave, cidade);
		cidade.setStatus(!cidadeSave.isStatus());
		
		try {
			LogAcao logAcao = funcoes.logAcao(cidadeSave.getId(), getDescricaoExcluir(), funcoes.getLoggedUser());
			logCidade(cidade, cidadeSave, logAcao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return Optional
				.ofNullable(cidadeRepository.save(cidadeSave))
				.map(cidadeAux -> ResponseEntity.ok().body(cidadeAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	private void validarCidade(Cidade cidade) throws GenericException {
		
		if(cidade.getUf() == null) {
			throw new GenericException("Erro","UF não pode ser nulo");
		}
		if(cidade.getNome()== null || cidade.getNome().isEmpty()) {
			throw new GenericException("Erro","Nome não pode ser nulo");
		}
		
	}
	
	private Cidade dtoToCidade(CidadeCustomDTO cidadeDto) {
		
		Cidade cidade = new Cidade();
		
		if(cidadeDto.getId() != null) {
			cidade.setId(cidadeDto.getId());
		}
		
		cidade.setNome(cidadeDto.getNome());
		cidade.setStatus(cidadeDto.isStatus());
		
		if(cidadeDto.getUf() != null) {
			cidade.setUf(ufRepository.findById(cidadeDto.getUf()).get());
		}
		
		return cidade;
		
	}
	
	public Long getDescricaoAdicionar() {
		return DescricaoLogAcaoHelper.ADICIONAR_CIDADE;
	}

	public Long getDescricaoEditar() {
		return DescricaoLogAcaoHelper.EDITAR_CIDADE;
	}

	public Long getDescricaoExcluir() {
		return DescricaoLogAcaoHelper.EXCLUIR_CIDADE;
	}
	
	public void logCidade(Cidade previous, Cidade current, LogAcao logAcao) {
		
		if(previous == null || previous.getId() == null) {
			previous = new Cidade();
		}
		
		List<Log> logs = new ArrayList<Log>();
		
		Date datAlteracao = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:ss:mm");
		
		logUtil.withEntidade("Cidade");
		
		logs.add(logUtil.fromValues("id_uf", 
				(previous.getUf() == null || previous.getUf().getId() == null) ? "-" : previous.getUf().getId().toString(),
				(current.getUf() == null || current.getUf().getId() == null) ? "-" : current.getUf().getId().toString(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("nome_cidade", 
				previous.getNome() == null ? "-" : previous.getNome(),
				current.getNome() == null ? "-" : current.getNome(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("status_cidade", 
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
