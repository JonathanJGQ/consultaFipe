package com.sif.core.controller;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sif.core.exception.GenericException;
import com.sif.core.service.AdministracaoService;
import com.sif.core.service.AverbadoraService;
import com.sif.core.service.ConsignatariaService;
import com.sif.core.service.MensagemDestinoService;
import com.sif.core.service.MensagemService;
import com.sif.core.service.OrgaoService;
import com.sif.core.service.UsuarioService;
import com.sif.core.utils.Funcoes;
import com.sif.core.utils.TipoPerfil;
import com.sif.model.Administracao;
import com.sif.model.Averbadora;
import com.sif.model.Consignataria;
import com.sif.model.Mensagem;
import com.sif.model.MensagemDestino;
import com.sif.model.Orgao;
import com.sif.model.Usuario;
import com.sif.model.custom.MensagemCustomDTO;

@RestController
@RequestMapping("/mensagem")
public class MensagemController {
	
	@Autowired
	MensagemService mensagemService;
	
	@Autowired
	MensagemDestinoService mensagemDestinoService;
	
	@Autowired
	UsuarioService usuarioService;
	
	@Autowired
	AdministracaoService administracaoService;
	
	@Autowired
	OrgaoService orgaoService;
	
	@Autowired
	AverbadoraService averbadoraService;
	
	@Autowired
	ConsignatariaService consignatariaService;
	
	@Autowired
	Funcoes funcoes;
	
	
	@GetMapping
	public Page<Mensagem> pesquisa(Pageable pageable, MensagemCustomDTO mensagemCustomDTO){
		return mensagemService.getAll(pageable, mensagemCustomDTO);
		
	}

	@GetMapping("/recebidas/{id}")
	@PreAuthorize("hasAuthority('/mensagem/recebidas')")
	public Page<Mensagem> getMessagesOfUser(Pageable pageable, @PathVariable("id") Long id, MensagemCustomDTO mensagemCustomDTO){
		
		Usuario usuario = funcoes.getLoggedUser();
		
		if(mensagemCustomDTO.getDataFinal() != null) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(mensagemCustomDTO.getDataFinal());
			calendar.add(Calendar.DAY_OF_MONTH, 1);
			mensagemCustomDTO.setDataFinal(calendar.getTime());
		}
		
		Page<MensagemDestino> mensagensDestino = mensagemService.getPageMessagesByDestino(pageable, id, usuario, mensagemCustomDTO);
		
		if(mensagensDestino == null) {
			throw new GenericException("Erro", "Não há mensagens para este usuário!");
		}
		
		//Adicionando em uma lista de mensagens e apos isso retornando
		List<Mensagem> mensagens = new ArrayList<Mensagem>();
		for(MensagemDestino mensagemDestino : mensagensDestino) {
			Mensagem mensagem = mensagemDestino.getMensagem();
			mensagem.setSeenByUser(mensagemDestino.getLida());
			mensagens.add(mensagem);
		}
		
