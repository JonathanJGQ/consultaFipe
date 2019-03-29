package com.sif.core.utils;

import java.net.Proxy;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.lacunasoftware.restpki.RestPkiClient;
import com.sif.core.service.FuncionalidadeService;
import com.sif.core.service.LogAcaoService;
import com.sif.core.service.ModuloService;
import com.sif.core.service.PerfilFuncionalidadeService;
import com.sif.model.Administracao;
import com.sif.model.Averbadora;
import com.sif.model.Consignataria;
import com.sif.model.DescricaoLogAcao;
import com.sif.model.Endereco;
import com.sif.model.Funcionalidade;
import com.sif.model.LogAcao;
import com.sif.model.Modulo;
import com.sif.model.Orgao;
import com.sif.model.Usuario;
import com.sif.model.UsuarioPermissao;
import com.sif.model.custom.EnderecoCustomDTO;
import com.sif.repository.AdministracaoRepository;
import com.sif.repository.AverbadoraRepository;
import com.sif.repository.CidadeRepository;
import com.sif.repository.ConsignatariaRepository;
import com.sif.repository.FuncionalidadeRepository;
import com.sif.repository.ModuloRepository;
import com.sif.repository.OrgaoRepository;
import com.sif.repository.PerfilFuncionalidadeRepository;
import com.sif.repository.UsuarioPermissaoRepository;
import com.sif.repository.UsuarioRepository;
import com.sif.repository.specification.ModuloSpecification;

@Service
public class Funcoes {
	
	@Autowired
	CidadeRepository cidadeRepository;
	
	@Autowired
	LogAcaoService logAcaoService;
	
	@Autowired
	ModuloRepository moduloRepository;
	
	@Autowired
	UsuarioRepository usuarioRepository;
	
	@Autowired
	AdministracaoRepository administracaoRepository;
	
	@Autowired
	OrgaoRepository orgaoRepository;
	
	@Autowired
	ConsignatariaRepository consignatariaRepository;
	
	@Autowired
	AverbadoraRepository averbadoraRepository;
	
	@Autowired
	ModuloService moduloService;
	
	@Autowired
	FuncionalidadeService funcionalidadeService;
	
	@Autowired
	FuncionalidadeRepository funcionalidadeRepository;
	
	@Autowired
	PerfilFuncionalidadeService perfilFuncionalidadeService;
	
	@Autowired
	PerfilFuncionalidadeRepository perfilFuncionalidadeRepository;
	
	@Autowired
	UsuarioPermissaoRepository usuarioPermissaoRepository;
	
	// PASTE YOUR API ACCESS TOKEN BELOW
	private static final String restPkiAccessToken = "2ghteLkkqujgqEsIdtiVLRSeq0govqDIidTKNTPhReb4vQWuOTXz3FfefSWGtB9R_VE3rVs7OCI05dVCNkh5uJcMuiueZ1QLumIFRbqhQG7kMYF4LvQu65sTthBCweQh5lx2t1KZI3YdICjsPKAdrqJEvfZ6VCDnP6qTqjGcjMLcp1TmQYoe3Nh-w4T9I6z9924rrBS3g3_eavdlhy0nJZjfS_daFgux_2XfnxFcp70_F5BzfLHei4kdVlgQMrFhNr4iGt5F1_dhatSTY9rMMst5vyGTSvlrm8qD3JWDuSTHeI1WgJeokBPkjTjbGD1_oMpspyoigNi-qIrIK_wrGSXyJOk4zdMuIO6Ad9LBHd_hSLsT-r9vnq_a8TYiatO0haVTXBVbKjdT_c3xzxQWCaquoPSegXoU2naeWZxPKSG6qeFdjj3XpI30zdhZ3x8-FR_s3q9-Kbwp5e3mL2CjglXZqvEOPE2Cgf6laE4XBqwQGZTJAbR0G3OIKkT5ReEBS7I_MapFGu01kEE0H2OAdYuCvRA";
	//
	
	public EnderecoCustomDTO enderecoToDTO(Endereco endereco) {
		
		EnderecoCustomDTO enderecoDto = new EnderecoCustomDTO();
		
		enderecoDto.setBairro(endereco.getBairro());
		enderecoDto.setCelular(endereco.getCelular());
		enderecoDto.setCep(endereco.getCep());
		enderecoDto.setCidade_id(endereco.getCidade().getId());
		enderecoDto.setId(endereco.getId());
		enderecoDto.setLogradouro(endereco.getLogradouro());
		enderecoDto.setNumero(endereco.getNumero());
		enderecoDto.setTelefone(endereco.getTelefone());
		enderecoDto.setUf_id(endereco.getCidade().getUf().getId());
		enderecoDto.setDdd(endereco.getDdd());
		enderecoDto.setComplemento(endereco.getComplemento());
		
		return enderecoDto;
		
	}
	
