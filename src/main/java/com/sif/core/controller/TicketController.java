package com.sif.core.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sif.core.exception.GenericException;
import com.sif.core.service.AdministracaoService;
import com.sif.core.service.AverbadoraService;
import com.sif.core.service.ConsignatariaService;
import com.sif.core.service.OrgaoService;
import com.sif.core.service.TicketService;
import com.sif.core.service.UsuarioService;
import com.sif.core.utils.Funcoes;
import com.sif.core.utils.TipoPerfil;
import com.sif.model.Administracao;
import com.sif.model.Averbadora;
import com.sif.model.Consignataria;
import com.sif.model.Orgao;
import com.sif.model.Perfil;
import com.sif.model.Usuario;
import com.sif.model.custom.TicketDTO;

@RestController
@RequestMapping("/ticket")
public class TicketController {
	
	@Autowired
	UsuarioService usuarioService;
	
	@Autowired
	TicketService ticketService;
	
	@Autowired
	AverbadoraService averbadoraService;
	
	@Autowired
	OrgaoService orgaoService;
	
	@Autowired
	AdministracaoService administracaoService;
	
	@Autowired
	ConsignatariaService consignatariaService;
	
	@Autowired
	Funcoes funcoes;
	
	@GetMapping
	@PreAuthorize("hasAuthority('/ticket/list')")
	public Page<TicketDTO> getAll(Pageable pageable, TicketDTO ticketDTO){
		try {
			return ticketService.getFilteredTickets(pageable, ticketDTO);
		} catch (IOException | URISyntaxException e) {
			
			e.printStackTrace();
			throw new GenericException("Erro", "Não foi possível realizar a consulta");
		}
	}
	
	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('/ticket/list')")
	public TicketDTO getTicket(@PathVariable Long id) {
		if(id == null) {
			throw new GenericException("Erro","Id inválido");
		}
		
		try {
			TicketDTO ticketDTO = ticketService.getTicket(Long.toString(id));
			
			return ticketDTO;
		} catch (IOException | URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	@PostMapping
	@PreAuthorize("hasAuthority('/ticket/new')")
	public TicketDTO createTicket(@RequestBody TicketDTO ticket) {
		
		try {
			
//			JSONObject jsonObject = new JSONObject(ticket);
			
//			TicketDTO ticketDTO = ticketService.parseJSON(jsonObject);
//			ticketDTO.setFiles(new ArrayList<File>());
			
			TicketDTO ticketDTO = ticket;
			
//			for(MultipartFile attach : files) {
//				
//				File convFile = new File(attach.getOriginalFilename());
//			    convFile.createNewFile(); 
//			    FileOutputStream fos = new FileOutputStream(convFile); 
//			    fos.write(attach.getBytes());
//			    fos.close();
//				
//				ticketDTO.getAttachments().add(convFile);
//			}
			
			Usuario usuario = funcoes.getLoggedUser();
			Perfil perfil = usuario.getPerfil();
			
			if(usuario.getItsmID() == null || usuario.getItsmID().isEmpty()) {
				throw new GenericException("Erro","Este usuário não tem um indentificador para o Fresh Desk");
			}
			
			//Preenchendo o ITSM id
			if(perfil.getId() == TipoPerfil.AVERBADORA) {
				Averbadora averbadora = averbadoraService.findAverbadoraById(usuario.getEntidade());
				
				ticketDTO.setCompany_id(averbadora.getItsmID());
			} else if(perfil.getId() == TipoPerfil.ORGAO) {
				Orgao orgao = orgaoService.findOrgaoById(usuario.getEntidade());
				
				ticketDTO.setCompany_id(orgao.getItsmID());
			} else if(perfil.getId() == TipoPerfil.CONSIGNATARIA) {
				Consignataria consignataria = consignatariaService.findConsignatariaById(usuario.getEntidade());
				
				ticketDTO.setCompany_id(consignataria.getItsmID());
			} else if(perfil.getId() == TipoPerfil.ADMINISTRADOR) {
				Administracao administracao = administracaoService.findAdministracaoById(usuario.getEntidade());
				
				ticketDTO.setCompany_id(administracao.getItsmID());
			}
			
			//Preenchendo os dados do ticket
			ticketDTO.setRequest_id(usuario.getItsmID());
			ticketDTO.setEmail(usuario.getEmail());
			ticketDTO.setPhone(usuario.getCelular());
			
			ticketDTO.setStatus("2");
			ticketDTO.setCpf(usuario.getDocumento());
			ticketDTO.setNomeUsuario(usuario.getNome());
			
			validadarTicket(ticketDTO);
		
		
			if(ticketService.createTicket(ticketDTO)) {
				return ticketDTO;
			} else {
				throw new GenericException("Erro","Algo de errado aconteceu ao tentar abrir este ticket!");
			}
		} catch (IOException | URISyntaxException e) {
			e.printStackTrace();
			
		}
		
		throw new GenericException("Erro","Algo de errado aconteceu ao tentar abrir este ticket!");
	}
	
	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('/ticket/new')")
	public TicketDTO deleteTicket(@PathVariable String id) {
		try {
			
			/*
			 * Esta funcao foi alterada para apenas fechar o ticket
			 */
			
			if(id == null || id.isEmpty()) {
				throw new GenericException("Erro","Id inválido");
			}
			
			TicketDTO ticketDTO = ticketService.getTicket(id);
			
			if(ticketDTO == null) {
				throw new GenericException("Erro","Este ticket não foi encontrado");
			}
			
			ticketDTO.setStatus("5");
			if(ticketService.updateTicket(ticketDTO)) {
				return ticketDTO;
			} else {
				throw new GenericException("Erro","Não foi possível fechar este ticket");
			}
			
//			if(ticketService.deleteTicket(Long.parseLong(id))) {
//				return ticketDTO;
//			} else {
//				throw new GenericException("Erro","Não foi possível deletar este ticket");
//			}
			
		} catch (IOException | URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('/ticket/new')")
	public TicketDTO update(@RequestBody TicketDTO ticket, @PathVariable() Long id) {
		try {
			
			if(id == null) {
				throw new GenericException("Erro","Id inválido");
			}
			
//			JSONObject jsonObject = new JSONObject(ticket);
//			
//			TicketDTO ticketDTO = ticketService.parseJSON(jsonObject);
//			ticketDTO.setFiles(new ArrayList<File>());
			
			TicketDTO ticketDTO = ticket;
			ticketDTO.setId(Long.toString(id));
			
			validadarTicket(ticketDTO);
			
			
			if(ticketService.updateTicket(ticketDTO)) {
				return ticketDTO;
			} else {
				throw new GenericException("Erro","Não foi possível alterar este ticket");
			}
		} catch (IOException | URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	public void validadarTicket(TicketDTO ticketDTO) throws GenericException {
		if(ticketDTO.getSubject() == null || ticketDTO.getSubject().isEmpty()) {
			throw new GenericException("Erro","Assunto não pode estar vazio");
		}
		
		if(ticketDTO.getDescription() == null || ticketDTO.getDescription().isEmpty()) {
			throw new GenericException("Erro","Descrição não pode estar vazia");
		}
		
		if(ticketDTO.getType() == null || ticketDTO.getType().isEmpty()) {
			throw new GenericException("Erro","Tipo não pode estar vazia");
		} else {
			if(!ticketDTO.getType().equals("Question")
				&& !ticketDTO.getType().equals("Incident")
				&& !ticketDTO.getType().equals("Problem")
				&& !ticketDTO.getType().equals("Feature Request")
				&& !ticketDTO.getType().equals("Refund")) {
				throw new GenericException("Erro","Tipo tem que ser inserido em um desses tipos: Question, Incident, Problem, Feature Request ou Refund");
			}
		}
		
		if(ticketDTO.getPriority() != 1
			&& ticketDTO.getPriority() != 2
			&& ticketDTO.getPriority() != 3
			&& ticketDTO.getPriority() != 4) {
			throw new GenericException("Erro","A priorida deve ser um número, onde: 1 - Baixa, 2 - Média, 3 - Alta, 4 - Urgente");
		}
	}

}
