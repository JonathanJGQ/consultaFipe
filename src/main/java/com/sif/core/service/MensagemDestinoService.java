package com.sif.core.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.sif.model.Mensagem;
import com.sif.model.MensagemDestino;
import com.sif.model.Usuario;
import com.sif.model.custom.MensagemCustomDTO;
import com.sif.repository.MensagemDestinoRepository;
import com.sif.repository.specification.MensagemDestinoSpecification;

@Service
public class MensagemDestinoService {

	@Autowired
	MensagemDestinoRepository mensagemDestinoRepository;
	
	//Pegando mensagens pelo destinatario
	public List<Mensagem> getMessagesByDestino(Usuario usuario){
		
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		Date today = calendar.getTime();
		
		MensagemDestinoSpecification mensagemDestinoSpecification = new MensagemDestinoSpecification();
		
		//Pegando mensagens pelo destinatário ativo
		List<MensagemDestino> mensagensByDestino
			= mensagemDestinoRepository.findAll(Specification.where(
				mensagemDestinoSpecification.findByDestino(usuario)
			).and(mensagemDestinoSpecification.findByStatusDestino(usuario))
			.and(mensagemDestinoSpecification.beforeDate(today))
			.and(mensagemDestinoSpecification.afterDate(today))
			.and(mensagemDestinoSpecification.findByStatusMensagem()));
		
		//Adicionando em uma lista de mensagens e apos isso retornando
		List<Mensagem> mensagens = new ArrayList<Mensagem>();
		for(MensagemDestino mensagemDestino : mensagensByDestino) {
			mensagens.add(mensagemDestino.getMensagem());
		}
		
		return mensagens;
	}
	
	public Page<MensagemDestino> getPageMessagesByDestino(Pageable pageable, Usuario usuario, MensagemCustomDTO mensagemCustomDTO){
		
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		Date today = calendar.getTime();
		
		MensagemDestinoSpecification mensagemDestinoSpecification = new MensagemDestinoSpecification();
		
		//Pegando mensagens pelo destinatário ativo
		return mensagemDestinoRepository.findAll(Specification.where(
				mensagemDestinoSpecification.findByDestino(usuario)
			).and(mensagemDestinoSpecification.findByStatusDestino(usuario))
			.and(mensagemDestinoSpecification.beforeDate(today))
			.and(mensagemDestinoSpecification.afterDate(today))
			.and(mensagemDestinoSpecification.beforeDateFinal(mensagemCustomDTO.getDataFinal()))
			.and(mensagemDestinoSpecification.afterDateInicial(mensagemCustomDTO.getDataInicial()))
			.and(mensagemDestinoSpecification.findByTitulo(mensagemCustomDTO.getTitulo()))
			.and(mensagemDestinoSpecification.findByStatusMensagem())
			.and(mensagemDestinoSpecification.orderById()), pageable);
	}
	
	public List<MensagemDestino> getMessagesNaoLidasByDestino(Usuario usuario){
		
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		Date today = calendar.getTime();
		
		MensagemDestinoSpecification mensagemDestinoSpecification = new MensagemDestinoSpecification();
		
		//Pegando mensagens pelo destinatário ativo
		List<MensagemDestino> mensagensByDestino
			= mensagemDestinoRepository.findAll(Specification.where(
				mensagemDestinoSpecification.findByStatusDestino(usuario)
			)
			.and(mensagemDestinoSpecification.findMessageNotSeen())
			.and(mensagemDestinoSpecification.beforeDate(today))
			.and(mensagemDestinoSpecification.afterDate(today))
			.and(mensagemDestinoSpecification.findByStatusMensagem()));
		
		return mensagensByDestino;
	}
	
	public Mensagem getMessagesByMensagemEDestino(Mensagem mensagem, Usuario usuario){
		
		MensagemDestinoSpecification mensagemDestinoSpecification = new MensagemDestinoSpecification();
		
		//Pegando uma mensagem na qual seja igua o usuario passado e a mensagem
		Optional<MensagemDestino> optional = mensagemDestinoRepository.findOne(Specification.where(
				mensagemDestinoSpecification.findByStatusDestino(usuario)
			).and(mensagemDestinoSpecification.findByStatusMensagem(mensagem)));
		
		if(optional.isPresent()) {
			
			return optional.get().getMensagem();
		}
		
		return null;
	}
	
	public MensagemDestino getMessagesDestinoByMensagemEDestino(Mensagem mensagem, Usuario usuario){
		
		MensagemDestinoSpecification mensagemDestinoSpecification = new MensagemDestinoSpecification();
		
		//Pegando uma mensagem na qual seja igua o usuario passado e a mensagem
		Optional<MensagemDestino> optional = mensagemDestinoRepository.findOne(Specification.where(
				mensagemDestinoSpecification.findByStatusDestino(usuario)
			).and(mensagemDestinoSpecification.findByStatusMensagem(mensagem)));
		
		if(optional.isPresent()) {
			
			return optional.get();
		}
		
		return null;
	}
	
	public List<MensagemDestino> getMensagensDestinoByMensagem(Mensagem mensagem){
		
		MensagemDestinoSpecification mensagemDestinoSpecification = new MensagemDestinoSpecification();
		
		//Pegando uma mensagem na qual seja igua o usuario passado e a mensagem
		List<MensagemDestino> mensagensDestino = mensagemDestinoRepository.findAll(Specification.where(
				mensagemDestinoSpecification.findByStatusMensagem(mensagem)));
		
		return mensagensDestino;
	}
	
	public MensagemDestino saveNew(Mensagem mensagem, Usuario destinatario) {
		MensagemDestino mensagemDestino = new MensagemDestino();
		mensagemDestino.setMensagem(mensagem);
		mensagemDestino.setDestino(destinatario);
		mensagemDestino.setLida(false);
		
		return mensagemDestinoRepository.save(mensagemDestino);
	}
	
	public void delete(MensagemDestino mensagemDestino) {
		mensagemDestinoRepository.delete(mensagemDestino);
	}
	
	public void save(MensagemDestino mensagemDestino) {
		mensagemDestinoRepository.save(mensagemDestino);
	}
	
}