	public Endereco dtoToEndereco(EnderecoCustomDTO enderecoDto) {
		
		Endereco endereco = new Endereco();
		
		endereco.setBairro(enderecoDto.getBairro());
		endereco.setCelular(enderecoDto.getCelular());
		endereco.setCep(enderecoDto.getCep());
		endereco.setCidade(cidadeRepository.findById(enderecoDto.getCidade_id()).get());
		endereco.setId(enderecoDto.getId());
		endereco.setLogradouro(enderecoDto.getLogradouro());
		endereco.setNumero(enderecoDto.getNumero());
		endereco.setTelefone(enderecoDto.getTelefone());
		endereco.setDdd(enderecoDto.getDdd());
		endereco.setComplemento(enderecoDto.getComplemento());
		
		return endereco;
	}
	
	public static String jsonMessage(String title, String message, String status) {
		JSONObject jsonObject = new JSONObject();
		
		jsonObject.put("title", title);
		jsonObject.put("message", message);
		jsonObject.put("status", status);
		
		return jsonObject.toString();
	}
	
	public LogAcao logAcao(Long idObjeto, Long idDescricao, Usuario usuario) throws Exception {
		if(idDescricao == null || usuario == null)
			return null;
		
 		LogAcao log = new LogAcao();
		log.setDataEvento(new Date());
		DescricaoLogAcao descricao = new DescricaoLogAcao();
		descricao.setId(idDescricao);		
		log.setDescricao(descricao);
		log.setUsuario(usuario);
		log.setIdObjeto(idObjeto);
		return logAcaoService.create(log).getBody();
	}
	
	public Usuario getLoggedUser() {
		
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		
		return usuarioRepository.findByDocumento(auth.getName());
	}
	
	public static RestPkiClient getRestPkiClient() {

		// Throw exception if token is not set (this check is here just for the sake of newcomers, you can remove it)
		if (restPkiAccessToken == null || restPkiAccessToken.equals("") || restPkiAccessToken.contains(" API ")) {
			throw new RuntimeException("The API access token was not set! Hint: to run this sample you must generate an API access token on the REST PKI website and paste it on the file src/main/java/sample/util/Util.java");
		}

		Proxy proxy = null;

		return new RestPkiClient("https://restpki.azurewebsites.net/", restPkiAccessToken, proxy);
	}
	
	public static String getRestPkiAccessToken() {
		return restPkiAccessToken;
	}
	
	public String buildMenu(Usuario usuario) {
		
		List<Modulo> modulos = moduloRepository.pegarModulos();
		
		List<Modulo> menuNeto = pegarModulosNivel3(modulos, usuario);
		List<Modulo> menuFilho = pegarModulosNivel2(modulos, menuNeto, usuario);
		List<Modulo> menuPai = pegarModulosNivel1(modulos, menuFilho, usuario);
		
		//Ordenando modulos
		Collections.sort(menuPai, new Comparator<Modulo>() {

			@Override
			public int compare(Modulo o1, Modulo o2) {
				return  o1.getOrdem().compareTo(o2.getOrdem());
			}
		});
		
		Collections.sort(menuFilho, new Comparator<Modulo>() {

			@Override
			public int compare(Modulo o1, Modulo o2) {
				return  o1.getOrdem().compareTo(o2.getOrdem());
			}
		});
		
		Collections.sort(menuNeto, new Comparator<Modulo>() {

			@Override
			public int compare(Modulo o1, Modulo o2) {
				return  o1.getOrdem().compareTo(o2.getOrdem());
			}
		});
		//Fim da ordenacao
		
		//Retirando separators desnecessários
		menuPai = retirarSeparators(menuPai);
		
		JSONArray arrayPais = new JSONArray();
		
		for(Modulo pai : menuPai) {
			JSONObject objectPai = new JSONObject();
			objectPai.put("id", pai.getId());
			objectPai.put("label", pai.getLabelMenu());
			objectPai.put("path", pai.getPath());
			objectPai.put("icone", pai.getIcone());
			objectPai.put("ordem", pai.getOrdem());
			
			JSONArray arrayFilhos = new JSONArray();
			if(menuFilho != null) {
				for(Modulo filho : menuFilho) {
					if(filho.getIdMaster() == pai.getId()) {
						
						JSONObject objectFilho = new JSONObject();
						objectFilho.put("id", filho.getId());
						objectFilho.put("label", filho.getLabelMenu());
						objectFilho.put("path", filho.getPath());
						objectFilho.put("icone", filho.getIcone());
						objectFilho.put("ordem", filho.getOrdem());
						
						JSONArray arrayNetos = new JSONArray();
						if(menuNeto != null) {
							for(Modulo neto : menuNeto) {
								if(neto.getIdMaster() == filho.getId()) {
									JSONObject objectNeto = new JSONObject();
									objectNeto.put("id", neto.getId());
									objectNeto.put("label", neto.getLabelMenu());
									objectNeto.put("path", neto.getPath());
									objectNeto.put("icone", neto.getIcone());
									objectNeto.put("ordem", neto.getOrdem());
									
									arrayNetos.put(objectNeto);
								}
								
							}
						}
						
						objectFilho.put("submenu", arrayNetos);
						arrayFilhos.put(objectFilho);
					}
				}
			}
			objectPai.put("submenu", arrayFilhos);
			arrayPais.put(objectPai);
		}
		
		return arrayPais.toString();
	}
	
