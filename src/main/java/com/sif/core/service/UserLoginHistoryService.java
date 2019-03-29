package com.sif.core.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.sif.model.UserLoginHistory;
import com.sif.model.Usuario;
import com.sif.model.custom.UserLoginHistoryDTO;
import com.sif.repository.UserLoginHistoryRepository;
import com.sif.repository.specification.UserLoginHistorySpecification;

@Service
public class UserLoginHistoryService {

	@Autowired
	UserLoginHistoryRepository userLoginHistoryRepository;
	
	
	public Page<UserLoginHistory> getAll(Pageable pageable, UserLoginHistory userLoginHistory) {
		UserLoginHistorySpecification spec = new UserLoginHistorySpecification ();
		
		List<UserLoginHistoryDTO> listaRetorno = new ArrayList<UserLoginHistoryDTO>();
		
		Page<UserLoginHistory> lista = userLoginHistoryRepository.findAll(Specification.where(
				spec.idEquals(userLoginHistory.getId()))
			.and(spec.findByLogin(userLoginHistory.isLogin()))
			.and(spec.usuarioEquals(userLoginHistory.getUsuario()))
			.and(spec.betweenDates(userLoginHistory.getDateBefore(), userLoginHistory.getDateAfter())),pageable
		);
//		for(UserLoginHistory objeto : lista) {
//			UserLoginHistoryDTO entity = new UserLoginHistoryDTO();
//			entity.setId(objeto.getId());
//			entity.setUsuario(objeto.getUsuario().getId());
//			entity.setLogin(objeto.isLogin());
//			entity.setEventDate(objeto.getEventDate());
//			
//			listaRetorno.add(entity);
//		}
//		
//		int start = Integer.parseInt(String.valueOf(pageable.getOffset()));
//		int end = (start + pageable.getPageSize()) > listaRetorno.size() ? listaRetorno.size() : (start + pageable.getPageSize());
		return lista;
	}
	
	public UserLoginHistory saveNewLoginHistory(Usuario usuario, String fingerprint) {
		
		UserLoginHistory userLoginHistory = new UserLoginHistory();
		
		Calendar calendar = Calendar.getInstance();
		Date today = calendar.getTime();
		
		userLoginHistory.setEventDate(today);
		userLoginHistory.setLogin(true);
		userLoginHistory.setUsuario(usuario);
		userLoginHistory.setFingerPrint(fingerprint);
		
		return userLoginHistoryRepository.save(userLoginHistory);
	}
	
	public UserLoginHistory saveNewLogoutHistory(Usuario usuario, String fingerprint) {
		
		UserLoginHistory userLoginHistory = new UserLoginHistory();
		
		Calendar calendar = Calendar.getInstance();
		Date today = calendar.getTime();
		
		userLoginHistory.setEventDate(today);
		userLoginHistory.setLogin(false);
		userLoginHistory.setUsuario(usuario);
		userLoginHistory.setFingerPrint(fingerprint);
		
		return userLoginHistoryRepository.save(userLoginHistory);
	}

	public UserLoginHistoryRepository getUserLoginHistoryRepository() {
		return userLoginHistoryRepository;
	}

	public void setUserLoginHistoryRepository(UserLoginHistoryRepository userLoginHistoryRepository) {
		this.userLoginHistoryRepository = userLoginHistoryRepository;
	}
}
