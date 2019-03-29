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
import com.sif.model.Funcionalidade;
import com.sif.model.Log;
import com.sif.model.LogAcao;
import com.sif.model.Modulo;
import com.sif.model.Perfil;
import com.sif.model.PerfilFuncionalidade;
import com.sif.model.custom.FuncionalidadeCustomDTO;
import com.sif.model.utils.DescricaoLogAcaoHelper;
import com.sif.repository.FuncionalidadeRepository;
import com.sif.repository.LogRepository;
import com.sif.repository.ModuloRepository;
import com.sif.repository.specification.FuncionalidadeSpecification;

@Service
public class FuncionalidadeService {

	@Autowired
	FuncionalidadeRepository funcionalidadeRepository;
	
	@Autowired
	Funcoes funcoes;
	
	@Autowired
	ModuloRepository moduloRepository;
	
	@Autowired
	SifLogUtil logUtil;
	
	@Autowired
	LogRepository logRepository;
	
	@Autowired
	PerfilFuncionalidadeService perfilFuncionalidadeService;
	
	@Autowired
	PerfilService perfilService;
	
	public Page<Funcionalidade> getAll(Pageable pageable, Funcionalidade funcionalidade) {
		FuncionalidadeSpecification spec = new FuncionalidadeSpecification();
		
//		List<FuncionalidadeDTO> listaRetorno = new ArrayList<FuncionalidadeDTO>();
		Page<Funcionalidade> lista = funcionalidadeRepository.findAll(Specification.where(
				spec.idEquals(funcionalidade.getId() != null ? funcionalidade.getId().toString() : null))
			.and(spec.nomeLike(funcionalidade.getNome()))
			.and(spec.rotaLike(funcionalidade.getRota()))
			.and(spec.findByStatus()),pageable);
		
//		for(Funcionalidade objeto : lista) {
//			FuncionalidadeDTO entity = new FuncionalidadeDTO();
//			entity.setId(objeto.getId());
//			entity.setNome(objeto.getNome());
//			entity.setRota(objeto.getRota());
//			
//			listaRetorno.add(entity);
//		}
//		
//		int start = Integer.parseInt(String.valueOf(pageable.getOffset()));
//		int end = (start + pageable.getPageSize()) > listaRetorno.size() ? listaRetorno.size() : (start + pageable.getPageSize());
		return lista;
	}
	
	
	public ResponseEntity<FuncionalidadeCustomDTO> findById(Long id) {
		
		Funcionalidade funcionalidade = funcionalidadeRepository.findById(id).get();
		FuncionalidadeCustomDTO funcionalidadeDto = new FuncionalidadeCustomDTO();
		
		BeanUtils.copyProperties(funcionalidade, funcionalidadeDto);
		
		funcionalidadeDto.setPerfis(getPerfisId(funcionalidade));
		
		funcionalidadeDto.setModulo(funcionalidade.getModulo().getId());
		
		return Optional
				.ofNullable(funcionalidadeDto)
				.map(funcionalidadeAux -> ResponseEntity.ok().body(funcionalidadeAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	private List<Long> getPerfisId(Funcionalidade funcionalidade){
		
		List<Long> ids = new ArrayList<Long>();
		List<PerfilFuncionalidade> perfisFuncionalidades = perfilFuncionalidadeService.findByFuncionalidade(funcionalidade.getId());
		
		for(PerfilFuncionalidade pf : perfisFuncionalidades) {
			ids.add(pf.getPerfil().getId());
		}
		
		return ids;
	}
	
	public ResponseEntity<Funcionalidade> create(FuncionalidadeCustomDTO funcionalidadeDto) {
		
		Funcionalidade funcionalidade = dtoToFuncionalidade(funcionalidadeDto);
		
		validarFuncionalidade(funcionalidade);
		if(funcionalidadeDto.getPerfis() == null || funcionalidadeDto.getPerfis().isEmpty()) {
			throw new GenericException("Aviso", "A funcionalidade deve ter relação com  pelo menos um perfil.");
		}
		
		Funcionalidade funcionalidadeSave = funcionalidadeRepository.save(funcionalidade);
		
		Calendar calendar = Calendar.getInstance();
		Date date = calendar.getTime();
		
		List<PerfilFuncionalidade> perfilFuncsByFunc = perfilFuncionalidadeService.findByFuncionalidade(funcionalidadeSave.getId());
		
		List<Long> idsPerfilExistentes = new ArrayList<Long>();
		
		for(PerfilFuncionalidade perfilFuncionalidade: perfilFuncsByFunc) {
			idsPerfilExistentes.add(perfilFuncionalidade.getPerfil().getId());
		}
		
		List<Long> perfilIncluir = new ArrayList<Long>();
		List<Long> perfilExcluir = new ArrayList<Long>();
		
		//Incluir
		for(Long perfilId : funcionalidadeDto.getPerfis()) {
			if(!idsPerfilExistentes.contains(perfilId)) {
				perfilIncluir.add(perfilId);
			}
		}
		
		//Excluir
		for(Long perfilId : idsPerfilExistentes) {
			if(!funcionalidadeDto.getPerfis().contains(perfilId)) {
				perfilExcluir.add(perfilId);
			}
		}
		
		//ExcluindoPerfis
		for(Long perfilId : perfilExcluir) {
			List<PerfilFuncionalidade> perfisFuncsByFunc 
				= perfilFuncionalidadeService.findByFuncionalidadeEPerfil(funcionalidadeSave.getId(), perfilId);
		
			for(PerfilFuncionalidade pf : perfisFuncsByFunc) {
				perfilFuncionalidadeService.forceDelete(pf.getId());
			}
		}
		
		//IncluindoPerfis
		for(Long perfilId : perfilIncluir) {
			Perfil perfil = perfilService.findById(perfilId).getBody();
			
			if(perfil != null){
				
				PerfilFuncionalidade perfilFuncionalidade = new PerfilFuncionalidade();
				perfilFuncionalidade.setFuncionalidade(funcionalidadeSave);
				perfilFuncionalidade.setPerfil(perfil);
				perfilFuncionalidade.setData(date);
				perfilFuncionalidade.setStatus(true);
				
				perfilFuncionalidadeService.perfilFuncionalidadeRepository.save(perfilFuncionalidade);
			}
		}
		
		try {
			LogAcao logAcao = funcoes.logAcao(funcionalidadeSave.getId(), getDescricaoAdicionar(), funcoes.getLoggedUser());
			logFuncionalidade(null, funcionalidade, logAcao, null, getNomePerfis(funcionalidade));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		
		return Optional
				.ofNullable(funcionalidadeSave)
				.map(funcionalidadeAux -> ResponseEntity.ok().body(funcionalidadeAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<Funcionalidade> update(FuncionalidadeCustomDTO funcionalidadeDto,Long id) {
		
		Funcionalidade funcionalidade = dtoToFuncionalidade(funcionalidadeDto);
		
		validarFuncionalidade(funcionalidade);
		
		if(funcionalidadeDto.getPerfis() == null || funcionalidadeDto.getPerfis().isEmpty()) {
			throw new GenericException("Aviso", "A funcionalidade deve ter relação com  pelo menos um perfil.");
		}
		
		Funcionalidade funcionalidadeSave = funcionalidadeRepository.findById(id).get();
		if(funcionalidadeSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		Calendar calendar = Calendar.getInstance();
		Date date = calendar.getTime();
		
		List<PerfilFuncionalidade> perfilFuncsByFunc = perfilFuncionalidadeService.findByFuncionalidade(funcionalidadeSave.getId());
		
		List<Long> idsPerfilExistentes = new ArrayList<Long>();
		
		for(PerfilFuncionalidade perfilFuncionalidade: perfilFuncsByFunc) {
			idsPerfilExistentes.add(perfilFuncionalidade.getPerfil().getId());
		}
		
		List<Long> perfilIncluir = new ArrayList<Long>();
		List<Long> perfilExcluir = new ArrayList<Long>();
		
		//Incluir
		for(Long perfilId : funcionalidadeDto.getPerfis()) {
			if(!idsPerfilExistentes.contains(perfilId)) {
				perfilIncluir.add(perfilId);
			}
		}
		
		//Excluir
		for(Long perfilId : idsPerfilExistentes) {
			if(!funcionalidadeDto.getPerfis().contains(perfilId)) {
				perfilExcluir.add(perfilId);
			}
		}
		
		//ExcluindoPerfis
		for(Long perfilId : perfilExcluir) {
			List<PerfilFuncionalidade> perfisFuncsByFunc 
				= perfilFuncionalidadeService.findByFuncionalidadeEPerfil(funcionalidadeSave.getId(), perfilId);
		
			for(PerfilFuncionalidade pf : perfisFuncsByFunc) {
				perfilFuncionalidadeService.forceDelete(pf.getId());
			}
		}
		
		//IncluindoPerfis
		for(Long perfilId : perfilIncluir) {
			Perfil perfil = perfilService.findById(perfilId).getBody();
			
			if(perfil != null){
				
				PerfilFuncionalidade perfilFuncionalidade = new PerfilFuncionalidade();
				perfilFuncionalidade.setFuncionalidade(funcionalidadeSave);
				perfilFuncionalidade.setPerfil(perfil);
				perfilFuncionalidade.setData(date);
				perfilFuncionalidade.setStatus(true);
				
				perfilFuncionalidadeService.perfilFuncionalidadeRepository.save(perfilFuncionalidade);
			}
		}
		
		Long funcionalidadeId = funcionalidadeSave.getId();
		
		try {
			LogAcao logAcao = funcoes.logAcao(funcionalidadeSave.getId(), getDescricaoEditar(), funcoes.getLoggedUser());
			logFuncionalidade(funcionalidadeSave, funcionalidade, logAcao, getNomePerfisByIds(idsPerfilExistentes), getNomePerfisByIds(funcionalidadeDto.getPerfis()));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		BeanUtils.copyProperties(funcionalidade, funcionalidadeSave);
		funcionalidadeSave.setId(funcionalidadeId);
		
		
		return Optional
				.ofNullable(funcionalidadeRepository.save(funcionalidadeSave))
				.map(funcionalidadeAux -> ResponseEntity.ok().body(funcionalidadeAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<Funcionalidade> delete(Long id) {
		
		Funcionalidade funcionalidadeSave = funcionalidadeRepository.findById(id).get();
		if(funcionalidadeSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		funcionalidadeSave.setStatus(false);
		
		Funcionalidade funcionalidade = new Funcionalidade();
		BeanUtils.copyProperties(funcionalidadeSave, funcionalidade);
		funcionalidade.setStatus(!funcionalidadeSave.isStatus());
		
		try {
			LogAcao logAcao = funcoes.logAcao(funcionalidadeSave.getId(), getDescricaoExcluir(), funcoes.getLoggedUser());
			logFuncionalidade(funcionalidade, funcionalidadeSave, logAcao, getNomePerfis(funcionalidade), getNomePerfis(funcionalidade));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return Optional
				.ofNullable(funcionalidadeRepository.save(funcionalidadeSave))
				.map(funcionalidadeAux -> ResponseEntity.ok().body(funcionalidadeAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public List<Funcionalidade> funcionalidadeByModulo(Modulo modulo){
		
		if(modulo == null) {
			return null;
		}
		
		FuncionalidadeSpecification funcSpec = new FuncionalidadeSpecification();
		
		return funcionalidadeRepository.findAll(Specification.where(
				funcSpec.idModuloEquals(modulo.getId())
		));
		
	}
	
	private void validarFuncionalidade(Funcionalidade funcionalidade) throws GenericException {
		
		if(funcionalidade.getModulo() == null) {
			throw new GenericException("Erro","Modulo não pode ser nulo");
		}
	}	
	
	private Funcionalidade dtoToFuncionalidade(FuncionalidadeCustomDTO funcionalidadeDto) {
		
		Funcionalidade funcionalidade = new Funcionalidade();
		BeanUtils.copyProperties(funcionalidadeDto, funcionalidade);
		funcionalidade.setModulo(moduloRepository.findById(funcionalidadeDto.getModulo()).get());
		
		return funcionalidade;
		
	}
	
	private String getNomePerfis(List<PerfilFuncionalidade> perfisFuncionalidades) {
		String perfis = "";
		
		for(PerfilFuncionalidade pf : perfisFuncionalidades) {
			perfis = perfis + pf.getPerfil().getNome() + ", ";
		}
		
		if(perfis.length() >= 2) {
			perfis = perfis.substring(0, perfis.length() - 2);
		}
		
		
		return perfis;
	}
	
	private String getNomePerfisByIds(List<Long> idPerfis) {
		String perfis = "";
		
		
		for(Long id : idPerfis) {
			Perfil perfil = perfilService.findById(id).getBody();
			if(perfil != null) {
				perfis = perfis + perfil.getNome() + ", ";
			}
			
		}
		if(perfis.length() > 2) {
			perfis = perfis.substring(0, perfis.length() - 2);
		}
		
		
		return perfis;
	}
	
	private String getNomePerfis(Funcionalidade funcionalidade) {
		String perfis = "";
		
		List<PerfilFuncionalidade> perfisFuncionalidades = perfilFuncionalidadeService.findByFuncionalidade(funcionalidade.getId());
		
		for(PerfilFuncionalidade pf : perfisFuncionalidades) {
			perfis = perfis + pf.getPerfil().getNome() + ", ";
		}
		if(perfis.length() >= 2) {
			perfis = perfis.substring(0, perfis.length() - 2);
		}
		
		return perfis;
	}
	
	public Long getDescricaoAdicionar() {
		return DescricaoLogAcaoHelper.ADICIONAR_FUNCIONALIDADE;
	}

	public Long getDescricaoEditar() {
		return DescricaoLogAcaoHelper.EDITAR_FUNCIONALIDADE;
	}

	public Long getDescricaoExcluir() {
		return DescricaoLogAcaoHelper.EXCLUIR_FUNCIONALIDADE;
	}
	
	public void logFuncionalidade(Funcionalidade previous, Funcionalidade current, LogAcao logAcao, String perfisPrevious, String perfisCurrent) {
		
		if(previous == null || previous.getId() == null) {
			previous = new Funcionalidade();
		}
		
		List<Log> logs = new ArrayList<Log>();
		
		Date datAlteracao = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:ss:mm");
		
		logUtil.withEntidade("Funcionalidade");
		
		logs.add(logUtil.fromValues("nome_funcionalidade", 
				previous.getNome() == null ? "-" : previous.getNome(),
				current.getNome() == null ? "-" : current.getNome(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("rota_funcionalidade", 
				previous.getRota() == null ? "-" : previous.getRota(),
				current.getRota() == null ? "-" : current.getRota(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("modulo_funcionalidade", 
				(previous.getModulo() == null || previous.getModulo().getId() == null) ? "-" : previous.getModulo().getId().toString(),
				(current.getModulo() == null || current.getModulo().getId() == null)? "-" : current.getModulo().getId().toString(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("status_funcionalidade", 
				previous.isSelecionado() ? "ativo" : "inativo",
				current.isSelecionado() ? "ativo" : "inativo",
				datAlteracao));
		
		logs.add(logUtil.fromValues("perfis_funcionalidades", 
				perfisPrevious,
				perfisCurrent,
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