	private List<Modulo> retirarSeparators(List<Modulo> modulos){
		
		ArrayList<Long> separators = new ArrayList<Long>();
		
		for(Modulo modulo : modulos) {
			if(modulo.getLabelMenu().equals("separator")) {
				separators.add(modulo.getId());
			}
		}
		
		Long indiceSeparators = separators.get(0);
		int qtdSeparators = separators.size();
		boolean hasBetween = false;
		Long idInicio = 0L;
		Long idFim = 0L;
		
		while(qtdSeparators != 1) {
			
			
			idInicio = indiceSeparators;
			idFim = separators.get(separators.size() - (qtdSeparators - 1));
			
			Modulo moduloInicio = moduloService.findById(idInicio).getBody();
			Modulo moduloFim = moduloService.findById(idFim).getBody();
			
//			List<Modulo> list = moduloService.betweenOrdens(moduloInicio.getOrdem(), moduloFim.getOrdem());
			
			List<Modulo> list = new ArrayList<Modulo>();
			
			hasBetween = false;
			for(Modulo moduloMeio : modulos) {
				if((moduloMeio.getOrdem() > moduloInicio.getOrdem())
					&& (moduloMeio.getOrdem() < moduloFim.getOrdem())	
					) {
					hasBetween = true;
					break;
				}
			}
			
			if(!hasBetween) {
				
				for(int i = 0; i < modulos.size(); i++) {
					if(moduloInicio.getId() == modulos.get(i).getId()) {
						modulos.remove(modulos.get(i));
						break;
					}
				}
			}
			
//			for(Long id = indiceSeparators + 1; id < separators.get(separators.size() - (qtdSeparators - 1)); id++) {
//				
//				Modulo modulo = moduloService.findById(id).getBody();
//				boolean canDelete = true;
//				for(int i = 0; i < modulos.size(); i++) {
//					if(modulo.getId() == modulos.get(i).getId()) {
//						
//						canDelete = false;
//						
//						break;
//					}
//				}
//				
//				if(canDelete) {
//					Modulo moduloSeparator = moduloService.findById(indiceSeparators).getBody();
//					
//					for(int i = 0; i < modulos.size(); i++) {
//						if(moduloSeparator.getId() == modulos.get(i).getId()) {
//							modulos.remove(modulos.get(i));
//						}
//					}
//				}
//				
//			}
			
			qtdSeparators--;
			indiceSeparators = separators.get(separators.size() - qtdSeparators);
		}
		
		return modulos;
		
	}
	
	public static String gerarHash(String texto) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		md.update(texto.getBytes());
		byte[] hash = md.digest();

