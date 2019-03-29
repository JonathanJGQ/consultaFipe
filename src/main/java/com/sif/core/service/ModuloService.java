package com.sif.core.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONObject;
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
import com.sif.model.Modulo;
import com.sif.model.list.ModuloDTO;
import com.sif.model.utils.DescricaoLogAcaoHelper;
import com.sif.repository.LogRepository;
import com.sif.repository.ModuloRepository;
import com.sif.repository.specification.ModuloSpecification;

@Service
public class ModuloService {

	@Autowired
	ModuloRepository moduloRepository;
	
	@Autowired
	SifLogUtil logUtil;
	
	@Autowired
	LogRepository logRepository;
	
	@Autowired
	Funcoes funcoes;
	
	public Page<Modulo> getAll(Pageable pageable, Modulo modulo) {
		ModuloSpecification spec = new ModuloSpecification();
		
		List<ModuloDTO> listaRetorno = new ArrayList<ModuloDTO>();
		Page<Modulo> lista = moduloRepository.findAll(Specification.where(
				spec.idEquals(modulo.getId() != null ? modulo.getId().toString() : null))
			.and(spec.nomeLike(modulo.getNome()))
			.and(spec.pathLike(modulo.getPath()))
			.and(spec.idMasterEqual(modulo.getIdMaster()))
			.and(spec.findByStatus()),pageable);
		
//		for(Modulo objeto : lista) {
//			ModuloDTO entity = new ModuloDTO();
//			entity.setId(objeto.getId());
//			entity.setNome(objeto.getNome());
//			entity.setPath(objeto.getPath());
//			entity.setIdMaster(objeto.getIdMaster());
//			
//			listaRetorno.add(entity);
//		}
//		
//		int start = Integer.parseInt(String.valueOf(pageable.getOffset()));
//		int end = (start + pageable.getPageSize()) > listaRetorno.size() ? listaRetorno.size() : (start + pageable.getPageSize());
		return lista;
	}
	
	public List<Modulo> betweenOrdens(Integer inicio, Integer fim) {
		ModuloSpecification spec = new ModuloSpecification();
		
		List<Modulo> lista = moduloRepository.findAll(Specification.where(
								spec.idOrdemMaiorQue(inicio)
							).and(spec.idOrdemMenorQue(fim)));
		
		return lista;
	}
	
