package com.sif.core.service;

import java.util.Calendar;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.sif.model.Log;
import com.sif.repository.LogRepository;
import com.sif.repository.specification.LogSpecification;

@Service
public class LogService {
	
	@Autowired
	LogRepository logRepository;
	
	public Page<Log> getAll(Pageable pageable, Log log) {
		
//		List<LogCustomDTO> listaRetorno = new ArrayList<LogCustomDTO>();
		
		LogSpecification spec = new LogSpecification();
		
//		if(log.getDataLog() != null) {
//			Calendar calGe = Calendar.getInstance();
//			calGe.setTime(log.getDataLog());
//			calGe.set(Calendar.HOUR, 0);
//			calGe.set(Calendar.MINUTE, 0);
//			calGe.set(Calendar.SECOND, 0);
//			calGe.set(Calendar.MILLISECOND, 0);
//			
//			log.setDataLogGe(calGe.getTime());
//			
//			Calendar calLe = Calendar.getInstance();
//			calLe.setTime(log.getDataLog());
//			calLe.set(Calendar.HOUR, 23);
//			calLe.set(Calendar.MINUTE, 59);
//			calLe.set(Calendar.SECOND, 59);
//			calLe.set(Calendar.MILLISECOND, 999);
//			
//			log.setDataLogLe(calLe.getTime());
//			
//		}
		
		if(log.getDataLogLe() != null) {
			Calendar calLe = Calendar.getInstance();
			calLe.setTime(log.getDataLogLe());
			calLe.add(Calendar.DAY_OF_MONTH, 1);
			log.setDataLogLe(calLe.getTime());
		}
		
		Page<Log> lista = logRepository.findAll(Specification.where(
				spec.nomeLike(log.getUsuarioNome()))
			.and(spec.ipLike(log.getIp()))
			.and(spec.entidadeLike(log.getEntidade()))
			.and(spec.idRowEquals(log.getIdRow()))
			.and(spec.campoMudadoLike(log.getCampoMudado()))
			.and(spec.valorAntigoLike(log.getValorAntigo()))
			.and(spec.valorNovoLike(log.getValorNovo()))
			.and(spec.dataLogGe(log.getDataLogGe()))
			.and(spec.dataLogLe(log.getDataLogLe()))
			.and(spec.orderById()),pageable);
		
//		for(Log objeto : lista) {
//			LogCustomDTO entity = new LogCustomDTO();
//			entity.setId(objeto.getId());
//			entity.setCampoMudado(objeto.getCampoMudado());
//			entity.setDataLog(objeto.getDataLog());
//			entity.setEntidade(objeto.getEntidade());
//			entity.setIdRow(objeto.getIdRow());
//			entity.setIp(objeto.getIp());
//			entity.setLogAcao(objeto.getLogAcao() == null ? null :objeto.getLogAcao().getId());
//			entity.setTipoLog(objeto.getTipoLog());
//			entity.setValorAntigo(objeto.getValorAntigo());
//			entity.setValorNovo(objeto.getValorNovo());
//			entity.setUsuario(objeto.getUsuario().getNome());
//			
//			listaRetorno.add(entity);
//		}
//		
//		int start = Integer.parseInt(String.valueOf(pageable.getOffset()));
//		int end = (start + pageable.getPageSize()) > listaRetorno.size() ? listaRetorno.size() : (start + pageable.getPageSize());
		return lista;
	}
	
	public List<Log> findByLogAcao(Long id){
		
		LogSpecification logSpec = new LogSpecification();
		
		List<Log> logs = logRepository.findAll(Specification.where(
				logSpec.findByLogAcao(id)
		));
		
		return logs;
	}
	
}