		StringBuilder s = new StringBuilder();
		for (int i = 0; i < hash.length; i++) {
			int parteAlta = ((hash[i] >> 4) & 0xf) << 4;
			int parteBaixa = hash[i] & 0xf;
			if (parteAlta == 0)
				s.append('0');
			s.append(Integer.toHexString(parteAlta | parteBaixa));
		}
		return s.toString();

	}
	
	public static String getDataPorExtensoCompleta() {
		Date data = new Date();
		Locale local = new Locale("pt", "BR");
		DateFormat formato = new SimpleDateFormat("dd 'de' MMMM 'de' yyyy 'às' HH:mm:ss", local);
		return formato.format(data);
	}
	
	public String getUsuarioEntidade(Usuario usuario) {
		if(usuario.getPerfil().getNome().equals("Supremo")) {
			return "Supremo";
		}
		else if(usuario.getPerfil().getNome().equals("Administrador")) {
			Administracao adm = administracaoRepository.findById(usuario.getEntidade()).get();
			return adm.getNome();
		}
		else if(usuario.getPerfil().getNome().equals("Orgão")) {
			Orgao orgao = orgaoRepository.findById(usuario.getEntidade()).get();
			return orgao.getNome();
		}
		else if(usuario.getPerfil().getNome().equals("Banco")) {
			Consignataria consignataria = consignatariaRepository.findById(usuario.getEntidade()).get();
			return consignataria.getNome();
		}
		else if(usuario.getPerfil().getNome().equals("Averbadora")) {
			Averbadora averbadora = averbadoraRepository.findById(usuario.getEntidade()).get();
			return averbadora.getNome();
		}
		
		return "";
	}
	
	private List<Modulo> pegarModulosNivel3(List<Modulo> modulos, Usuario usuario){
		List<Modulo> modulosNivel3 = new ArrayList<Modulo>();
		ModuloSpecification moduloSpecification = new ModuloSpecification();
		boolean alreadyExists = false;
		
		for(Modulo modulo : modulos) {
			//Nivel 1
			if(modulo.getIdMaster() == 0) {
				continue;
			} else {
				Modulo moduloAnterior = moduloRepository.findById(modulo.getIdMaster()).get();
				
				if(moduloAnterior != null) {
					//Nivel 2
					if(moduloAnterior.getIdMaster() == 0) {
						continue;
					} else {
						//Nivel 3
						
						//Pegando as funcionalidades do modulo
						List<Funcionalidade> funcionalidades = funcionalidadeRepository.getFuncionalidadesByModulo(modulo.getId());
						
						//Para cada funcionalidade veja se a lista de perfil funcionalidade existe
						boolean canAdd = false;
						for(Funcionalidade funcionalidade : funcionalidades) {
				
							List<UsuarioPermissao> usuarioPermissoes = usuarioPermissaoRepository.getPermissaoByFuncEUser(funcionalidade.getId(), usuario.getId());
							
							if(usuarioPermissoes != null && !usuarioPermissoes.isEmpty()) {
								canAdd = true;
								break;
							}
						}
						
						if(funcionalidades == null || funcionalidades.isEmpty()) {
							if(modulo.getIdMaster() == 0) {
								canAdd = true;
							}
						}
						
						if(canAdd) {
							
							alreadyExists = false;
							for(int i = 0; i < modulosNivel3.size(); i++) {
								if(modulo.getId() == modulosNivel3.get(i).getId()) {
									alreadyExists = true;
									break;
								}
							}
							
							if(!alreadyExists) {
								modulosNivel3.add(modulo);
							}
						}
					}
				}
			}
		}
		
		return modulosNivel3;
	}
	
	private List<Modulo> pegarModulosNivel2(List<Modulo> modulos, List<Modulo> modulosN3, Usuario usuario){

		List<Modulo> modulosNivel2 = new ArrayList<Modulo>();
		boolean alreadyExists = false;
		
		if(modulosN3 != null && !modulosN3.isEmpty()) {
			for(Modulo moduloN3 : modulosN3) {
				Modulo moduloN2 = moduloRepository.pegarModuloByMaster(moduloN3.getIdMaster());
				
				if(moduloN2 != null) {
					
					alreadyExists = false;
					for(int i = 0; i < modulosNivel2.size(); i++) {
						if(moduloN2.getId() == modulosNivel2.get(i).getId()) {
							alreadyExists = true;
							break;
						}
					}
					
					if(!alreadyExists) {
						modulosNivel2.add(moduloN2);
					}
					
				}
				
			}
		}
		
		
		for(Modulo modulo : modulos) {
			
			if(modulo.getIdMaster() == 0) {
				continue;
			} else {
				Modulo moduloAnterior = moduloRepository.findById(modulo.getIdMaster()).get();
				
				if(moduloAnterior != null) {
					
					if(moduloAnterior.getIdMaster() == 0) {
						List<Funcionalidade> funcionalidades = funcionalidadeRepository.getFuncionalidadesByModulo(modulo.getId());
						
						boolean canAdd = false;
						for(Funcionalidade funcionalidade : funcionalidades) {

							List<UsuarioPermissao> usuarioPermissoes = usuarioPermissaoRepository.getPermissaoByFuncEUser(funcionalidade.getId(), usuario.getId());
							
							if(usuarioPermissoes != null && !usuarioPermissoes.isEmpty()) {
								canAdd = true;
								break;
							}
						}
						
						if(funcionalidades == null || funcionalidades.isEmpty()) {
							if(modulo.getIdMaster() == 0) {
								canAdd = true;
							}
						}
						
						if(canAdd) {
							
							alreadyExists = false;
							for(int i = 0; i < modulosNivel2.size(); i++) {
								if(modulo.getId() == modulosNivel2.get(i).getId()) {
									alreadyExists = true;
									break;
								}
							}
							
							if(!alreadyExists) {
								modulosNivel2.add(modulo);
							}
						}
					}
				}
			}
		}
		
		return modulosNivel2;
		
	}
	
	private List<Modulo> pegarModulosNivel1(List<Modulo> modulos, List<Modulo> modulosN2, Usuario usuario){

		List<Modulo> modulosNivel1 = new ArrayList<Modulo>();
		boolean alreadyExists = false;
		
		if(modulosN2 != null && !modulosN2.isEmpty()) {
			for(Modulo moduloN2 : modulosN2) {
				Modulo moduloN1 = moduloRepository.pegarModuloByMaster(moduloN2.getIdMaster());
				
				if(moduloN1 != null) {
					
					alreadyExists = false;
					for(int i = 0; i < modulosNivel1.size(); i++) {
						if(moduloN1.getId() == modulosNivel1.get(i).getId()) {
							alreadyExists = true;
							break;
						}
					}
					
					if(!alreadyExists) {
						modulosNivel1.add(moduloN1);
					}
					
				}
			}
		}
		
		for(Modulo modulo : modulos) {
			if(modulo.getIdMaster() == 0) {
				List<Funcionalidade> funcionalidades = funcionalidadeRepository.getFuncionalidadesByModulo(modulo.getId());
				
				boolean canAdd = false;
				for(Funcionalidade funcionalidade : funcionalidades) {

					List<UsuarioPermissao> usuarioPermissoes = usuarioPermissaoRepository.getPermissaoByFuncEUser(funcionalidade.getId(), usuario.getId());
					
					if(usuarioPermissoes != null && !usuarioPermissoes.isEmpty()) {
						canAdd = true;
						break;
					}
				}
				
				if(funcionalidades == null || funcionalidades.isEmpty()) {
					if(modulo.getIdMaster() == 0) {
						
						List<Modulo> modulosFilhos = moduloRepository.pegarModuloByPai(modulo.getId());
						
						if(modulosFilhos != null && !modulosFilhos.isEmpty()) {
							canAdd = false;
						} else {
							canAdd = true;
						}
					}
				}
				
				if(canAdd) {
					
					alreadyExists = false;
					for(int i = 0; i < modulosNivel1.size(); i++) {
						if(modulo.getId() == modulosNivel1.get(i).getId()) {
							alreadyExists = true;
							break;
						}
					}
					
					if(!alreadyExists) {
						modulosNivel1.add(modulo);
					}
				}
			}
		}
			
		return modulosNivel1;
	}
	
	public static boolean validarEmail(String email)
    {
        boolean isEmailIdValid = false;
        if (email != null && email.length() > 0) {
            String expression = "^[\\w\\.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$";
            Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(email);
            if (matcher.matches()) {
                isEmailIdValid = true;
            }
        }
        return isEmailIdValid;
    }
	
	public boolean isValidCNPJ(String cnpj) {
		final int[] pesoCNPJ = { 6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2 };

		if ((cnpj == null) || (cnpj.length() != 14))
			return false;

		Integer digito1 = calcularDigito(cnpj.substring(0, 12), pesoCNPJ);
		Integer digito2 = calcularDigito(cnpj.substring(0, 12) + digito1, pesoCNPJ);
		return cnpj.equals(cnpj.substring(0, 12) + digito1.toString() + digito2.toString());
	}
	
	private int calcularDigito(String str, int[] peso) {
		int soma = 0;
		for (int indice = str.length() - 1, digito; indice >= 0; indice--) {
			digito = Integer.parseInt(str.substring(indice, indice + 1));
			soma += digito * peso[peso.length - str.length() + indice];
		}
		soma = 11 - soma % 11;
		return soma > 9 ? 0 : soma;
	}
}