	public ResponseEntity<Modulo> findById(Long id) {
		
		return Optional
				.ofNullable(moduloRepository.findById(id).get())
				.map(modulo -> ResponseEntity.ok().body(modulo))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public Modulo findByMasterId(Long id) {
		
		ModuloSpecification moduloSpec = new ModuloSpecification();
		
		List<Modulo> modulos = moduloRepository.findAll(Specification.where(
			moduloSpec.idMasterEqual(id)
		));
		
		if(modulos != null) {
			if(!modulos.isEmpty()) {
				return modulos.get(0);
			}
		}
		
		return null;
		
	}
	
	public ResponseEntity<Modulo> create(Modulo modulo) {
		
		validarModulo(modulo);
		
		Modulo moduloSave = moduloRepository.save(modulo);
		
		try {
			LogAcao logAcao = funcoes.logAcao(moduloSave.getId(), getDescricaoAdicionar(), funcoes.getLoggedUser());
			logModulo(null, modulo, logAcao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return Optional
				.ofNullable(moduloSave)
				.map(moduloAux -> ResponseEntity.ok().body(moduloAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<Modulo> update(Modulo modulo,Long id) {
		
		validarModulo(modulo);
		
		Modulo moduloSave = moduloRepository.findById(id).get();
		if(moduloSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		Long moduloId = moduloSave.getId();
		
		try {
			LogAcao logAcao = funcoes.logAcao(moduloSave.getId(), getDescricaoEditar(), funcoes.getLoggedUser());
			logModulo(moduloSave, modulo, logAcao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		BeanUtils.copyProperties(modulo, moduloSave);
		moduloSave.setId(moduloId);
		
		return Optional
				.ofNullable(moduloRepository.save(moduloSave))
				.map(moduloAux -> ResponseEntity.ok().body(moduloAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<Modulo> delete(Long id) {
		
		Modulo moduloSave = moduloRepository.findById(id).get();
		if(moduloSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		moduloSave.setStatus(0);
		
		Modulo modulo = new Modulo();
		BeanUtils.copyProperties(moduloSave, modulo);
		modulo.setStatus(moduloSave.getStatus() == 1 ? 0 : 1);
		
		try {
			LogAcao logAcao = funcoes.logAcao(moduloSave.getId(), getDescricaoExcluir(), funcoes.getLoggedUser());
			logModulo(modulo, moduloSave, logAcao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return Optional
				.ofNullable(moduloRepository.save(moduloSave))
				.map(moduloAux -> ResponseEntity.ok().body(moduloAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	private void validarModulo(Modulo modulo) throws GenericException {
		
		if(modulo.getNome() == null || modulo.getNome().isEmpty()) {
			throw new GenericException("Erro","Nome não pode ser nulo");
		}
	}	
	
	public ResponseEntity<String> getMenu() {
	
		return Optional
				.ofNullable(funcoes.buildMenu(funcoes.getLoggedUser()))
				.map(moduloAux -> ResponseEntity.ok().body(moduloAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<String> saveMenu(JSONArray json) {
		
		for(Object object : json) {
			JSONObject jsonModulo = (JSONObject) object;
			Modulo moduloPai = moduloRepository.findById(jsonModulo.getLong("id")).get();
			moduloPai.setOrdem(jsonModulo.getInt("ordem"));
			moduloRepository.save(moduloPai);
			
			JSONArray jsonFilho = jsonModulo.getJSONArray("submenu");
			
			for(Object objectFilho : jsonFilho) {
				JSONObject jsonModuloFilho = (JSONObject) objectFilho;
				Modulo moduloFilho = moduloRepository.findById(jsonModuloFilho.getLong("id")).get();
				moduloFilho.setOrdem(jsonModuloFilho.getInt("ordem"));
				moduloRepository.save(moduloFilho);
				
				JSONArray jsonNeto = jsonModulo.getJSONArray("submenu");
				
				for(Object objectNeto : jsonNeto) {
					JSONObject jsonModuloNeto = (JSONObject) objectNeto;
					Modulo moduloNeto = moduloRepository.findById(jsonModuloNeto.getLong("id")).get();
					moduloNeto.setOrdem(jsonModuloNeto.getInt("ordem"));
					moduloRepository.save(moduloNeto);
				}
			}
			
			
		}
		
		return Optional
				.ofNullable(funcoes.buildMenu(funcoes.getLoggedUser()))
				.map(moduloAux -> ResponseEntity.ok().body(moduloAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public Long getDescricaoAdicionar() {
		return DescricaoLogAcaoHelper.ADICIONAR_MODULO;
	}

	public Long getDescricaoEditar() {
		return DescricaoLogAcaoHelper.EDITAR_MODULO;
	}

	public Long getDescricaoExcluir() {
		return DescricaoLogAcaoHelper.EXCLUIR_MODULO;
	}
	
	public Long getDescricaoAlterarOrdenacaoModulo() {
		return DescricaoLogAcaoHelper.ALTERAR_ORDENACAO_DO_MODULO;
	}
	
	public void logModulo(Modulo previous, Modulo current, LogAcao logAcao) {
		
		if(previous == null || previous.getId() == null) {
			previous = new Modulo();
		}
		
		List<Log> logs = new ArrayList<Log>();
		
		Date datAlteracao = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:ss:mm");
		
		logUtil.withEntidade("Modulo");
		
		logs.add(logUtil.fromValues("nome_modulo", 
				previous.getNome() == null ? "-" : previous.getNome(),
				current.getNome() == null ? "-" : current.getNome(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("path_modulo", 
				previous.getPath() == null ? "-" : previous.getPath(),
				current.getPath() == null ? "-" : current.getPath(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("label_menu", 
				previous.getLabelMenu() == null ? "-" : previous.getLabelMenu(),
				current.getLabelMenu() == null ? "-" : current.getLabelMenu(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("id_master", 
				previous.getIdMaster() == null ? "-" : previous.getIdMaster().toString(),
				current.getIdMaster() == null ? "-" : current.getIdMaster().toString(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("ordem_modulo", 
				previous.getOrdem() == null ? "-" : previous.getOrdem().toString(),
				current.getOrdem() == null ? "-" : current.getOrdem().toString(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("icone_modulo", 
				previous.getIcone() == null ? "-" : previous.getIcone(),
				current.getIcone() == null ? "-" : current.getIcone(),
				datAlteracao));
		
		logs.add(logUtil.fromValues("status_modulo", 
				previous.getStatus() == 1 ? "ativo" : "inativo",
				current.getStatus() == 1  ? "ativo" : "inativo",
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