		Long start = pageable.getOffset();
		Long end = (start + pageable.getPageSize()) > mensagens.size() ? mensagens.size() : (start + pageable.getPageSize());
		Page<Mensagem> pages = new PageImpl<Mensagem>(mensagens.subList(start.intValue(), end.intValue()), pageable, mensagens.size());
		return pages;
	}
	
	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('/mensagem/list')")
	public ResponseEntity<MensagemCustomDTO> getMessageById(@PathVariable("id") Long id){
		
		Usuario usuario = funcoes.getLoggedUser();
		
		Mensagem mensagem = mensagemService.getMessageById(id, usuario);
		
		List<MensagemDestino> mensagensDestinatarios = mensagemDestinoService.getMensagensDestinoByMensagem(mensagem);
		
		for(MensagemDestino mensagemDestino : mensagensDestinatarios) {
			if(mensagemDestino.getDestino().getId() == usuario.getId()) {
				
				mensagemDestino.setLida(true);
				mensagemDestinoService.save(mensagemDestino);
				break;
			}
		}
		
		if(mensagem == null) {
			throw new GenericException("Erro", "Esta mensagem não existe ou você não tem acesso a ela.");
		}
		
		return Optional
				.ofNullable(mensagemService.mensagemToDTO(mensagem))
				.map(mensagemAux -> ResponseEntity.ok().body(mensagemAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
		
	}
	
	@GetMapping("/enviadas/{id}")
	@PreAuthorize("hasAuthority('/mensagem/enviadas')")
	public ResponseEntity<List<MensagemCustomDTO>> getMensagensEnviadas(@PathVariable("id") Long id) {
		Usuario usuario = funcoes.getLoggedUser();
		
		List<Mensagem> mensagens = mensagemService.getMessagesByRemetente(id, usuario);
	
		if(mensagens == null) {
			throw new GenericException("Erro", "Nenhuma mensagem foi encontrada!");
		}
		
		List<MensagemCustomDTO> mensagensDTO = new ArrayList<MensagemCustomDTO>();
		
		for(Mensagem mensagem : mensagens) {
			mensagensDTO.add(mensagemService.mensagemToDTO(mensagem));
		}
		
		return Optional
				.ofNullable(mensagensDTO)
				.map(mensagemAux -> ResponseEntity.ok().body(mensagemAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	@PostMapping
	@PreAuthorize("hasAuthority('/mensagem/new')")
	public ResponseEntity<String> enviarMensagem(@RequestBody MensagemCustomDTO mensagemCustomDTO) {
		
		Usuario usuarioLogado = funcoes.getLoggedUser();
		mensagemCustomDTO.setRemetente(usuarioLogado.getId());
		
		Calendar calendar = Calendar.getInstance();
		Date today = calendar.getTime();
		mensagemCustomDTO.setDataRegistro(today);
		
		List<Long> ids = new ArrayList<Long>();
		
		if(mensagemCustomDTO.getFiltro() == null) {
			throw new GenericException("Erro", "Você deve selecionar algum tipo de filtro.");
		}
		
		if(mensagemCustomDTO.getFiltro() == 0) {
			//Pegando todos os usuarios da UF
			
			List<Usuario> usuarios = usuarioService.findAll();
			
			if(usuarios.contains(usuarioLogado)) {
				usuarios.remove(usuarioLogado);
			}
			
			for(Usuario usuario : usuarios) {
				ids.add(usuario.getId());
			}
			
		} else if (mensagemCustomDTO.getFiltro() == 1) {
			//Pegando todos os usuarios das Entidades
			
			if(mensagemCustomDTO.getDestinatarios() == null 
				|| mensagemCustomDTO.getDestinatarios().isEmpty()) {
				
				//Bloco que pega os usuarios de administracoes
				if(mensagemCustomDTO.getEntidade().getAdministracoes() != null
					&& !mensagemCustomDTO.getEntidade().getAdministracoes().isEmpty()) {
					
					for(Long administracaoID 
						: mensagemCustomDTO.getEntidade().getAdministracoes()) {
						
						Administracao administracao = administracaoService.findAdministracaoById(administracaoID);
						
						if(administracao != null) {
							List<Usuario> usuarios = usuarioService.findByPerfilEntidade(TipoPerfil.ADMINISTRADOR, administracaoID);
						
							for(Usuario usuarioDestino : usuarios) {
								ids.add(usuarioDestino.getId());
							}
						}
						
					}
					
				}
				
				//Bloco que pega os usuarios de orgaos
				if(mensagemCustomDTO.getEntidade().getOrgaos() != null
						&& !mensagemCustomDTO.getEntidade().getOrgaos().isEmpty()) {
					
					for(Long orgaoID 
							: mensagemCustomDTO.getEntidade().getOrgaos()) {
						
						Orgao orgao = orgaoService.findOrgaoById(orgaoID);
						
						if(orgao != null) {
							
							List<Usuario> usuarios = usuarioService.findByPerfilEntidade(TipoPerfil.ORGAO, orgaoID);
							
							for(Usuario usuarioDestino : usuarios) {
								ids.add(usuarioDestino.getId());
							}
						}
					}
				}
				
				//Bloco que pega os usuarios de averbadoras
				if(mensagemCustomDTO.getEntidade().getAverbadoras() != null
						&& !mensagemCustomDTO.getEntidade().getAverbadoras().isEmpty()) {
					
					for(Long averbadoraID 
							: mensagemCustomDTO.getEntidade().getAverbadoras()) {
					
						Averbadora averbadora = averbadoraService.findAverbadoraById(averbadoraID);
						
						if(averbadora != null) {
							
							List<Usuario> usuarios = usuarioService.findByPerfilEntidade(TipoPerfil.AVERBADORA, averbadoraID);
							
							for(Usuario usuarioDestino : usuarios) {
								ids.add(usuarioDestino.getId());
							}
							
						}
						
					}
					
				}
				
				//Bloco que pega os usuarios de consignatarias
				if(mensagemCustomDTO.getEntidade().getConsignatarias() != null
						&& !mensagemCustomDTO.getEntidade().getConsignatarias().isEmpty()) {
					
					for(Long consignatariaID 
							: mensagemCustomDTO.getEntidade().getConsignatarias()) {
						
						Consignataria consignataria = consignatariaService.findConsignatariaById(consignatariaID);
						
						if(consignataria != null) {
							List<Usuario> usuarios = usuarioService.findByPerfilEntidade(TipoPerfil.CONSIGNATARIA, consignatariaID);
							
							for(Usuario usuarioDestino : usuarios) {
								ids.add(usuarioDestino.getId());
							}
						}
						
					}
					
				}
				
			} else {
				
				ids = mensagemCustomDTO.getDestinatarios();
				
			}
			
		} else if (mensagemCustomDTO.getFiltro() == 2) {
			//pegando os usuarios especificos
			
			ids = mensagemCustomDTO.getDestinatarios();
		}
		
		mensagemCustomDTO.setDestinatarios(ids);
		
		if(mensagemCustomDTO.getDestinatarios() == null || mensagemCustomDTO.getDestinatarios().isEmpty()){
			throw new GenericException("Erro", "Lista de destinatários não deve ser vazia");
		}
		
		mensagemCustomDTO.setLida(false);
		Mensagem mensagemCriada = mensagemService.dtoToNewMensagem(mensagemCustomDTO);
		mensagemService.validarMensagem(mensagemCriada);
		mensagemCriada.setStatus(true);
		mensagemCriada.setFiltro(mensagemCustomDTO.getFiltro());
		
		Mensagem mensagemSalva = mensagemService.saveNewMensagem(mensagemCriada, mensagemCustomDTO.getDestinatarios());
	
		if(mensagemSalva != null) {
			return  Optional
						.ofNullable(Funcoes.jsonMessage("Mensagem enviada", "A mensagem foi enviada com sucesso", "200"))
						.map(margemAux -> ResponseEntity.ok().body(margemAux))
						.orElseGet(() -> ResponseEntity.notFound().build());
					
		}
		
		return Optional
				.ofNullable(Funcoes.jsonMessage("A mensagem não foi enviada", "Não foi possível enviar esta mensagem", "500"))
				.map(margemAux -> ResponseEntity.status(400).body(margemAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
				
	}
	
	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('/mensagem/new')")
	public ResponseEntity<String> editarMensagem(@RequestBody MensagemCustomDTO mensagemCustomDTO, @PathVariable Long id) {
		
		if(mensagemCustomDTO.getId() == null) {
			return Optional
					.ofNullable(Funcoes.jsonMessage("A mensagem não foi editada", "O id da mensagem não pode estar vazio", "500"))
					.map(margemAux -> ResponseEntity.status(400).body(margemAux))
					.orElseGet(() -> ResponseEntity.notFound().build());
		}
		
		Mensagem mensagem = mensagemService.dtoToEditMensagem(mensagemCustomDTO);
		if(mensagem == null) {
			return Optional
					.ofNullable(Funcoes.jsonMessage("A mensagem não foi editada", "Não foi possível encontrar esta mensagem", "500"))
					.map(margemAux -> ResponseEntity.status(400).body(margemAux))
					.orElseGet(() -> ResponseEntity.notFound().build());
					
		}
		
		mensagem.setDataRegistro(new Date());
		mensagemService.validarMensagem(mensagem);
		
		Usuario usuarioLogado = funcoes.getLoggedUser();
		
		if(usuarioLogado.getId() != mensagem.getRemetente().getId()) {
			throw new GenericException("Erro", "Você não é o remetente desta mensagem");
		}
		
		List<Long> ids = new ArrayList<Long>();
		
		if(mensagemCustomDTO.getFiltro() == null) {
			throw new GenericException("Erro", "Você deve selecionar algum tipo de filtro.");
		}
		
		
		Mensagem mensagemEditada = mensagemService.editMensagem(mensagem);
		if(mensagemEditada == null) {
			return Optional
					.ofNullable(Funcoes.jsonMessage("A mensagem não foi editada", "Não foi possível editar esta mensagem", "500"))
					.map(margemAux -> ResponseEntity.status(400).body(margemAux))
					.orElseGet(() -> ResponseEntity.notFound().build());
					
		}
		
		return Optional
				.ofNullable(Funcoes.jsonMessage("Mensagem editada", "A mensagem foi editada com sucesso", "200"))
				.map(margemAux -> ResponseEntity.status(200).body(margemAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
			
	}
	
	@GetMapping("/notification")
	public ResponseEntity<String> getLastNotSeenMessage(){
		Usuario usuarioLogado = funcoes.getLoggedUser();
		Calendar calendar = Calendar.getInstance();
		Date today = calendar.getTime();
		
		Mensagem mensagem = mensagemService.getLastNotSeenMessage(usuarioLogado.getId(), today);
		
		JSONObject jsonObject = new JSONObject();
		JSONObject jsonObjectMessagem = new JSONObject();
		
		if(mensagem != null) {
		
			jsonObjectMessagem.put("id", mensagem.getId());
			jsonObjectMessagem.put("titulo", mensagem.getTitulo());
			jsonObjectMessagem.put("corpo", mensagem.getCorpo());
			jsonObjectMessagem.put("dataInicial", mensagem.getDataInicial());
			jsonObjectMessagem.put("dataFinal", mensagem.getDataFinal());
			jsonObjectMessagem.put("dataRegistro", mensagem.getDataRegistro());
			jsonObjectMessagem.put("status", mensagem.isStatus());
			jsonObjectMessagem.put("remetente", mensagem.getRemetente().getNome());
			jsonObjectMessagem.put("seenByUser", "false");
		}	
		jsonObject.put("mensagem", jsonObjectMessagem);
		

		List<MensagemDestino> mensagensNaoLidas = mensagemDestinoService.getMessagesNaoLidasByDestino(usuarioLogado);

		if(mensagensNaoLidas != null && !mensagensNaoLidas.isEmpty()) {
			
			jsonObject.put("quantidade", Integer.toString(mensagensNaoLidas.size()));
		} else {
			jsonObject.put("quantidade", "0");
		}
		
		return Optional
				.ofNullable(jsonObject.toString())
				.map(mensagemAux -> ResponseEntity.ok().body(mensagemAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	@GetMapping("/quantidadeNaoLidas")
	public ResponseEntity<String> getQuantidadeMensagensNaoLidas(){
		Usuario usuarioLogado = funcoes.getLoggedUser();
		
		List<MensagemDestino> mensagensNaoLidas = mensagemDestinoService.getMessagesNaoLidasByDestino(usuarioLogado);
	
		JSONObject jsonObject = new JSONObject();
		if(mensagensNaoLidas != null && !mensagensNaoLidas.isEmpty()) {
			
			jsonObject.put("quantidade", Integer.toString(mensagensNaoLidas.size()));
		} else {
			jsonObject.put("quantidade", "0");
		}
		
		return Optional
				.ofNullable(jsonObject.toString())
				.map(margemAux -> ResponseEntity.status(200).body(margemAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('/mensagem/delete')")
	public ResponseEntity<String> apagarMensagem(@PathVariable("id") String id) {
		
		if(id == null || id.isEmpty()) {
			
			return Optional
					.ofNullable(Funcoes.jsonMessage("A mensagem não foi deletada", "Id inválido", "500"))
					.map(administracaoAux -> ResponseEntity.status(400).body(administracaoAux))
					.orElseGet(() -> ResponseEntity.badRequest().build());
		}
		
		Long idParsed = Long.parseLong(id);
		
		Usuario usuarioLogado = funcoes.getLoggedUser();
		
		Mensagem mensagem = mensagemService.getMessageByIdERemetente(idParsed, usuarioLogado);
		if(mensagem != null) {
			Mensagem mensagemDeletada = mensagemService.deleteMensagem(mensagem);
			
			if(mensagemDeletada != null) {
				return Optional
						.ofNullable(Funcoes.jsonMessage("A mensagem deletada", "A mensagem foi deletada com sucesso", "200"))
						.map(administracaoAux -> ResponseEntity.status(200).body(administracaoAux))
						.orElseGet(() -> ResponseEntity.badRequest().build());
			}
		}
		
		return Optional
				.ofNullable(Funcoes.jsonMessage("A mensagem não foi deletada", "Não foi possível encontrar a mensagem", "500"))
				.map(administracaoAux -> ResponseEntity.status(404).body(administracaoAux))
				.orElseGet(() -> ResponseEntity.badRequest().build());
	}
	
}
